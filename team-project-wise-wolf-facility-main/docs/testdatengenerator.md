# Testdatengenerator

Reproduzierbarer Generator, der PostgreSQL und MongoDB mit **demselben** Datensatz befüllt,
damit der Mongo-vs-Relational-Vergleich auf identischen Daten läuft.
Code: `src/main/java/at/spengergasse/sj2526pos1javaseed/testdata/`.

## Ausführen (empfohlen: App läuft, Generator per HTTP)

Die App wird **einmal** gestartet, Einfügen und Löschen werden dann per HTTP ausgelöst.
Die zurückgegebenen Zeiten enthalten damit nur Generierung + Inserts, **nicht** den App-Start.

**1. Datenbanken und App starten**

```powershell
docker compose up -d postgres mongo
./mvnw spring-boot:run '-Dspring-boot.run.arguments=--app.testdata.endpoint-enabled=true --app.seed.enabled=false'
```

(`app.seed.enabled=false`: der normale Start-Seed ist unnötig, die Daten werden ohnehin geleert.)

**2. Einfügen** – leert die Ziel-DB(s) und befüllt sie neu. Alle Felder optional
(Defaults = `app.testdata.*`), `target` = `rel`, `mongo` oder `both`:

```powershell
Invoke-RestMethod -Method Post -Uri http://localhost:8080/tests/testdata -ContentType 'application/json' -Body '{"count":10000,"seed":42,"target":"both","chunkSize":1000}'
```

**3. Nur löschen** (ohne neu zu befüllen):

```powershell
Invoke-RestMethod -Method Delete -Uri 'http://localhost:8080/tests/testdata?target=both'
```

Bash / Git Bash (in PowerShell `curl.exe` statt `curl` verwenden, `curl` ist dort ein Alias):

```bash
./mvnw spring-boot:run -Dspring-boot.run.arguments="--app.testdata.endpoint-enabled=true --app.seed.enabled=false"
curl -X POST http://localhost:8080/tests/testdata -H "Content-Type: application/json" -d '{"count":10000,"target":"both"}'
curl -X DELETE "http://localhost:8080/tests/testdata?target=both"
```

### Antwort

Einfügen (`POST`), alle Zeiten in ms:

| Feld           | Inhalt |
|----------------|--------|
| `wipeMs`       | Leeren der Ziel-DB(s) vor dem Einfügen |
| `generationMs` | Daten im Speicher erzeugen (einmal für beide DBs) |
| `postgresMs`   | reine Insert-Zeit PostgreSQL (Katalog + Kunden) |
| `mongoMs`      | reine Insert-Zeit MongoDB (Mapping auf `CustomerDocument` + `insertBatch`) |
| `totalMs`      | Generierung + Inserts, ohne Wipe |

Außerdem `sizes`, `postgresRows` (Zeilen pro Tabelle) und `mongoSummary` (Dokumente, Ø-Größe).

Löschen (`DELETE`): `postgresMs` (`TRUNCATE` der Generator-Tabellen), `mongoMs` (Drop der Collection
`customers`), `totalMs` (Summe), danach `postgresRows` / `mongoSummary` zur Kontrolle (alles 0).
Achtung: das ist ein Komplett-Leeren, kein zeilenweises Löschen – nicht mit dem Delete-Benchmark
aus `/tests/run` vergleichbar.

### Hinweise

- Die Endpoints existieren nur mit `app.testdata.endpoint-enabled=true` (Default `false`), weil sie
  die DB(s) **leeren**. Die Host-Allowlist gilt genauso.
- Es läuft immer nur ein Lauf gleichzeitig.
- Ungültige Parameter (z. B. `count` < 10) → HTTP 400 mit Fehlermeldung, es wird nichts gelöscht.
- Der erste Lauf nach dem Start ist durch JIT-Warmup langsamer → für Messungen einen Aufwärmlauf verwerfen.

## Alternative: einmaliger Lauf beim App-Start (CLI-Modus)

Generiert direkt beim Start und **beendet die App danach** (Exit-Code 0, bei Fehler 1). Praktisch
zum schnellen Befüllen, aber die Gesamtlaufzeit enthält den App-Start – für Messungen den
Endpoint oben verwenden. Fortschritt und Zeiten stehen im Log.

```powershell
# beide DBs, 1000 Kunden, Seed 42 (Defaults)
./mvnw spring-boot:run '-Dspring-boot.run.arguments=--app.testdata.enabled=true'

# nur PostgreSQL, 100.000 Kunden, Seed 7
./mvnw spring-boot:run '-Dspring-boot.run.arguments=--app.testdata.enabled=true --app.testdata.count=100000 --app.testdata.seed=7 --app.testdata.target=rel'

# über das gebaute jar
java -jar target/sj2526-pos1-java-seed-0.0.1-SNAPSHOT.jar --app.testdata.enabled=true --app.testdata.count=100
```

In PowerShell das `-D...`-Argument immer komplett in einfache Anführungszeichen setzen, sonst meldet
Maven `Unknown lifecycle phase ".run.arguments=..."`. In Bash stattdessen
`-Dspring-boot.run.arguments="--app.testdata.enabled=true ..."`.

## Parameter

