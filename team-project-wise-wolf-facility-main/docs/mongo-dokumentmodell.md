# MongoDB-Dokumentmodell

Abgeleitet aus dem relationale Schema (Postgres, `docs/Videothek.puml`),
"optimiert auf Frontend" wie im Unterricht besprochen: zusammengehörige
Daten werden **embedded** in ein Dokument gesteckt statt per JOINS
zusammengeführt.

Extrem-Variante für den Vergleich: es wird **so viel wie möglich
eingebettet**, auch wenn das normalisiert unlogisch wäre — genau das
soll der Benchmark zeigen (Datenmenge + Leseperformance vs. Joins).

Code: `nosql/model/CustomerDocument.java`
Beispiel: `src/main/resources/nosql/customer-document.example.json`

## Root-Dokument: `Customer`

Pro Kunde existiert **genau ein Dokument**. Begründung:

- die Frontend-Kundenansicht braucht auf einen Blick: Stammdaten,
  Adresse, Zahlungsdaten und den kompletten Verleih-Verlauf
- in der normalisierten Variante sind das 6+ Tabellen
  (`customer`, `address_*`-Spalten, `billing_information`,
  `order_placement`, `orders`, `store`, `store_films`, `store_series`,
  `film_*`, `series_*` + element collections)
- mit embedded Daten liefert **ein einziger `find` auf `_id`**
  die komplette Ansicht, kein client-seitiges Nachladen, keine Joins

## Was embedded ist (alles außer einer ID)

| Relation (relational) | Im Dokument | Form |
|---|---|---|
| `customer.address_*` (1:1) | `address` | eingebettetes Objekt |
| `billing_information` (1:n) | `billingInformation[]` | Array von Sub-Docs |
| `order_placement` (1:n) | `orderPlacements[]` | Array, Verleih-Daten (`orderDate`, `orderReturnDate`, `actualReturnDate`) |
| `orders` (n:1) | `orderPlacements[].order` | **Order-Objekt embedded** (`orderId`, `orderDate`, `totalAmount`) |
| `store` (n:1) | `orderPlacements[].order.store` | **eingebettetes Store-Objekt** |
| `store_films` (m:n) | `store.films[]` | **Film-Sub-Docs embedded** |
| `store_series` (m:n) | `store.series[]` | **Series-Sub-Docs embedded** |
| `film_*`, `film_languages`, `film_genres` | `store.films[]` Felder | Titel, Jahr, Länge, Sprachen, Genres |
| `series_*`, `series_languages`, `series_genres` | `store.series[]` Felder | Titel, Jahr, Episoden, Länge, Sprachen, Genres |

Heisst: Der **gesamte Katalog des Stores** (10 Films + 10 Series
inkl. Element-Collections) steckt in **jedem** `orderPlacement` jedes
Kundendokuments — dieselbe Store-Info wird also pro Kunde und pro
Placement **redundant gespeichert**. Genau das ist der Trade-off, den
der Benchmark quantifizieren soll.

## Was referenziert bleibt

- **nichts** — das Modell ist zu 100% embedded. Sogar die Order ist
  als Sub-Dokument in `orderPlacements[].order` gesteckt; `orderId`
  existiert nur noch als Datenfeld innerhalb dieses Objekts, nicht
  als Verweis auf eine andere Collection

## Beispiel-Dokument

```json
{
  "_id": 1,
  "nickname": "seed.user.1",
  "email": "seed.user.1@example.com",
  "firstName": "Anna",
  "lastName": "Huber1",
  "permission": "Admin",
  "address": {
    "street": "Seedstraße 1",
    "city": "Wien",
    "zipCode": "1001",
    "country": "Austria"
  },
  "billingInformation": [
    {
      "cardNumber": "4000000000000000",
      "expirationDate": "2027-01-01",
      "cvv": 100
    }
  ],
  "orderPlacements": [
    {
      "orderDate": "2020-01-01",
      "orderReturnDate": "2020-01-15",
      "actualReturnDate": "2020-01-11",
      "order": {
        "orderId": 1,
        "orderDate": "2020-01-01T10:00:00",
        "totalAmount": 42.00,
        "store": {
        "storeId": 1,
        "phoneNumber": "+43 1 55500001",
        "films": [
          {
            "filmId": 101,
            "title": "Film 101",
            "releaseYear": 1991,
            "length": 91.0,
            "languages": ["German", "English"],
            "genres": ["Action", "Drama"]
          }
        ],
        "series": [
          {
            "seriesId": 201,
            "title": "Series 201",
            "releaseYear": 1996,
            "episodes": 7,
            "length": 41.0,
            "languages": ["German"],
            "genres": ["Comedy", "Thriller"]
          }
        ]
        }
      }
    }
  ]
}
```

## Indizes (Benchmark)

Auf den Feldern, die in den Find-Filtern und Sortierungen der
Benchmark-Abfragen vorkommen, werden Indizes angelegt
(`BenchmarkRunner.applyIndex`, automatisch pro Lauf, wird nach dem
Lauf gedroppt bzw. vor dem naechsten Lauf neu erstellt):

| Feld (Filter/Sort) | MongoDB | Relational (Postgres) |
|---|---|---|
| `address.city` | `bench_city_idx` auf `address.city` | `bench_customer_city_idx` auf `customer(address_city)` |
| `nickname` (Sort + Update-Filter) | `bench_nick_idx` auf `nickname` | `bench_customer_nick_idx` auf `customer(nickname)` |

- Find-Filter: `findByCity`, `findByCityProjection`,
  `findByCityProjectionSorted` filtern alle auf `address.city`
- Sortierung läuft auf `nickname`, Update-Filter
  (`updateLastNameByNickname`) ebenfalls
- Variante "mit/ohne Index" ist pro Lauf waehlbar:
  `POST /tests/run {"index": true|false}` oder
  `POST /tests/compare {"scales":[...], "index": true|false}`
  (bei compare ohne `index`-Angabe laufen automatisch beide Varianten)

## Trade-offs (kurz)

- **READ**: 1 Query statt Customer + Billings + Placements + Orders +
  Store + Films + Series + allen Join-Tabellen — riesiger Unterschied
  zur normalisierten Variante
- **WRITE**: Daten werden massiv dupliziert (Store-Katalog pro Kunde
  pro Placement) — Insert ist langsamer, Collection wird groß
- **UPDATE-Anomalie**: ändert sich ein Film-Titel, muss er in
  *jedem* Kundendokument gefixt werden — klassisches embedded-Problem,
  wird im Vergleich sichtbar
- **Grösse**: Dokumente bleiben unter dem 16MB-Limit, aber die
  Collection wächst deutlich schneller als relational
