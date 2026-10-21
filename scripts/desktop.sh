#!/bin/bash
# Abre a aplicação desktop já compilada (ver compile.sh). A API tem de estar a correr.
cd "$(dirname "$0")/.." && java -jar frontend/target/turismo-desktop.jar