| Property                     | Default | Bedeutung |
|------------------------------|---------|-----------|
| `app.testdata.enabled`       | `false` | CLI-Modus: beim Start generieren, dann App beenden. |
| `app.testdata.count`         | `1000`  | **Anzahl Kunden** (10 – 100.000) = Anzahl Mongo-Dokumente = Zeilen in `customer`. |
| `app.testdata.seed`          | `42`    | Zufalls-Seed. Gleicher Seed + count → gleicher Datensatz. |
| `app.testdata.target`        | `both`  | `rel`, `mongo` oder `both`. |
| `app.testdata.chunk-size`    | `1000`  | Kunden pro Schreib-Chunk (1 – 10.000). Beeinflusst die Daten **nicht**. |
| `app.testdata.allowed-hosts` | `localhost,127.0.0.1,::1,postgres,mongo,host.docker.internal` | Nur auf diese DB-Hosts darf geschrieben werden, sonst Abbruch vor dem Löschen. |
| `app.testdata.endpoint-enabled` | `false` | Aktiviert `POST` / `DELETE /tests/testdata` in der laufenden App (empfohlen für Messungen). |

## Mengengerüst

Primäre Größe ist die Kundenanzahl `n`, alles andere wird abgeleitet:

| Entität               | Formel                         | n=100 | n=1.000 | n=100.000 |
|-----------------------|--------------------------------|------:|--------:|----------:|
| customer              | `n`                            | 100   | 1.000   | 100.000   |
| store                 | `max(3, ceil(n/1000))`         | 3     | 3       | 100       |
| film                  | `max(20, ceil(n/200))`         | 20    | 20      | 500       |
| series                | `max(10, film/2)`              | 10    | 10      | 250       |
| store_films pro Store | `max(10, ceil(film/store))`    | 10    | 10      | 10        |
| store_series pro Store| `max(5, ceil(series/store))`   | 5     | 5       | 5         |
| billing_information   | 1–2 pro Kunde (Ø 1,3)          | ~130  | ~1.300  | ~130.000  |
| orders / order_placement | 1–3 pro Kunde (Ø 2), 1:1    | ~200  | ~2.000  | ~200.000  |

Der Katalog ist bewusst klein: im Mongo-Modell steckt in **jedem** Placement der komplette
Store-Katalog. Bei 100.000 Kunden ergibt das Ø ~6 KB pro Dokument (max ~9 KB),
~570 MB Daten bzw. ~150 MB Storage (komprimiert).

## Wie dieselben Daten in beide DBs kommen

Ablauf in `TestDataService` (aufgerufen vom `TestDataController` bzw. im CLI-Modus vom `TestDataRunner`):

1. `TestDataGenerator` erzeugt ein neutrales Modell (`TestData`-Records), unabhängig von JPA und Mongo.
2. Der Katalog (Filme, Serien, Stores inkl. m:n) wird einmal erzeugt.
3. Kunden werden in Chunks erzeugt; **jeder Chunk wird genau einmal generiert** und dieselben
   Objekte gehen an beide Writer:
   - `RelationalTestDataWriter`: JDBC-Batch-Inserts normalisiert in alle Tabellen (FK-Reihenfolge).
   - `MongoTestDataWriter`: Mapping auf das bestehende `CustomerDocument` (Store + Katalog pro
     Placement eingebettet) und Insert über `MongoCustomerRepository.insertBatch`.
4. Jeder Datensatz bekommt einen eigenen Zufalls-Seed aus `(seed, typ, index)`. Dadurch hängt das
   Ergebnis nicht von Chunk-Größe, Reihenfolge oder Ziel-DB ab: getrennte Läufe mit `target=rel`
   und `target=mongo` liefern dieselben Daten wie ein Lauf mit `target=both`.

Vollständigkeit: jeder Film/jede Serie ist in mindestens einem Store, jeder Store hat mindestens
eine Order, jeder Kunde mindestens ein Placement. Damit kommt alles, was relational gespeichert
wird, auch im Mongo-Dokument vor.

## Daten

- Namen, Straßennamen: [Datafaker](https://www.datafaker.net/) mit Locale `de-AT`, geseedet.
- Städte + PLZ: echte österreichische Städte, gewichtet nach Größe (Wien am häufigsten).
- Nickname/E-Mail: `vorname.nachname<id>` → garantiert eindeutig; E-Mail besteht die `Email`-Validierung.
- Kreditkarten: 16-stellig, gültige Luhn-Prüfziffer.
- Film-/Serientitel: echte Titel mit Erscheinungsjahr aus `src/main/resources/testdata/*.txt`,
  pro Seed gemischt. Bei mehr Filmen als Titeln wiederholen sich Titel (IDs bleiben eindeutig).

IDs: customer `1..n`, billing/placement `customerId*10 + k`, order `1_000_000_000 + customerId*10 + k`.
Der Order-Offset verhindert Kollisionen mit den Orders, die der `BenchmarkRunner` mit
`orderId = customerId` oberhalb der höchsten Kunden-ID anlegt.

## Sicherheit

- Gelöscht werden kann nur, wenn eines der beiden Flags gesetzt ist: ohne `app.testdata.enabled=true`
  existiert der CLI-Runner nicht, ohne `app.testdata.endpoint-enabled=true` existieren die Endpoints
  `POST`/`DELETE /tests/testdata` nicht (beide Default `false`).
- Im CLI-Modus wird `app.seed.enabled=false` erzwungen (`DataInitializer` seedet nicht) und
  der Webserver läuft nur auf einem zufälligen Port auf `127.0.0.1`
  (`TestDataModeEnvironmentPostProcessor`).
- Vor dem Löschen: Validierung aller Parameter und Host-Allowlist für jede Ziel-DB.
  Bei Fehler Abbruch (CLI: Exit-Code 1, Endpoint: HTTP 400), es wird nichts gelöscht.
- Gelöscht werden nur die Generator-Tabellen (`TRUNCATE`, `flyway_schema_history` und
  `phone_numbers` bleiben) bzw. die Collection `customers`. Das Log nennt die betroffenen DBs.
