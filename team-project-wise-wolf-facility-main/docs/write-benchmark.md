# Write-Benchmark (INSERT, Issue #8)

Misst, wie lange das Einfügen eines kompletten Datensatzes in eine leere Datenbank dauert –
für **PostgreSQL** (`rel`), **MongoDB embedded** (`mongo`) und **MongoDB referenziert** (`mongo-ref`),
jeweils mit 100 / 1.000 / 100.000 Kunden und mehreren Wiederholungen.
Gestartet wird wie die übrigen Benchmarks über **k6** (`MODE=insert`), gemessen wird in der App.
Code: `src/main/java/at/spengergasse/sj2526pos1javaseed/benchmark/write/`.

## Ausführen

Ein Befehl startet alles Nötige und führt k6 aus:

```powershell
docker compose -f docker-compose.yml -f docker-compose.benchmark.yml run --rm k6
```

Mit Parametern (siehe [Parameter](#parameter)):

```powershell
$env:SCALES="100,1000"; $env:REPETITIONS="3"
docker compose -f docker-compose.yml -f docker-compose.benchmark.yml run --rm k6
Remove-Item Env:SCALES, Env:REPETITIONS     # PowerShell behält $env:-Variablen sonst für die Sitzung
```

```bash
SCALES=100,1000 REPETITIONS=3 docker compose -f docker-compose.yml -f docker-compose.benchmark.yml run --rm k6
```

Was dabei passiert (`docker-compose.benchmark.yml`):

1. `postgres` und `mongo` werden gestartet, falls sie nicht laufen.
2. `benchmark-app` startet: der `app`-Service mit freigeschaltetem Test-Endpoint
   (`APP_TESTDATA_ENDPOINT_ENABLED=true`), ohne Seed und ohne veröffentlichten Port.
3. Sobald die App bereit ist (Health-Check), läuft k6 im selben Docker-Netzwerk
   (`grafana/k6:2.4.0`, `MODE=insert` wenn nichts gesetzt ist).
4. Die Ergebnisdateien landen in `./benchmark-results/`.

Danach läuft `benchmark-app` weiter (nur im Docker-Netzwerk erreichbar); weitere Läufe starten
dadurch sofort. Stoppen mit `docker stop benchmark_app`.
**Nach Code-Änderungen** das Image neu bauen:
`docker compose -f docker-compose.yml -f docker-compose.benchmark.yml build benchmark-app`.

Ein normales `docker compose up` lädt diese Datei nicht: der `app`-Service hat den Endpoint
weiterhin **aus**, und k6 startet nie automatisch. Beide Benchmark-Services haben zusätzlich das
Profil `benchmark`, laufen also auch bei `up` mit beiden Dateien nicht mit.

Die bestehenden k6-Modi laufen genauso, z. B. `$env:MODE="compare"; $env:SCALES="100,1000,100000"`.

Warum alles im Docker-Netzwerk: unter Docker Desktop (Windows/macOS) läuft jede Verbindung vom Host zu
`localhost:15432` / `localhost:27017` über die Port-Weiterleitung von Docker. Die verzögert kleine
Nachrichten um ~40 ms – je nach Datenbank unterschiedlich stark (siehe
[Messumgebung](#messumgebung-host-vs-docker-netzwerk)). Im Container erreicht die App die DBs direkt.

> **Achtung:** Der Benchmark **leert** die gewählten Datenbanken vor jeder Wiederholung (wie der
> Testdatengenerator, inkl. Seed-Daten). Danach enthält jede DB den Datensatz der letzten Skalierung.
> Die Host-Allowlist `app.testdata.allowed-hosts` wird vor dem ersten Löschen geprüft.
> Nicht gleichzeitig mit `/tests/run`, `/tests/compare` oder `/tests/testdata` starten –
> die Läufe würden sich gegenseitig die Daten löschen und die Zeiten verfälschen.

## Parameter

Alle optional; ohne Angabe gelten die Vorgaben aus #8.

| k6 Env-Var    | JSON-Feld (`POST /tests/insert`) | Default               | Bedeutung |
|---------------|----------------------------------|-----------------------|-----------|
| `DATABASES`   | `databases`                      | `rel,mongo,mongo-ref` | Beliebige Teilmenge, einzeln wählbar. |
| `SCALES`      | `scales`                         | `100,1000,100000`     | Anzahl Kunden pro Datensatz (10 – 100.000). |
| `REPETITIONS` | `repetitions`                    | `5`                   | Gemessene Wiederholungen pro Kombination (gehen in die Statistik ein). |
| `WARMUPS`     | `warmups`                        | `1`                   | Aufwärmläufe pro Kombination (werden exportiert, aber nicht ausgewertet). |
| `SEED`        | `seed`                           | `42`                  | Seed des Testdatengenerators. |
| `CHUNK_SIZE`  | `chunkSize`                      | `1000`                | Kunden pro Chunk = pro JDBC-Batch-Transaktion bzw. pro `insertMany`. |
| `MAX_WAIT_S`, `POLL_S` | –                       | `7200`, `5`           | wie bei den anderen k6-Modi (`BASE_URL` setzt die Compose-Datei) |

Ungültige Werte → HTTP 400 mit Fehlermeldung, es wird nichts gelöscht.
Defaults: 3 DBs × 3 Skalierungen × (1 + 5) Läufe = 54 Messungen, davon 45 ausgewertet;
Laufzeit im Docker-Netzwerk ca. 3–4 Minuten, fast alles davon für 100.000 Kunden.

Endpoints (nur mit `app.testdata.endpoint-enabled=true`, gleicher Schalter wie `/tests/testdata`;
`benchmark-app` setzt ihn, sonst ist er aus):

| Endpoint | Inhalt |
|----------|--------|
| `POST /tests/insert` | startet asynchron (202), Antwort = Report mit `id`, Status `RUNNING` |
| `GET /tests/insert/{id}` | Report: `status`, `completedSteps/totalSteps`, `measurements`, `summaries`, `environment` |
| `GET /tests/insert/{id}/measurements.csv` | alle Einzelmessungen |
| `GET /tests/insert/{id}/summary.csv` | Statistik |

## Ablauf

```
Parameter + Host-Allowlist prüfen (vor jedem Löschen)
pro Skalierung:
    Datensatz einmal generieren (fixer Seed, nicht gemessen)
    CustomerDocuments einmal mappen (nicht gemessen)
    pro Wiederholung (erst Warmups, dann gemessene):
        pro DB (Reihenfolge rotiert pro Wiederholung):
            prepare: DB leeren                       nicht gemessen
            execute: kompletten Datensatz einfügen   gemessen (System.nanoTime)
            verify:  Zeilen/Dokumente zählen          nicht gemessen
Statistik → Report (k6 speichert CSV + JSON)
```

Schlägt die Prüfung fehl (z. B. fehlende Zeilen), endet der Lauf mit Status `FAILED`;
bis dahin gemessene Werte bleiben im Report und werden trotzdem gespeichert.

### Was genau gemessen wird

| DB          | gemessen |
|-------------|----------|
| `rel`       | Katalog (Filme, Serien, Stores + m:n) in einer Transaktion, dann pro Chunk eine Transaktion mit JDBC-Batches für customer, billing_information, orders, order_placement (`RelationalTestDataWriter`, `reWriteBatchedInserts=true`). Inklusive Commit. |
| `mongo`     | pro Chunk ein `insertMany` der eingebetteten `CustomerDocument`s (`MongoCustomerRepository.insertBatch`, inkl. BSON-Konvertierung). Der Katalog steckt in jedem Placement. |
| `mongo-ref` | Katalog einmal (`ref_stores`, `ref_films`, `ref_series`, `ref_store_films`, `ref_store_series`), dann pro Chunk je ein `insertMany` für `ref_customers`, `ref_addresses`, `ref_billings`, `ref_order_placements`, `ref_orders` (`MongoReferencedRepository.insertCatalog` / `insertCustomers`, inkl. Normalisierung + BSON-Konvertierung). |

`records` in den Ergebnissen = Anzahl Kunden (= Mongo-Dokumente im embedded Modell). Relational
und referenziert werden dafür deutlich mehr Zeilen/Dokumente geschrieben (Billings, Orders,
Placements, Katalog). Alle drei Varianten speichern logisch denselben Datensatz.

## Faire Messung

- **Gleiche Daten:** ein Datensatz pro Skalierung aus dem Testdatengenerator (fixer Seed),
  dieselben Objekte für alle DBs und alle Wiederholungen. Mengengerüst siehe
  [testdatengenerator.md](testdatengenerator.md).
- **Nicht gemessen:** Datengenerierung, Mapping auf `CustomerDocument`, App-Start, HTTP,
  Leeren der DB, Prüfung.
- **Leere DB vor jeder Wiederholung:** `TRUNCATE` der Generator-Tabellen bzw. Drop der
  Collections. Die Mongo-Collections werden direkt danach leer neu angelegt, damit das Anlegen
  nicht im ersten gemessenen Insert passiert. Indexe: nur die Schema-Indexe (PostgreSQL: PKs,
  FKs, Unique-Constraints aus Flyway; MongoDB: `_id`).
- **Verbindungen:** Hikari-Pool und `MongoClient` werden beim App-Start aufgebaut. Warmups fangen
  JIT, erste Prepared Statements und Connection-Pool-Effekte ab.
- **Batching/Transaktionen:** gleiche Chunk-Größe für alle DBs. PostgreSQL: eine Transaktion pro
  Chunk (realistisches Bulk-Load-Muster; MongoDB nutzt hier keine Multi-Dokument-Transaktionen,
  ein `insertMany` pro Chunk ist das Gegenstück). MongoDB: ein `insertMany` pro Chunk und
  Collection (ordered, Standard des Treibers).
- **Reihenfolge:** die DB-Reihenfolge rotiert pro Wiederholung, damit keine DB systematisch
  nach der Hintergrundarbeit (Checkpoints, Autovacuum) einer anderen läuft.
- **Ausreißer:** alle Einzelmessungen bleiben im Export, die Statistik enthält Median und
  Standardabweichung neben dem Mittelwert.

### Dauerhaftigkeit (Durability)

Die beiden Systeme bestätigen einen Schreibvorgang zu unterschiedlichen Zeitpunkten, gemessen wird
jeweils mit den **Standardeinstellungen**:

- **PostgreSQL** (`synchronous_commit = on`): der Commit kehrt erst zurück, wenn das WAL auf die
  Platte geschrieben (fsync) ist. Jede Chunk-Transaktion bezahlt also einen Flush.
- **MongoDB** (Standalone, Write Concern im Client nicht gesetzt → Server-Default `w:1`): der
  Insert wird bestätigt, sobald er im Speicher angewendet ist; das Journal wird nicht abgewartet
  (WiredTiger schreibt es alle 100 ms). Ein Absturz direkt nach der Bestätigung kann die letzten
  Schreibvorgänge verlieren.

MongoDB hat damit beim Schreiben einen strukturellen Vorteil. Die tatsächlichen Einstellungen stehen
in jeder JSON-Datei unter `environment` (`postgresSynchronousCommit`, `mongoWriteConcernClient`,
`mongoTopology`; `mongoWriteConcernServerDefault` ist auf einem Standalone-Server `n/a`, weil
`getDefaultRWConcern` dort nicht unterstützt wird – es gilt `w:1`).

## Ausgabe

k6 schreibt pro Lauf drei Dateien nach `./benchmark-results/`, Name `insert-<yyyyMMdd-HHmmss>` (UTC-Startzeit):

| Datei | Inhalt |
|-------|--------|
| `insert-….csv`         | jede Einzelmessung. Spalten wie die k6-CSV aus #6/#7 (`id,database,scale,indexVariant,status,operation,durationMs`) plus `repetition,warmup,records`. `durationMs` mit 3 Nachkommastellen. |
| `insert-…-summary.csv` | pro DB und Skalierung: `n,meanMs,medianMs,minMs,maxMs,stddevMs,records,recordsPerSecond` (ohne Warmups, Standardabweichung = Stichprobe). |
| `insert-….json`        | kompletter Report: `config`, `status`, `environment` (DB-Versionen, Durability, Insert-Pfad), alle `measurements`, `summaries`. |

## Messumgebung: Host vs. Docker-Netzwerk

Gleicher Code, gleiche Daten, Mittelwert in ms (3 Wiederholungen), App einmal auf dem Windows-Host
und einmal im Docker-Netzwerk:

| Kunden | `rel` Host | `rel` Docker | `mongo` Host | `mongo` Docker | `mongo-ref` Host | `mongo-ref` Docker |
|-------:|-----------:|-------------:|-------------:|---------------:|-----------------:|-------------------:|
| 10     | 25         | 25           | 49           | 9              | 358              | 13                 |
| 100    | 166        | 44           | 62           | 17             | 356              | 14                 |
| 1.000  | 251        | 192          | 378          | 170            | 219              | 45                 |

Über den Host bleibt `mongo-ref` bis 300 Kunden konstant bei ~355 ms, und die Werte springen in
~40-ms-Stufen – typisch für TCP-Verzögerungen in der Port-Weiterleitung von Docker Desktop, nicht für
die Datenbanken. Im Docker-Netzwerk wachsen alle Zeiten erwartungsgemäß mit der Datenmenge.

## Abgrenzung zum Query-Benchmark (`BenchmarkRunner`)

Der bestehende Benchmark unter `benchmark/` (`BenchmarkRunner`, `/tests/run`, `/tests/compare`)
misst Finds, Updates, Delete und die Aggregation. Beide laufen über k6; der Insert-Benchmark ist
bewusst ein eigener Lauf, nutzt aber dieselben Bausteine, wo es die Anforderungen aus #8 zulassen.

**Gemeinsam genutzt:**

- Dokumentmodell `CustomerDocument`
- `MongoCustomerRepository.insertBatch` (Insert für `mongo`)
- `MongoReferencedRepository` (Normalisierung + Insert für `mongo-ref`; `insertBatch` ist dafür
  in `insertCatalog` und `insertCustomers` aufgeteilt, Verhalten von `insertBatch` unverändert)
  inkl. der `ref_*`-Collection-Namen
- k6-Skript `k6/benchmark.js` (eigener `MODE=insert`) und die CSV-Spalten der Auswertung
  (`id,database,scale,indexVariant,status,operation,durationMs`)
- Muster async-Start + Polling (wie `/tests/run` + `/tests/results`)

**Bewusst getrennt:**

| Thema | `BenchmarkRunner` | Insert-Benchmark | Grund |
|---|---|---|---|
| Testdaten | `generateDocuments` (synthetisch, `Benchmarkstadt`) | Testdatengenerator (fixer Seed) | #8 verlangt den Generator; die Finds im Query-Benchmark hängen an `Benchmarkstadt` |
| Reset | löscht nur `bench.user.*`, Seed-Daten bleiben | leert die DB komplett | Insert-Zeiten nur auf leerer DB vergleichbar |
| PostgreSQL-Insert | JPA (`JpaPersistenceRepository`) | JDBC-Batch (`RelationalTestDataWriter`) | Entscheidung für #8: Treiber gegen Treiber |
| Zeitmessung | ein Lauf, ganze ms | Warmups + Wiederholungen, ns-genau, Statistik | Anforderungen #8 |

**Für die Auswertung:** die INSERT-Zahlen für #8 kommen aus `MODE=insert`, nicht aus
`insertBatchMs` des `BenchmarkRunner` (anderer Datensatz, anderer Pfad, keine Wiederholungen).
