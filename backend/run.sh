#!/bin/bash
# Arranca a API já compilada (ver compile.sh). Requer TURISMO_DB_PASSWORD.
cd "$(dirname "$0")/.." && java -jar backend/target/turismo-api.jar
