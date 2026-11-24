# Theorieteil: Relationale Datenbank vs. MongoDB

This document places the practical comparison of this project
(relational Postgres/JPA vs. MongoDB embedded vs. MongoDB
referenced) into a theoretical context. All statements refer to the
actually implemented reference project (`docs/Videothek.puml`,
`nosql/model/CustomerDocument.java`, `nosql/ref/MongoReferencedRepository.java`)
and to the measured benchmark results (`docs/mongo-referenzmodell.md`,
`docs/benchmark-abfragen.md`).

## 1. Relationale vs. dokumentenorientierte Datenbanken

**Relationale Datenbanken** (z. B. PostgreSQL) store data in tables
with a fixed schema. Entities are normalized to avoid redundancy;
relationships are expressed through foreign keys and reconstructed at
read time via JOINs. Integrity is enforced declaratively (PK/FK
constraints, ACID transactions).

**Document-oriented databases** (e.g. MongoDB) store data as
self-contained documents (BSON — binary JSON) in collections.
There is no fixed schema per document; related data that is always
read together can be embedded directly into the document, so a single
read returns a complete aggregate. Relationships that span documents
are expressed by ID references and resolved with `$lookup`
aggregations or multiple queries.

The core trade-off: normalization minimizes storage and update
anomalies but costs JOINs on reads; embedding optimizes read locality
at the price of duplicated data and expensive cross-document updates.

## 2. Relationales Schema des Referenzprojekts

Das Referenzprojekt (Wise Wolf Videothek) persistiert in Postgres
(`docs/Videothek.puml`, Flyway-Migrationen):

| Entität | Tabellen | Beziehungen |
|---|---|---|
| `Customer`/`User` | `customer`, `users`-Vererbung | 1:n `billing_information`, 1:n `order_placement`, 1:1 Adresse (Spalten `address_*`) |
| `Billing_Information` | `billing_information` | n:1 `customer` |
| `Order_Placement` | `order_placement` | n:1 `customer`, n:1 `orders` |
| `Order` | `orders` | n:1 `store` |
| `Store` | `store`, `phone_numbers` | **m:n `store_films`, m:n `store_series`** |
| `Film`/`Series` | `film`, `series` + `film_languages`, `film_genres`, `series_languages`, `series_genres` (Element-Collections) | n:m über Join-Tabellen |

Die zentrale **M:N-Beziehung** ist `Store ↔ Film` und `Store ↔ Series`,
realisiert über die Join-Tabellen `store_films` bzw. `store_series`
(`Store.java`: `@ManyToMany` + `@JoinTable`). Ein Store führt mehrere
Filme/Serien, ein Film kann in mehreren Stores verfügbar sein.
Sprachen und Genres sind als Element-Collections eigene Tabellen
(`film_languages`, `film_genres`, …).

Lesen der Kundenansicht erfordert damit relational **6+ Tabellen
plus Joins**: `customer` → `billing_information`, `order_placement` →
`orders` → `store` → `store_films`/`store_series` → `film`/`series` →
Element-Collections.

## 3. MongoDB-Embedded-Modell

`nosql/model/CustomerDocument.java` bildet das „frontend-optimierte"
Modell ab: **ein Customer = ein Dokument**. Alles, was die
Kundenansicht anzeigt, steckt im selben Dokument:

```
CustomerDocument
├── address                (Sub-Dokument, 1:1)
├── billingInformation[]   (Array, 1:n)
└── orderPlacements[]      (Array, 1:n)
    └── order              (Sub-Dokument: orderId, orderDate, totalAmount)
        └── store          (Sub-Dokument)
            ├── films[]    (Sub-Docs, m:n-Katalog embedded)
            └── series[]   (Sub-Docs, m:n-Katalog embedded)
```

Begründung für Embedding (siehe `docs/mongo-dokumentmodell.md`):
die Frontend-Kundenansicht ist ein Aggregat — ein `find({_id})`
liefert Stammdaten, Adresse, Zahlung und kompletten Verleih-Verlauf
inkl. Store-Katalog in **einem** Roundtrip, ohne client-seitiges
Nachladen und ohne Joins. Die m:n-Beziehung `store_films`/
`store_series` ist als Arrays von Sub-Dokumenten im Store-Sub-Dokument
eingebettet.

## 4. Unterschiede: relationale Abbildung vs. eingebettetes JSON

| Aspekt | Relational (Postgres/JPA) | MongoDB embedded | MongoDB referenziert |
|---|---|---|---|
| Speicher | normalisiert, keine Duplikation | massive Duplikation (Store-Katalog pro Kunde pro Placement, ~3,4 KB/Doc) | normalisiert über Collections, Referenzen per `_id` |
| Read (Aggregate) | viele Tabellen + JOINs | **1 Document-Fetch** | `$lookup`/Hydration über Collections |
| Write | 1 Transaktion, FK-Constraints | 1 `insertMany`, aber große BSON-Docs | mehrere `insertMany` über 10 Collections |
| Update-Anomalie | Änderung an 1 Stelle | z. B. Film-Titel in *jedem* Kundendokument | Änderung an 1 Stelle (wie relational) |
| Schema | starr (DDL, Constraints) | schemaless pro Dokument | schemaless pro Collection |
| Integrität | FK-Constraints, ACID | keine FK-Durchsetzung (nur appl.) | keine FK-Durchsetzung |

