# Hel_Dra_Projekt-Mongo-versus-rel.DB

K6 Tests starten mit (K6 muss installiert sein): 

(in cmd)
----------------
k6 run k6\benchmark.js
set DATABASE=rel&& set SCALE=10000&& set INDEX=true&& k6 run k6\benchmark.js

set MODE=crud&& set VUS=10&& set DURATION=30s&& k6 run k6\benchmark.js
----------------