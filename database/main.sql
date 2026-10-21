-- ============================================================
-- TRABALHO PRÁTICO - BASE DE DADOS I (ISCTEM 2026)
-- GRUPO 1: GESTÃO DE TURISMO EM MOÇAMBIQUE
-- main.sql: corre todos os scripts do projeto pela ordem certa (Oracle 10g XE)
--
-- O @@ procura os ficheiros na mesma pasta deste script. O SPOOL do passo 9
-- escreve na pasta de onde se abriu o SQL*Plus, por isso execute a partir de
-- database/:
--   cd database
--   sqlplus yosef@//192.168.64.4:1521/XE @main.sql
--
-- tabelas.sql e Sequences.sql começam com DROP: na primeira execução esses
-- DROP dão erro (os objetos ainda não existem), o que é normal.
-- Para apagar TUDO antes (incluindo histórico e views), use apagarAtualizar.sql.
--
-- As views (passo 8) precisam do privilégio CREATE VIEW, que o SYSTEM concede
-- uma vez com conceder_privilegios.sql; sem ele dão ORA-01031.
-- ============================================================

SET ECHO ON
SET SERVEROUTPUT ON
SET DEFINE OFF
alter session set nls_date_format = 'DD-MM-YYYY';

-- 1. Estrutura base: tabelas e sequences
@@tabelas.sql
@@Sequences.sql

-- 2. Tabelas de histórico (usadas pelos triggers de auditoria)
@@Tabelas_de_Historico.sql

-- 3. Índices
@@Index.sql

-- 4. Triggers (antes dos dados, para os INSERT já serem validados)
@@Triggers.sql

-- 5. Procedures
@@procedures.sql

-- 6. Dados de teste
@@Inserts.sql
COMMIT;

-- 7. Funções (depois dos dados: o fim do script tem testes que os usam)
@@functions.sql

-- 8. Views
@@views.sql

-- 9. Cálculo do espaço por atributo (o resultado vai para o relatório)
SPOOL relatorio_calculo2.txt
@@calculoEspacoPorAtributo.sql
SPOOL OFF

-- 10. Verificação final
-- O privilégio CREATE VIEW tem de aparecer (ver conceder_privilegios.sql)
SELECT privilege FROM session_privs WHERE privilege = 'CREATE VIEW';

-- Objetos inválidos e erros de compilação: não devem devolver nenhuma linha
SELECT object_type, object_name, status FROM user_objects WHERE status <> 'VALID' ORDER BY object_type, object_name;
SELECT name, type, line, position, text FROM user_errors ORDER BY name, sequence;

-- Views: devem existir 24
SELECT COUNT(*) AS total_views FROM user_views;

SET ECHO OFF
