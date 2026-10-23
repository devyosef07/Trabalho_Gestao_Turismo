#!/bin/bash
# Compila os dois módulos (API e aplicação desktop) com o Maven, sem testes.
# Uso: ./scripts/compilar.sh
cd "$(dirname "$0")/.." || exit 1
mvn -q -DskipTests package && echo "Compilação concluída: backend/target/turismo-api.jar e frontend/target/turismo-desktop.jar"
