# MongoDB-Referenzmodell (normalisiert)

Zweite Modellvariante zum Vergleich mit dem Embedded-Modell
(`docs/mongo-dokumentmodell.md`). Hier wird **referenziert** statt
eingebettet: eine Collection pro Entität, verknüpft über `_id`-Referenzen
— analog zu Fremdschlüsseln in der relationalen DB.

Code: `nosql/ref/MongoReferencedRepository.java`
Beispiel: `src/main/resources/nosql/customer-referenced.example.json`

## Collections und Referenzen

| Collection | Inhalt | Referenzen |
|---|---|---|
| `ref_customers` | Stammdaten | `addressId` → `ref_addresses._id` |
| `ref_addresses` | Adresse (1:1) | — |
| `ref_billings` | Zahlungsdaten (1:n) | `customerId` → `ref_customers._id` |
| `ref_order_placements` | Verleihvorgang | `customerId` → customers, `orderId` → `ref_orders._id` |
| `ref_orders` | Bestellung | `storeId` → `ref_stores._id` |
| `ref_stores` | Filiale | — |
| `ref_store_films` | **m:n Join** Store↔Film | `storeId`, `filmId` |
| `ref_films` | Filme (inkl. languages/genres) | — |
| `ref_store_series` | **m:n Join** Store↔Series | `storeId`, `seriesId` |
| `ref_series` | Serien (inkl. languages/genres) | — |

Damit ist das Schema eine fast 1:1-Übersetzung des relationalen
Postgres-Schemas — nur dass die "Tabellen" Collections sind und die
"Foreign Keys" `_id`-Werte in Feldern.

## Wie die m:n-Beziehung abgebildet ist

`store_films` und `store_series` sind **eigene Join-Collections** —
das direkte MongoDB-Analogon zu den relationalen Join-Tabellen.
Jedes Dokument ist ein Tripel `(storeId, filmId)` / `(storeId, seriesId)`.

Alternative wäre ein `filmIds: []`-Array im Store-Dokument gewesen —
die Join-Collection ist aber näher am relationalen Vorbild und zeigt
den echten m:n-Fall (viele Stores ↔ viele Filme).

## Wie referenzierte Abfragen gelöst werden

- **`findByCityProjection(Sorted)`**: Aggregation mit `$lookup`
  (`customers.addressId` → `addresses._id`), danach `$match` auf
  `address.city`, `$project`/`$sort`/`$limit`
- **`findAll` / `findByCity`** (volle Dokumente): erst Customers laden,
  dann alle referenzierten Daten **gebündelt** per `$in` nachladen
  (addresses, billings, placements, orders, stores, join collections,
  films, series) und in Java zum `CustomerDocument` zusammensetzen —
  das MongoDB-Analogon zu JOINs + Entity-Assembly im ORM
- **`sumOrderTotalsByCity`**: Aggregation mit `$lookup`-Kette
  `placements → orders → customers → addresses`, dann `$group`

## Begründung: wann Referencing besser ist als Embedding

| Kriterium | Referencing gewinnt | Embedding gewinnt |
|---|---|---|
| **Datenmenge** | Katalog (Store/Filme/Serien) liegt **einmal** in der DB, nicht pro Kunde dupliziert | — |
| **Update-Anomalien** | Film-Titel ändern = 1 Update | Änderung muss in *jedem* Kundendokument gefixt werden |
| **Schreibkosten** | Insert schreibt ~5 kleine Docs statt 1 riesiges (~3,4 KB) | 1 `insertMany` reicht, keine Referenzen pflegen |
| **Leseperformance (1 Doc)** | — | 1 `find` liefert alles, kein Join |
| **Leseperformance (cross-entity)** | Index auf Join-Feldern hilft | `$lookup`/Hydration kostet extra Queries |
| **16MB-Limit** | Docs bleiben klein | Tiefe Nesting kann ans Limit stoßen |

Faustregel: **Embedding** wenn Daten immer zusammen gelesen werden und
„owned" sind (1:n vom Root). **Referencing** wenn Daten geteilt werden
(m:n), oft unabhängig geändert werden oder die Dokumente sonst zu groß
werden. Genau das misst der Benchmark.

