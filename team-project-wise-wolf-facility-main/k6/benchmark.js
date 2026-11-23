// k6 benchmark/load tests fuer die rest api
//
// zwei modi per env var MODE:
// - benchmark: triggert POST /tests/run + pollt /tests/results
//   misst die serverseitigen operation zeiten (insertBatch, findAll, ...)
// - crud: klassischer http load test auf /items (create, get, list, update, delete)
//
// aufruf:
//   k6 run k6/benchmark.js                                  # benchmark, mongo, 1000
//   BASE_URL=http://localhost:8080 MODE=benchmark DATABASE=rel SCALE=10000 INDEX=true k6 run k6/benchmark.js
//   MODE=crud VUS=10 DURATION=30s k6 run k6/benchmark.js
//
// output: benchmark-results.json + benchmark-results.csv landen im cwd

import http from 'k6/http';
import { check, sleep } from 'k6';
import { textSummary } from 'https://jslib.k6.io/k6-summary/0.0.3/index.js';

const BASE_URL = __ENV.BASE_URL || 'http://localhost:8080';
const MODE = __ENV.MODE || 'benchmark';
const DATABASE = __ENV.DATABASE || 'mongo';
const SCALE = Number(__ENV.SCALE || 1000);
const INDEX = (__ENV.INDEX || 'false') === 'true';
const VUS = Number(__ENV.VUS || 5);
const DURATION = __ENV.DURATION || '30s';
const POLL_S = Number(__ENV.POLL_S || 5);
const MAX_WAIT_S = Number(__ENV.MAX_WAIT_S || 7200);

const JSON_HEADERS = { 'Content-Type': 'application/json' };

// fertige benchmark results sammeln, damit handleSummary csv/json draus bauen kann
var benchmarkResults = [];

export const options =
  MODE === 'crud'
    ? {
        scenarios: {
          crud: {
            executor: 'constant-vus',
            vus: VUS,
            duration: DURATION,
            exec: 'crudFlow',
          },
        },
        thresholds: {
          http_req_failed: ['rate<0.01'],
          http_req_duration: ['p(95)<1000'],
          checks: ['rate>0.95'],
        },
      }
    : {
        scenarios: {
          benchmark: {
            executor: 'per-vu-iterations',
            vus: 1,
            iterations: 1,
            exec: 'benchmarkFlow',
          },
        },
        thresholds: {
          http_req_failed: ['rate<0.01'],
        },
      };

// ---------- benchmark modus ----------
// startet testlauf async und pollt bis er durch ist
// server macht reset + index + insert + alle finds/updates/deletes
// ergebnis liegt in durationsMs (op name -> ms)

export function benchmarkFlow() {
  const res = http.post(
    `${BASE_URL}/tests/run`,
    JSON.stringify({ database: DATABASE, scale: SCALE, index: INDEX }),
    { headers: JSON_HEADERS }
  );

  check(res, {
    'run started (202)': (r) => r.status === 202,
    'run has id': (r) => r.json('id') !== undefined,
  });

  if (res.status !== 202) {
    console.error(`tests/run failed: ${res.status} ${res.body}`);
    return;
  }

  const run = res.json();
  console.log(`benchmark run ${run.id} gestartet (db=${DATABASE}, scale=${SCALE}, index=${INDEX})`);

  const deadline = Date.now() + MAX_WAIT_S * 1000;
  let final = null;

  while (Date.now() < deadline) {
    sleep(POLL_S);

    const rr = http.get(`${BASE_URL}/tests/results`);
    check(rr, { 'results ok': (r) => r.status === 200 });

    const runs = rr.json() || [];
    final = runs.find((x) => x.id === run.id);

    if (final && final.status !== 'RUNNING') {
      break;
    }
    console.log(`run ${run.id} noch am laufen ...`);
  }

  if (!final) {
    console.error(`run ${run.id} timeout nach ${MAX_WAIT_S}s`);
    return;
  }

  benchmarkResults.push(final);

  if (final.status === 'FAILED') {
    console.error(`run ${run.id} FAILED: ${final.error}`);
  } else {
    console.log(`run ${run.id} fertig, status=${final.status}`);
    console.log(`operation zeiten (ms): ${JSON.stringify(final.durationsMs)}`);
  }
}

// ---------- crud modus ----------
// last test auf /items: pro iteration create -> get -> list -> update -> delete
// messen=http layer (serverseitig mongo), gut fuer last/vergleich frontend

export function crudFlow() {
  const tag = `${__VU}-${__ITER}-${Date.now()}`;

  const doc = {
    nickname: `k6.user.${tag}`,
    email: `k6.user.${tag}@example.com`,
    firstName: 'K6',
    lastName: 'Loadtest',
    permission: 'Customer',
    address: { street: 'Lastweg 1', city: 'K6stadt', zipCode: '1010', country: 'Austria' },
    billingInformation: [{ cardNumber: '4000000000000001', expirationDate: '2028-01-01', cvv: 123 }],
    orderPlacements: [],
  };

  // create
  const create = http.post(`${BASE_URL}/items`, JSON.stringify(doc), { headers: JSON_HEADERS });
  const created = check(create, {
    'create 201': (r) => r.status === 201,
    'create returns id': (r) => r.json('_id') !== undefined,
  });
  if (!created) return;

  const id = create.json('_id');

  // read single
  const get = http.get(`${BASE_URL}/items/${id}`);
  check(get, {
    'get 200': (r) => r.status === 200,
    'get returns doc': (r) => r.json('nickname') !== undefined,
  });

  // read list
  const list = http.get(`${BASE_URL}/items?limit=10`);
  check(list, { 'list 200': (r) => r.status === 200 });

  // update (replace ganzes doc)
  const updated = Object.assign({}, doc, { lastName: 'Updated' });
  const put = http.put(`${BASE_URL}/items/${id}`, JSON.stringify(updated), { headers: JSON_HEADERS });
  check(put, { 'update 200': (r) => r.status === 200 });

  // delete
  const del = http.del(`${BASE_URL}/items/${id}`);
  check(del, { 'delete 204': (r) => r.status === 204 });

  sleep(0.1);
}

// ---------- summary output ----------
// json + csv mit allen fertigen benchmark runs
// csv hat eine zeile pro operation (insertBatchMs, findAllMs, ...)

export function handleSummary(data) {
  const rows = ['id,database,scale,indexVariant,status,operation,durationMs'];

  benchmarkResults.forEach((r) => {
    Object.entries(r.durationsMs || {}).forEach(([op, ms]) => {
      rows.push([r.id, r.database, r.scale, r.indexVariant, r.status, op, ms].join(','));
    });
  });

  // http metriken (crud modus) + fertige server-side benchmark runs
  const output = {
    metrics: data.metrics,
    benchmarkRuns: benchmarkResults,
  };

  return {
    'benchmark-results.json': JSON.stringify(output, null, 2),
    'benchmark-results.csv': rows.join('\n'),
    stdout: textSummary(data, { indent: ' ', enableColors: true }),
  };
}