## 5. Vorteile, Nachteile, Grenzen

**Relational:** klare Integrität, kein Datendrift, ausgereifte
Query-Optimizer. Nachteil im Projekt: tiefe Join-Ketten für die
Kundenansicht (6+ Tabellen), speicher-/indexlastig bei 100k Sätzen.

**Embedded:** sehr schneller Aggregate-Read, wenn die Dokumente klein
sind — im Projekt allerdings begrenzt durch die Duplikation (siehe
Messung: `findAll` 18,3s bei 100k, langsamer als referenziert).
Keine Joins, horizontale Skalierung per Sharding auf Dokumentebene.
Nachteile: Duplikation bläht die Collection auf (~340 MB bei 100k
Kunden), Updates sind O(Dokumente), 16-MB-Dokumentlimit, keine
FK-Integrität.

**Referenziert (Mongo):** dekat die relationale Normalisierung —
Katalog einmal gespeichert, m:n als Join-Collections. Kombiniert
Mongo-Operations (`$lookup`, Aggregation) mit relationaler
Denkweise; gleicht bei Writes/`findAll` der relation, bei
Filter/Aggregation zahlt es Join-Kosten analog zu SQL.

**Grenzen beider Ansätze:** kein Ansatz ist universell — die Wahl
hängt von der Zugriffsstruktur ab (lesedominiert vs. schreib-
dominiert, Aggregate-Reads vs. punktuelle Updates).

## 6. Interpretation der Messergebnisse

Messung: `/tests/compare`, identischer synthetischer Datensatz
(`bench.user.*`, Stadt `Benchmarkstadt`), Skalen 100 / 1.000 /
100.000, ohne Index. Volle Tabelle in `docs/mongo-referenzmodell.md`.

Befunde (scale 100.000, ms):

- **insertBatch**: rel 40.124 / embedded 14.320 / ref **5.151** —
  embedded zahlt den Preis der ~3,4-KB-Duplikation pro Kunde;
  ref schreibt kleine Dokumente + Katalog einmal.
- **findAll / findByCity (volle Docs)**: embedded 18.343 / 16.364 —
  die Riesen-Docs müssen komplett gelesen + per Jackson zerlegt
  werden; ref hydriert per `$in`-Chunks auf kleinen Docs (2.771 /
  6.871) und schlägt damit sogar die relationale Seite leicht.
- **findByCityProjection(Sorted)**: embedded 218 / 750 — der
  Projektionsfilter `address.city` liegt direkt im Dokument;
  ref muss vor `$match` `$lookup` auf `addresses` fahren
  (3.902 / 4.048), weil das Filterfeld nicht im Customer-Dokument
  liegt — klassischer Nachteil ohne Denormalisierung.
- **Aggregation** (`$unwind`/`$group` vs. `GROUP BY` vs.
  `$lookup`-Kette): embedded 251 ≈ rel 241, ref 10.316 — die
  `$lookup`-Kette `placements → orders → customers → addresses`
  traversiert pro Placement drei Collections.

**Bezug zum Datenmodell:** die Messwerte bestätigen die Theorie —
Embedding gewinnt genau dort, wo das Modell „frontend-optimiert"
lesedominiert ist (Aggregate-Reads, projezierte Filter auf
Dokumentfeldern); es verliert, sobald die Dokumente durch
Redundanz aufblähen (Writes) oder die benötigten Daten nicht im
Dokument liegen (fremde Filter, Joins). Referencing intermediate
beide: normalspeichernd wie relational, aber mit Mongo-Operations
auf kleinen Docs — die Joins zur Laufzeit kosten ähnlich viel wie
relationale JOINs.

## Quellen

- MongoDB Documentation — Data Modeling (Embedding vs. Referencing):
  https://www.mongodb.com/docs/manual/data-modeling/concepts/
- MongoDB Documentation — Aggregation `$lookup`:
  https://www.mongodb.com/docs/manual/reference/operator/aggregation/lookup/
- MongoDB Documentation — BSON-Datentypen / 16-MB-Limit:
  https://www.mongodb.com/docs/manual/reference/limits/
- MongoDB University — M001 „MongoDB Basics" (Schema Design Concepts)
- Projekt-interne Doku: `docs/mongo-dokumentmodell.md`,
  `docs/mongo-referenzmodell.md`, `docs/benchmark-abfragen.md`,
  `docs/Videothek.puml`
- Referenz-Musterprojekt lt. Aufgabenstellung:
  https://github.com/schletz/Dbi3Sem/tree/master/13_NoSQL/Uebungen/SalesDb/SalesDbGenerator
