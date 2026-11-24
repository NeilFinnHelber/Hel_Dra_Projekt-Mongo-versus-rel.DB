# MongoDB-Dokumentmodell

Abgeleitet aus dem relationale Schema (Postgres, `docs/Videothek.puml`),
"optimiert auf Frontend" wie im Unterricht besprochen: zusammengehörige
Daten werden **embedded** in ein Dokument gesteckt statt per JOINS
zusammengeführt.

Code: `nosql/model/CustomerDocument.java`
Beispiel: `src/main/resources/nosql/customer-document.example.json`

## Root-Dokument: `Customer`

Pro Kunde existiert **genau ein Dokument**. Begründung:

- die Frontend-Kundenansicht braucht auf einen Blick: Stammdaten,
  Adresse, Zahlungsdaten und den kompletten Verleih-Verlauf
- in der normalisierten Variante sind das 4+ Tabellen
  (`customer`, `address_*`-Spalten, `billing_information`,
  `order_placement` + `orders` + `store`)
- mit embedded Daten liefert **ein einziger `find` auf `_id`**
  die komplette Ansicht, kein client-seitiges Nachladen

## Was embedded ist

| Relation (relational) | Im Dokument | Form |
|---|---|---|
| `customer.address_*` (1:1) | `address` | eingebettetes Objekt |
| `billing_information` (1:n) | `billingInformation[]` | Array von Sub-Docs |
| `order_placement` + `orders` (1:n) | `orderPlacements[]` | Array, Order-Felder denormalisiert (`orderDate`, `totalAmount`, `storeId`) |

Die m:n- bzw. 1:n-Beziehungen des Kunden werden also als **eingebettete
Arrays** abgebildet — genau die accesses, die das Frontend pro Kunde
immer gebündelt liest.

## Was referenziert bleibt

- `orderId` und `storeId` in `orderPlacements` bleiben **IDs**
  (Referenzen), keine kopierten Entities
- Begründung: Orders und Stores werden von vielen Kunden geteilt.
  Würde man sie komplett einbetten, würde sich derselbe Store-Katalog
  in jedem Kundendokument duplizieren (Daten explosion + Update-Anomalien
  wenn sich z.B. der Preis ändert)
- Filmbestand (`store_films`, `store_series` — die echte m:n des Schemas)
  gehört nicht in das Customer Aggregat: das ist der Katalog des Stores,
  nicht des Kunden. Der richtige Ort dafür wäre ein eigenes
  Store-Root-Dokument, das die Film/Series IDs (oder Sub-Docs) einbettet

## Beispiel-Dokument

```json
{
  "_id": 1,
  "nickname": "seed.user.1",
  "email": "seed.user.1@example.com",
  "firstName": "James",
  "lastName": "Jameson",
  "permission": "Admin",
  "address": {
    "street": "Jamesonstreet 1",
    "city": "Wienna",
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
      "orderId": 1,
      "orderDate": "2020-01-01",
      "orderReturnDate": "2020-01-15",
      "actualReturnDate": "2020-01-11",
      "totalAmount": 42.00,
      "storeId": 1
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

- **READ**: 1 Query statt Customer + Billings + Placements + Orders + Store
- **CREATE**: `updateLastNameAll` o.ä. bleibt simpel, aber
  Änderungen an geteilter Order-Info (`totalAmount`) werden im
  Kundendokument **denormalisiert mitkopiert** — Inkonsistenz-Risiko,
  für den Benchmark akzeptabel
- Grösse: Kundendokumente bleiben klein (< 16MB Limit locker)
