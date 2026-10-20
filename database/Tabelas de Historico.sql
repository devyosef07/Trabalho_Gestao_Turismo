CREATE TABLE auditoria_alojamento (ID_alojamento  VARCHAR2(6), nome_alojamento  VARCHAR2(50), accao VARCHAR2(6), data_accao DATE, utilizador VARCHAR2(30));

CREATE TABLE auditoria_destino (ID_destino  VARCHAR2(6), nome_destino  VARCHAR2(80), accao VARCHAR2(6), data_accao DATE, utilizador VARCHAR2(30));

CREATE TABLE auditoria_atracao (ID_  VARCHAR2(6), tipo_atracao  VARCHAR2(30), accao VARCHAR2(6), data_accao DATE, utilizador VARCHAR2(30));

CREATE TABLE auditoria_turista (ID_  VARCHAR2(6), nome_turista  VARCHAR2(80), accao VARCHAR2(6), data_accao DATE, utilizador VARCHAR2(30));

CREATE TABLE auditoria_tipoAlojamento (tipo_alojamento  VARCHAR2(11), cap_alojamento  VARCHAR2(2), accao VARCHAR2(6), data_accao DATE, utilizador VARCHAR2(30));

CREATE TABLE auditoria_reserva (ID_reserva  VARCHAR2(6), ID_turista  VARCHAR2(6), accao VARCHAR2(6), data_accao DATE, utilizador VARCHAR2(30));

CREATE TABLE auditoria_operador (ID_operador  VARCHAR2(6), nome_operador  VARCHAR2(25), accao VARCHAR2(6), data_accao DATE, utilizador VARCHAR2(30));

CREATE TABLE auditoria_transporte (ID_transporte  VARCHAR2(6), tipo_transporte  VARCHAR2(15), accao VARCHAR2(6), data_accao DATE, utilizador VARCHAR2(30));

CREATE TABLE auditoria_pagamento (ID_pagamento  VARCHAR2(6), metodo_pagamento  VARCHAR2(15), accao VARCHAR2(6), data_accao DATE, utilizador VARCHAR2(30));

CREATE TABLE auditoria_turistaDestino (ID_turista  VARCHAR2(6), ID_destino  VARCHAR2(6), accao VARCHAR2(6), data_accao DATE, utilizador VARCHAR2(30));

CREATE TABLE auditoria_operadorAlojamento (ID_operador  VARCHAR2(6), ID_alojamento  VARCHAR2(6), accao VARCHAR2(6), data_accao DATE, utilizador VARCHAR2(30));

CREATE TABLE auditoria_operadorTransporte (ID_operador  VARCHAR2(6), ID_transporte  VARCHAR2(6), accao VARCHAR2(6), data_accao DATE, utilizador VARCHAR2(30));

CREATE TABLE auditoria_operadorTurista (ID_operador  VARCHAR2(6), ID_turista  VARCHAR2(6), accao VARCHAR2(6), data_accao DATE, utilizador VARCHAR2(30));

CREATE TABLE auditoria_reservaAlojamento (ID_reserva  VARCHAR2(6), ID_alojamento  VARCHAR2(6), accao VARCHAR2(6), data_accao DATE, utilizador VARCHAR2(30));

CREATE TABLE auditoria_turistaPreferencia (ID_turista  VARCHAR2(6), prefrencia  VARCHAR2(200), accao VARCHAR2(6), data_accao DATE, utilizador VARCHAR2(30));