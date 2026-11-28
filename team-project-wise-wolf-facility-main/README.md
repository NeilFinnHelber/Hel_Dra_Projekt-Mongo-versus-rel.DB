[![Review Assignment Due Date](https://classroom.github.com/assets/deadline-readme-button-22041afd0340ce965d47ae6ef1cefeee28c7c493a6346c4f15d667ab976d596c.svg)](https://classroom.github.com/a/PFQW70f3)


To start:
docker compose up
run: sj2526JavaSeedApplication
in a chosen browser go to: http://localhost:8080/

## Benchmarks ausführen (k6)

```
docker compose -f docker-compose.yml -f docker-compose.benchmark.yml run --rm k6
```

Startet DBs + eine Benchmark-App und führt k6 im Docker-Netzwerk aus (Default `MODE=insert`,
andere Modi über `MODE=benchmark|compare|crud`). Ergebnisse in `benchmark-results/`.
Ein normales `docker compose up` startet k6 nicht. Details: `docs/write-benchmark.md`.
