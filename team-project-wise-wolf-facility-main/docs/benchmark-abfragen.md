# Benchmark-Abfragen: die vier Find-Szenarien

Konkrete Spezifikation der vier Find-Varianten aus der Aufgabenstellung.
Alle Szenarien laufen **identisch** auf drei Persistenz-Varianten:

- `rel` — relational (Postgres via JPA, `benchmark/jpa/JpaPersistenceRepository`)
- `mongo` — MongoDB embedded (`nosql/MongoCustomerRepository`)
- `mongo-ref` — MongoDB referenziert (`nosql/ref/MongoReferencedRepository`)

Code: `benchmark/BenchmarkRunner.java` (`FIND_VARIANTS`, `findVariant`),
`benchmark/PersistenceRepository.java`.

## Testdatensatz (reproduzierbar)

Jeder Lauf generiert synthetische Kunden über
`BenchmarkRunner.generateDocuments(scale, idBase)`:

- `nickname` = `bench.user.<id>` (Regex `^bench\.user\.`)
- `address.city` = **`Benchmarkstadt`** (`BENCH_CITY`-Konstante)
- 1 `billingInformation`, 1 `orderPlacement` (mit `OrderJson` +
  Store-Katalog: 10 Films + 10 Series) pro Kunde
- `idBase` = höchste vorhandene `_id`/`id` + 1 → IDs laufen über
  die Skalen hinweg weiter, kein Kollidieren
- Limit: `/tests/run` → `min(1000, scale)`,
  `/tests/compare` → `scale + 10_000` (damit rel Seed-Rows nicht capt)

## Szenario 1: Find ohne Filter

| | |
|---|---|
| **Was gelesen wird** | alle Kundendokumente/-zeilen bis `limit` |
| **Filter** | keiner |
| **Felder** | vollständiger Customer-Datensatz (`CustomerDocument`) |
| **Erwartete Menge** | alle Benchmark-Kunden (plus Seed-Daten auf `rel`, deshalb Count-Vergleich über Deltas) |
| **Methoden** | `findAll(limit)` → JPQL `select c from Customer c` / `customers.find()` / `ref_customers.find()` |

## Szenario 2: Find mit Filter

| | |
|---|---|
| **Filterfeld** | `address.city` (rel: `customer.address_city`-Join-Spalte) |
| **Testwert** | **`"Benchmarkstadt"`** (`BENCH_CITY`) |
| **Erwartete Menge** | genau die `scale` generierten Benchmark-Kunden (Seed-Städte sind andere) |
| **Methoden** | `findByCity("Benchmarkstadt", limit)` → JPQL `where c.address.city = :city` / `Filters.eq("address.city", ...)` / `$lookup`+`$match` bei `mongo-ref` |

## Szenario 3: Find mit Filter und Projektion

| | |
|---|---|
| **Filter** | identisch zu Szenario 2: `address.city = "Benchmarkstadt"` |
| **Zurückgegebene Felder** | nur `_id`, `nickname`, `city` → `CustomerProjection(id, nickname, city)` |
| **NICHT geliefert** | email, namen, adresse(rest), billing, placements — bewusst weggelassen |
| **Methoden** | `findByCityProjection(city, limit)` → JPQL `select c.id, c.nickname, c.address.city` / `Projections.include("_id","nickname","address.city")` / `$lookup`+`$project` bei `mongo-ref` |

## Szenario 4: Find mit Filter, Projektion und Sortierung

| | |
|---|---|
| **Filter** | identisch: `address.city = "Benchmarkstadt"` |
| **Projektionsfelder** | identisch: `_id`, `nickname`, `city` |
| **Sortierfeld** | `nickname` |
| **Richtung** | **aufsteigend** (`ascending = true` in `findVariant`) |
| **Methoden** | `findByCityProjectionSorted(city, true, limit)` → JPQL `order by c.nickname asc` / `Sorts.ascending("nickname")` / `$sort` nach `$project` bei `mongo-ref` |

## Äquivalenz und Vergleich

- **Semantik**: alle vier Varianten liefern pro Seite dieselbe
  Ergebnis*anzahl* (Vergleich über Deltas zum Basis-Zähler, weil `rel`
  Seed-Rows enthält die Mongo nicht kennt). `variantMatches` +
  `countsMatch` im `BenchmarkResult`.
- **Laufzeiten**: `durationsMs` enthält pro Variante
  `rel.<variant>Ms`, `mongo.<variant>Ms`, `mongo-ref.<variant>Ms`
  (compare) bzw. `<variant>Ms` (Einzel-Lauf).
- **Ausführung**:
  - `POST /tests/run {"database":"rel|mongo|mongo-ref","scale":N,"index":bool}`
  - `POST /tests/compare {"scales":[100,1000,100000],"index":bool}`
    → alle drei DBs, alle vier Finds + Aggregation, Count-Vergleich
  - k6: `MODE=compare SCALES=100,1000,100000 k6 run k6/benchmark.js`
- **Verifiziert**: Compare-Läufe auf scale 100 / 1.000 / 100.000 —
  `variantMatches` und `countsMatch` überall `true`
  (siehe `docs/mongo-referenzmodell.md` für die Messwerte).
