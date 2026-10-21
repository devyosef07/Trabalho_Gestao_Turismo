#!/bin/bash
# ------------------------------------------------------------
# Gestão de Turismo — arranque em macOS / Linux
# 1. compila (se ainda não houver jars), 2. arranca a API se não estiver
# a correr, 3. abre a aplicação desktop. Ao fechar a aplicação, a API
# arrancada por este script também termina.
# ------------------------------------------------------------
cd "$(dirname "$0")" || exit 1

if [ -z "$TURISMO_DB_PASSWORD" ] && ! grep -qs '^db.password=.\+' config/turismo.local.properties; then
    echo "ERRO: defina a palavra-passe da base de dados:"
    echo "  export TURISMO_DB_PASSWORD='...'"
    echo "  (ou crie config/turismo.local.properties a partir do ficheiro .example)"
    exit 1
fi

if [ ! -f backend/target/turismo-api.jar ] || [ ! -f frontend/target/turismo-desktop.jar ]; then
    echo "A compilar o projeto (mvn package)..."
    mvn -q -DskipTests package || { echo "ERRO: a compilação falhou."; exit 1; }
fi

PORT=$(grep -E '^api.port=' config/turismo.properties | cut -d= -f2 | tr -d '[:space:]')
PORT=${PORT:-8083}
URL="http://localhost:$PORT/api/estado"

if curl -s -o /dev/null "$URL"; then
    echo "API já está a correr na porta $PORT."
else
    echo "A arrancar a API na porta $PORT (log em backend/server.log)..."
    java -jar backend/target/turismo-api.jar > backend/server.log 2>&1 &
    API_PID=$!
    trap 'kill $API_PID 2>/dev/null' EXIT
    for _ in $(seq 1 40); do
        curl -s -o /dev/null "$URL" && break
        sleep 0.5
    done
    curl -s -o /dev/null "$URL" || { echo "ERRO: a API não arrancou. Veja backend/server.log."; exit 1; }
fi

echo "A abrir a aplicação..."
java -jar frontend/target/turismo-desktop.jar