## Indizes (Benchmark)

`BenchmarkRunner.applyIndex` legt pro Variante an/entfernt:

| Feld | Collection | Index-Name |
|---|---|---|
| `city` (Filter) | `ref_addresses` | `ref_addr_city_idx` |
| `nickname` (Sort + Update) | `ref_customers` | `ref_cust_nick_idx` |
| `addressId` (Join) | `ref_customers` | `ref_cust_addr_idx` |
| `customerId` (Join) | `ref_order_placements` | `ref_place_cust_idx` |
| `customerId` (Join) | `ref_billings` | `ref_bill_cust_idx` |

## Interpretation (gemessene Werte, ohne Index)

Drei-Wege-Vergleich `rel` vs `mongo` (embedded) vs `mongo-ref`
(`/tests/compare`, gleiche generierten Daten, countsMatch überall `true`):

| Operation | scale | rel | mongo embedded | mongo-ref |
|---|---:|---:|---:|---:|
| insertBatch | 100 | 109 | 22 | 55 |
| insertBatch | 1.000 | 452 | 109 | 46 |
| insertBatch | 100.000 | 40.124 | 14.320 | **5.151** |
| findAll | 100.000 | 5.118 | 18.343 | **2.771** |
| findByCity | 100.000 | 4.801 | 16.364 | 6.871 |
| findByCityProjection | 100.000 | 103 | 218 | 3.902 |
| findByCityProjectionSorted | 100.000 | 148 | 750 | 4.048 |
| aggregation | 100.000 | 241 | 251 | 10.316 |

### Was zeigt sich

- **Referencing gewinnt beim Insert ab mittlerer Größe**: die
  Embedded-Variante schreibt pro Kunde ein ~3,4 KB-Dokument mit dem
  kompletten Store-Katalog (10 Films + 10 Series) dupliziert — bei
  100k Kunden sind das ~340 MB BSON. `mongo-ref` schreibt 5–10 kleine
  Dokumente pro Kunde + den Katalog **einmal**, ist also trotz mehrerer
  `insertMany`-Calls schneller (5,1s vs 14,3s).
- **Referencing gewinnt bei `findAll`/`findByCity` (volle Docs)**:
  2,8s vs 18,3s. Der `$lookup`/`$in`-Hydration-Pfad arbeitet auf kleinen
  Dokumenten mit `_id`-Index; embedded muss die riesigen Docs komplett
  aus dem Storage lesen und per Jackson zerlegen.
- **Referencing verliert bei Filtern/Projektionen auf fremden Feldern**:
  `findByCityProjection` braucht `$lookup` auf `addresses` **bevor**
  `$match` auf `city` greift — ohne Denormalisierung kann der Filter
  keinen Index auf `customers` nutzen. 3,9s vs 218ms (embedded hat den
  Wert direkt im Dokument). Genau der Classic-Fall: *man kann nicht auf
  ein Feld filtern, das nicht im Dokument liegt*.
- **Aggregation ist bei referencing am teuersten** (10,3s): die
  `$lookup`-Kette `placements → orders → customers → addresses` muss
  pro Placement 3 Collections traversieren. Embedded delegiert das an
  `$unwind`/`$group` auf einem Dokument — 251ms.
- **Kleinstskala (100)** ist Rauschen: alle Varianten unter ~100ms,
  Referencing-Overhead (mehre Queries) wiegt noch nicht.

### Fazit

Referencing zahlt sich aus, wenn **geschrieben** wird oder wenn
**kleine gezielte** Reads dominate — und verliert, sobald **Joins zur
Laufzeit** nötig sind. Embedded invertiert das: Write teuer (Duplikation),
Read billig. Genau der Trade-off, den die Aufgabe untersuchen sollte.

## Ausführung

```bat
:: einzelne varainte
{"database": "mongo-ref", "scale": 1000, "index": false}
POST /tests/run

:: compare läuft automatisch auf allen drei seiten
:: (rel, mongo embedded, mongo-ref) mit timings mongo-ref.*
POST /tests/compare {"scales":[100,1000,100000]}
```
