@echo off
rem ------------------------------------------------------------
rem Gestao de Turismo - arranque em Windows
rem 1. compila (se ainda nao houver jars), 2. arranca a API numa janela
rem minimizada se nao estiver a correr, 3. abre a aplicacao desktop.
rem ------------------------------------------------------------
setlocal EnableDelayedExpansion
cd /d "%~dp0"

if "%TURISMO_DB_PASSWORD%"=="" (
    if not exist "config\turismo.local.properties" (
        echo ERRO: defina a palavra-passe da base de dados:
        echo   set TURISMO_DB_PASSWORD=...
        echo   ^(ou crie config\turismo.local.properties a partir do ficheiro .example^)
        exit /b 1
    )
)

if not exist "backend\target\turismo-api.jar" goto compilar
if not exist "frontend\target\turismo-desktop.jar" goto compilar
goto arrancar

:compilar
echo A compilar o projeto (mvn package)...
call mvn -q -DskipTests package
if errorlevel 1 (
    echo ERRO: a compilacao falhou.
    exit /b 1
)

:arrancar
set PORT=8083
for /f "tokens=2 delims==" %%p in ('findstr /b "api.port=" config\turismo.properties') do set PORT=%%p
set URL=http://localhost:%PORT%/api/estado

curl -s -o nul "%URL%"
if not errorlevel 1 (
    echo API ja esta a correr na porta %PORT%.
    goto app
)

echo A arrancar a API na porta %PORT% ^(log em backend\server.log^)...
start "Turismo API" /min cmd /c "java -jar backend\target\turismo-api.jar > backend\server.log 2>&1"
set /a tentativas=0
:esperar
timeout /t 1 /nobreak > nul
curl -s -o nul "%URL%"
if not errorlevel 1 goto app
set /a tentativas+=1
if !tentativas! lss 30 goto esperar
echo ERRO: a API nao arrancou. Veja backend\server.log.
exit /b 1

:app
echo A abrir a aplicacao...
java -jar frontend\target\turismo-desktop.jar
endlocal
