#!/bin/bash
# O projeto usa Maven: compila os dois módulos (API e aplicação desktop).
cd "$(dirname "$0")/.." && mvn -q -DskipTests package && echo "Compilação concluída: backend/target/turismo-api.jar e frontend/target/turismo-desktop.jar"
