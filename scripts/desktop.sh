#!/bin/bash
# Abre só a aplicação desktop já compilada (ver compilar.sh). A API tem de estar a correr.
# Uso: ./scripts/desktop.sh
cd "$(dirname "$0")/.." || exit 1
java -jar frontend/target/turismo-desktop.jar
