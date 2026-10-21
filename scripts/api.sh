#!/bin/bash
# Compila e arranca só a API (backend). Para abrir também a aplicação desktop
# use ../executar.sh. Requer a variável TURISMO_DB_PASSWORD (definida no ~/.zshrc).

SCRIPT_DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )"
cd "$SCRIPT_DIR/.." || exit 1

if [ -z "$TURISMO_DB_PASSWORD" ]; then
    echo "ERRO: a variável TURISMO_DB_PASSWORD não está definida."
    echo "Execute: export TURISMO_DB_PASSWORD=<password>"
    exit 1
fi

PORT=$(grep -E '^api.port=' config/turismo.properties | cut -d= -f2 | tr -d '[:space:]')
PORT=${PORT:-8083}
if lsof -nP -iTCP:"$PORT" -sTCP:LISTEN > /dev/null 2>&1; then
    echo "A porta $PORT está ocupada, a parar o servidor anterior..."
    pkill -f turismo-api.jar
    pkill -f com.turismo.api.TurismoApiServer
    sleep 1
fi

echo "A compilar (mvn package)..."
mvn -q -DskipTests -pl backend -am package || { echo "ERRO: a compilação falhou."; exit 1; }

echo "API disponível em http://localhost:$PORT/api (Ctrl+C para parar)"
java -jar backend/target/turismo-api.jar
