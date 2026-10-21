-- ============================================================
-- TRABALHO PRÁTICO - BASE DE DADOS I (ISCTEM 2026)
-- GRUPO 1: GESTÃO DE TURISMO EM MOÇAMBIQUE
-- TABELAS DE HISTÓRICO (AUDITORIA) - Oracle 10g XE
-- Executar depois de tabelas.sql e antes de Triggers.sql: os triggers de
-- auditoria escrevem nestas tabelas.
--
-- NOTA: correções ao script original
--   * auditoria_atracao e auditoria_turista tinham a coluna "ID_"; passou a
--     ID_atracao e ID_turista;
--   * auditoria_tipoAlojamento guardava cap_alojamento como VARCHAR2(2); passou
--     a NUMBER(2), o tipo da coluna em Tipo_Alojamento;
--   * auditoria_turistaPreferencia: "prefrencia" passou a "preferencia".
--
-- As tabelas de histórico não são apagadas ao executar de novo o script, para
-- não se perder o registo de auditoria: se já existirem, o CREATE TABLE só dá
-- ORA-00955 (o nome já está a ser usado) e a tabela fica como estava.
-- ============================================================

--==================================================
-- CRIAÇÃO DAS TABELAS
--==================================================

CREATE TABLE auditoria_alojamento (ID_alojamento  VARCHAR2(6), nome_alojamento  VARCHAR2(50), accao VARCHAR2(6), data_accao DATE, utilizador VARCHAR2(30));

CREATE TABLE auditoria_destino (ID_destino  VARCHAR2(6), nome_destino  VARCHAR2(80), accao VARCHAR2(6), data_accao DATE, utilizador VARCHAR2(30));

CREATE TABLE auditoria_atracao (ID_atracao  VARCHAR2(6), tipo_atracao  VARCHAR2(30), accao VARCHAR2(6), data_accao DATE, utilizador VARCHAR2(30));

CREATE TABLE auditoria_turista (ID_turista  VARCHAR2(6), nome_turista  VARCHAR2(80), accao VARCHAR2(6), data_accao DATE, utilizador VARCHAR2(30));

CREATE TABLE auditoria_tipoAlojamento (tipo_alojamento  VARCHAR2(11), cap_alojamento  NUMBER(2), accao VARCHAR2(6), data_accao DATE, utilizador VARCHAR2(30));

CREATE TABLE auditoria_reserva (ID_reserva  VARCHAR2(6), ID_turista  VARCHAR2(6), accao VARCHAR2(6), data_accao DATE, utilizador VARCHAR2(30));

CREATE TABLE auditoria_operador (ID_operador  VARCHAR2(6), nome_operador  VARCHAR2(25), accao VARCHAR2(6), data_accao DATE, utilizador VARCHAR2(30));

CREATE TABLE auditoria_transporte (ID_transporte  VARCHAR2(6), tipo_transporte  VARCHAR2(15), accao VARCHAR2(6), data_accao DATE, utilizador VARCHAR2(30));

CREATE TABLE auditoria_pagamento (ID_pagamento  VARCHAR2(6), metodo_pagamento  VARCHAR2(15), accao VARCHAR2(6), data_accao DATE, utilizador VARCHAR2(30));

CREATE TABLE auditoria_turistaDestino (ID_turista  VARCHAR2(6), ID_destino  VARCHAR2(6), accao VARCHAR2(6), data_accao DATE, utilizador VARCHAR2(30));

CREATE TABLE auditoria_operadorAlojamento (ID_operador  VARCHAR2(6), ID_alojamento  VARCHAR2(6), accao VARCHAR2(6), data_accao DATE, utilizador VARCHAR2(30));

CREATE TABLE auditoria_operadorTransporte (ID_operador  VARCHAR2(6), ID_transporte  VARCHAR2(6), accao VARCHAR2(6), data_accao DATE, utilizador VARCHAR2(30));

CREATE TABLE auditoria_operadorTurista (ID_operador  VARCHAR2(6), ID_turista  VARCHAR2(6), accao VARCHAR2(6), data_accao DATE, utilizador VARCHAR2(30));

CREATE TABLE auditoria_reservaAlojamento (ID_reserva  VARCHAR2(6), ID_alojamento  VARCHAR2(6), accao VARCHAR2(6), data_accao DATE, utilizador VARCHAR2(30));

CREATE TABLE auditoria_turistaPreferencia (ID_turista  VARCHAR2(6), preferencia  VARCHAR2(200), accao VARCHAR2(6), data_accao DATE, utilizador VARCHAR2(30));
