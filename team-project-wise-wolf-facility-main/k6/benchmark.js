// k6 benchmark/load tests fuer die rest api
//
// zwei modi per env var MODE:
// - benchmark: triggert POST /tests/run + pollt /tests/results
//   misst die serverseitigen operation zeiten (insertBatch, findAll, ...)
// - crud: klassischer http load test auf /items (create, get, list, update, delete)
//
// aufruf im docker-netzwerk (empfohlen, keine lokale k6 installation noetig):
//   docker compose -f docker-compose.yml -f docker-compose.benchmark.yml run --rm k6   # MODE default insert
//
// aufruf mit lokalem k6:
//   k6 run k6/benchmark.js                                  # benchmark, mongo, 1000
//   BASE_URL=http://localhost:8080 MODE=benchmark DATABASE=rel SCALE=10000 INDEX=true k6 run k6/benchmark.js
//   MODE=compare SCALES=1000,10000,100000 k6 run k6/benchmark.js
//   MODE=crud VUS=10 DURATION=30s k6 run k6/benchmark.js
//   MODE=insert k6 run k6/benchmark.js                      # insert benchmark #8 (defaults: alle dbs, 100/1000/100000, 1+5 laeufe)
//   MODE=insert DATABASES=rel,mongo SCALES=100,1000 REPETITIONS=3 WARMUPS=1 k6 run k6/benchmark.js
//
// output: benchmark-results.json + benchmark-results.csv landen im cwd
//         (MODE=insert: insert-<zeitstempel>.csv / -summary.csv / .json)

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
// skalierungen für compare modus, kommagetrennt
const SCALES = (__ENV.SCALES || '1000').split(',').map(Number);

const JSON_HEADERS = { 'Content-Type': 'application/json' };

// fertige benchmark results sammeln, damit handleSummary csv/json draus bauen kann
var benchmarkResults = [];

// insert modus: alles passiert in setup() (siehe unten), daher grosszuegiger setup timeout
const INSERT_OPTIONS = {
  setupTimeout: `${MAX_WAIT_S + 60}s`,
  scenarios: {
    insert: {
      executor: 'per-vu-iterations',
      vus: 1,
      iterations: 1,
      exec: 'insertFlow',
    },
  },
  thresholds: {
    http_req_failed: ['rate<0.01'],
  },
};

