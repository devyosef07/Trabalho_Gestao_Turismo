-- ============================================================
-- TRABALHO PRÁTICO - BASE DE DADOS I (ISCTEM 2026)
-- GRUPO 1: GESTÃO DE TURISMO EM MOÇAMBIQUE
-- conceder_privilegios.sql: PRIVILÉGIOS (Oracle 10g XE)
--
-- Executar UMA vez como SYSTEM (ou SYS), não como o dono do esquema:
--   cd database
--   sqlplus system@//192.168.64.4:1521/XE @conceder_privilegios.sql
--
-- Porquê: no Oracle 10g os roles CONNECT e RESOURCE não incluem CREATE VIEW.
-- Sem este privilégio o "create or replace view" de views.sql falha com
-- ORA-01031 (insufficient privileges) e nenhuma das 24 views é criada.
--
-- Se o esquema do grupo tiver outro nome, troque "yosef" abaixo.
-- ============================================================

GRANT CREATE VIEW TO yosef;

-- Confirmação: deve aparecer CREATE VIEW
SELECT grantee, privilege FROM dba_sys_privs WHERE grantee = 'YOSEF' ORDER BY privilege;