export const options =
  MODE === 'insert'
    ? INSERT_OPTIONS
    : MODE === 'crud'
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
    : MODE === 'compare'
      ? {
          scenarios: {
            compare: {
              executor: 'per-vu-iterations',
              vus: 1,
              iterations: 1,
              exec: 'compareFlow',
            },
          },
          thresholds: {
            http_req_failed: ['rate<0.01'],
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

  const final = waitForResult(run.id);
  if (!final) {
    console.error(`run ${run.id} timeout nach ${MAX_WAIT_S}s`);
    return;
  }

  reportResult(final);
}

// ---------- compare modus ----------
// postet /tests/compare mit allen SCALES, pollt jeden lauf bis fertig
// ergebnis hat relationalCounts/mongoCounts/variantMatches zum vergleichen

export function compareFlow() {
  // INDEX gesetzt -> nur diese variante, sonst laeuft compare beide (mit + ohne)
  const body = { scales: SCALES };
  if (__ENV.INDEX !== undefined && __ENV.INDEX !== '') {
    body.index = __ENV.INDEX === 'true';
  }

  const res = http.post(
    `${BASE_URL}/tests/compare`,
    JSON.stringify(body),
    { headers: JSON_HEADERS }
  );

  check(res, {
    'compare started (202)': (r) => r.status === 202,
    'compare returns runs': (r) => Array.isArray(r.json()) && r.json().length > 0,
  });

  if (res.status !== 202) {
    console.error(`tests/compare failed: ${res.status} ${res.body}`);
    return;
  }

  const runs = res.json();
  console.log(`compare gestartet: ${SCALES.length} skalierungen -> run ids ${runs.map((r) => r.id)}`);

  runs.forEach((run) => {
    const final = waitForResult(run.id);
    if (final) {
      reportResult(final);
    } else {
      console.error(`compare run ${run.id} timeout nach ${MAX_WAIT_S}s`);
    }
  });
}

// ---------- insert modus (issue #8) ----------
// startet den insert benchmark (POST /tests/insert) und pollt GET /tests/insert/{id}.
// gemessen wird serverseitig (leere db, warmups + wiederholungen), k6 startet nur und speichert.
// braucht app.testdata.endpoint-enabled=true in der app (endpoint leert die dbs).
//
// laeuft komplett in setup(): handleSummary sieht keine variablen, die waehrend des tests
// gesetzt werden, aber den rueckgabewert von setup() als data.setup_data

function csvList(value) {
  return value.split(',').map((v) => v.trim()).filter((v) => v !== '');
}

export function setup() {
  if (MODE !== 'insert') {
    return null;
  }

  // nur gesetzte env vars mitschicken, rest = server defaults
  const body = {};
  if (__ENV.DATABASES) body.databases = csvList(__ENV.DATABASES);
  if (__ENV.SCALES) body.scales = csvList(__ENV.SCALES).map(Number);
  if (__ENV.REPETITIONS) body.repetitions = Number(__ENV.REPETITIONS);
  if (__ENV.WARMUPS) body.warmups = Number(__ENV.WARMUPS);
  if (__ENV.SEED) body.seed = Number(__ENV.SEED);
  if (__ENV.CHUNK_SIZE) body.chunkSize = Number(__ENV.CHUNK_SIZE);

  const res = http.post(`${BASE_URL}/tests/insert`, JSON.stringify(body), { headers: JSON_HEADERS });
  check(res, { 'insert started (202)': (r) => r.status === 202 });
  if (res.status !== 202) {
    console.error(`tests/insert failed: ${res.status} ${res.body}`);
    return null;
  }

  let report = res.json();
  const id = report.id;
  console.log(`insert benchmark ${id} gestartet: ${JSON.stringify(report.config)} -> ${report.totalSteps} messungen`);

  const deadline = Date.now() + MAX_WAIT_S * 1000;
  while (report.status === 'RUNNING' && Date.now() < deadline) {
    sleep(POLL_S);
    const r = http.get(`${BASE_URL}/tests/insert/${id}`);
    check(r, { 'insert report ok': (x) => x.status === 200 });
    if (r.status === 200) {
      report = r.json();
      console.log(`insert ${id}: ${report.completedSteps}/${report.totalSteps} messungen`);
    }
  }

  if (report.status === 'RUNNING') {
    console.error(`insert ${id} timeout nach ${MAX_WAIT_S}s`);
    return null;
  }
  if (report.status === 'FAILED') {
    console.error(`insert ${id} FAILED: ${report.error}`);
  }

  (report.summaries || []).forEach((s) => {
    console.log(`${s.database} scale=${s.scale}: mean ${s.meanMs.toFixed(3)} ms, median ${s.medianMs.toFixed(3)} ms (n=${s.n})`);
  });

  return {
    report: report,
    measurementsCsv: http.get(`${BASE_URL}/tests/insert/${id}/measurements.csv`).body,
    summaryCsv: http.get(`${BASE_URL}/tests/insert/${id}/summary.csv`).body,
  };
}

// eigentliche arbeit passiert in setup()
export function insertFlow() {}

// insert-<yyyyMMdd-HHmmss> aus dem startzeitpunkt (utc) vom server
function insertFileBase(report) {
  const ts = (report.startedAt || new Date().toISOString()).slice(0, 19).replace(/[-:]/g, '').replace('T', '-');
  return `insert-${ts}`;
}

// ---------- helpers ----------

// pollt /tests/results bis der run nicht mehr RUNNING ist
function waitForResult(runId) {
  const deadline = Date.now() + MAX_WAIT_S * 1000;
  let final = null;

  while (Date.now() < deadline) {
    sleep(POLL_S);

    const rr = http.get(`${BASE_URL}/tests/results`);
    check(rr, { 'results ok': (r) => r.status === 200 });

    const runs = rr.json() || [];
    final = runs.find((x) => x.id === runId);

    if (final && final.status !== 'RUNNING') {
      break;
    }
    console.log(`run ${runId} noch am laufen ...`);
  }

  return final && final.status !== 'RUNNING' ? final : null;
}

function reportResult(final) {
  benchmarkResults.push(final);

  if (final.status === 'FAILED') {
    console.error(`run ${final.id} FAILED: ${final.error}`);
    return;
  }

  console.log(`run ${final.id} fertig, status=${final.status}, typ=${final.type || 'run'}`);
  if (final.type === 'compare') {
    console.log(`counts rel:   ${JSON.stringify(final.relationalCounts)}`);
    console.log(`counts mongo: ${JSON.stringify(final.mongoCounts)}`);
    console.log(`variant matches: ${JSON.stringify(final.variantMatches)} -> countsMatch=${final.countsMatch}`);
  }
  console.log(`operation zeiten (ms): ${JSON.stringify(final.durationsMs)}`);
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
  if (MODE === 'insert') {
    const out = { stdout: textSummary(data, { indent: ' ', enableColors: true }) };
    const insert = data.setup_data;
    if (insert && insert.report) {
      const base = insertFileBase(insert.report);
      out[`${base}.csv`] = insert.measurementsCsv;
      out[`${base}-summary.csv`] = insert.summaryCsv;
      out[`${base}.json`] = JSON.stringify(insert.report, null, 2);
    }
    return out;
  }

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
