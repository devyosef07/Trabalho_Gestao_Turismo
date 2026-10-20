-- ============================================================
-- TRABALHO PRÁTICO - BASE DE DADOS I (ISCTEM 2026)
-- GRUPO 1: GESTÃO DE TURISMO EM MOÇAMBIQUE
-- SCRIPT DE CORREÇÕES (Oracle 10g XE)
--
-- Contexto: na base de dados de desenvolvimento (verificado em 20-10-2026)
--   * nenhuma das 24 views de views.sql existe (user_views vazia);
--   * 12 triggers e 5 procedures estão INVALID (ver user_errors).
-- Um trigger INVALID faz falhar QUALQUER INSERT/UPDATE/DELETE que o dispare
-- (ORA-04098). Por isso hoje é impossível inserir em Atracao, Alojamento,
-- Reserva, Pagamento, Operador_Turista e Reserva_Alojamento, e apagar
-- Turista ou Reserva.
--
-- Este script recria esses objetos mantendo a intenção original.
-- As decisões em que foi preciso interpretar o script original estão
-- assinaladas com "NOTA".
--
-- Executar no SQL*Plus / SQL Developer, a partir da pasta database/:
--   @correcoes.sql
-- ============================================================

SET SERVEROUTPUT ON

-- ------------------------------------------------------------
-- 1. TRIGGERS DE INTEGRIDADE
-- ------------------------------------------------------------

-- NOTA: o original fazia "DELETE FROM turista WHERE ID_reserva = ..."; a tabela
-- Turista não tem ID_reserva e apagar o turista ao apagar uma reserva não faz
-- sentido. Mantêm-se só os deletes dos dependentes da reserva.
CREATE OR REPLACE TRIGGER deleteReserva
AFTER DELETE ON reserva
FOR EACH ROW
BEGIN
    DELETE FROM pagamento WHERE ID_reserva = :OLD.ID_reserva;
    DELETE FROM reserva_alojamento WHERE ID_reserva = :OLD.ID_reserva;
END;
/

-- ------------------------------------------------------------
-- 2. TRIGGERS DE VALIDAÇÃO
-- ------------------------------------------------------------

-- NOTA: o original estava na tabela Alojamento, que não tem cap_alojamento.
-- A capacidade pertence a Tipo_Alojamento, por isso o trigger passa para lá.
CREATE OR REPLACE TRIGGER validarcapalojamento
BEFORE INSERT OR UPDATE ON tipo_alojamento
FOR EACH ROW
BEGIN
    IF :NEW.cap_alojamento <= 0 THEN
        RAISE_APPLICATION_ERROR(-20003, 'ERRO: Capacidade de alojamento invalida!');
    END IF;
END;
/

-- NOTA: "IS NOT BETWEEN" não é PL/SQL válido. O limite passa de 5.9 para 5.0,
-- igual à constraint CHECK da tabela. A avaliação pode ser nula (as procedures
-- associar_* inserem NULL).
CREATE OR REPLACE TRIGGER validaravaoperador
BEFORE INSERT OR UPDATE ON operador_turista
FOR EACH ROW
BEGIN
    IF :NEW.avaliacao_operador IS NOT NULL
       AND :NEW.avaliacao_operador NOT BETWEEN 0.0 AND 5.0 THEN
        RAISE_APPLICATION_ERROR(-20004, 'ERRO: Avaliacao do operador invalida!');
    END IF;
END;
/

-- NOTA: o original usava :NEW.avaliacao_operador (coluna de outra tabela).
CREATE OR REPLACE TRIGGER validaravalojamento
BEFORE INSERT OR UPDATE ON reserva_alojamento
FOR EACH ROW
BEGIN
    IF :NEW.avaliacao_alojamento IS NOT NULL
       AND :NEW.avaliacao_alojamento NOT BETWEEN 0.0 AND 5.0 THEN
        RAISE_APPLICATION_ERROR(-20005, 'ERRO: Avaliacao do alojamento invalida!');
    END IF;
END;
/

-- NOTA: REGEXP não existe; a função do Oracle 10g é REGEXP_LIKE.
-- A expressão original tinha um parêntese reto em falta ("[0-5[0-9]").
CREATE OR REPLACE TRIGGER validarhorabertura
BEFORE INSERT OR UPDATE ON atracao
FOR EACH ROW
BEGIN
    IF NOT REGEXP_LIKE(:NEW.hora_abertura, '^([01][0-9]|2[0-3]):[0-5][0-9]$') THEN
        RAISE_APPLICATION_ERROR(-20006, 'ERRO: Hora invalida!');
    END IF;
END;
/

CREATE OR REPLACE TRIGGER validarhorafecho
BEFORE INSERT OR UPDATE ON atracao
FOR EACH ROW
BEGIN
    IF NOT REGEXP_LIKE(:NEW.hora_fecho, '^([01][0-9]|2[0-3]):[0-5][0-9]$') THEN
        RAISE_APPLICATION_ERROR(-20007, 'ERRO: Hora invalida!');
    END IF;
    IF :NEW.hora_fecho <= :NEW.hora_abertura THEN
        RAISE_APPLICATION_ERROR(-20007, 'ERRO: Hora de fecho invalida!');
    END IF;
END;
/

-- NOTA: o horário especial é opcional; só se valida quando está preenchido.
CREATE OR REPLACE TRIGGER validarhoraberturaesp
BEFORE INSERT OR UPDATE ON atracao
FOR EACH ROW
BEGIN
    IF :NEW.hora_abertura_especial IS NOT NULL
       AND NOT REGEXP_LIKE(:NEW.hora_abertura_especial, '^([01][0-9]|2[0-3]):[0-5][0-9]$') THEN
        RAISE_APPLICATION_ERROR(-20006, 'ERRO: Hora invalida!');
    END IF;
END;
/

CREATE OR REPLACE TRIGGER validarhorafechoesp
BEFORE INSERT OR UPDATE ON atracao
FOR EACH ROW
BEGIN
    IF :NEW.hora_fecho_especial IS NOT NULL THEN
        IF NOT REGEXP_LIKE(:NEW.hora_fecho_especial, '^([01][0-9]|2[0-3]):[0-5][0-9]$') THEN
            RAISE_APPLICATION_ERROR(-20007, 'ERRO: Hora invalida!');
        END IF;
        IF :NEW.hora_fecho_especial <= :NEW.hora_abertura_especial THEN
            RAISE_APPLICATION_ERROR(-20007, 'ERRO: Hora de fecho especial invalida!');
        END IF;
    END IF;
END;
/

-- NOTA: faltava o END IF. Além disso, a regra só se aplica ao inserir ou quando
-- a data de chegada muda; caso contrário seria impossível atualizar o estado
-- (ex.: para CONCLUIDA) de uma reserva cuja chegada já passou.
CREATE OR REPLACE TRIGGER validardatachegada
BEFORE INSERT OR UPDATE ON reserva
FOR EACH ROW
BEGIN
    IF (INSERTING OR :NEW.data_chegada <> :OLD.data_chegada)
       AND TRUNC(:NEW.data_chegada) < TRUNC(SYSDATE) THEN
        RAISE_APPLICATION_ERROR(-20008, 'ERRO: Data de chegada invalida!');
    END IF;
END;
/

-- NOTA: faltava o END IF.
CREATE OR REPLACE TRIGGER validardatapartida
BEFORE INSERT OR UPDATE ON reserva
FOR EACH ROW
BEGIN
    IF :NEW.data_partida <= :NEW.data_chegada THEN
        RAISE_APPLICATION_ERROR(-20009, 'ERRO: Data de partida invalida!');
    END IF;
END;
/

-- NOTA: a segunda condição do original referia colunas inexistentes
-- (:NEW.data_chegada, reserva.data_entrada, reserva.ID) e não era possível
-- deduzir a regra pretendida, por isso foi retirada. Mantêm-se as regras
-- "não pode ser no futuro" e "não pode ter mais de 12 meses", aplicadas ao
-- inserir ou quando a data muda (para permitir concluir pagamentos antigos).
CREATE OR REPLACE TRIGGER validarpagamento
BEFORE INSERT OR UPDATE ON pagamento
FOR EACH ROW
BEGIN
    IF INSERTING OR :NEW.data_pagamento <> :OLD.data_pagamento THEN
        IF TRUNC(:NEW.data_pagamento) > TRUNC(SYSDATE) THEN
            RAISE_APPLICATION_ERROR(-20010, 'ERRO: Data de pagamento invalida!');
        END IF;
        IF MONTHS_BETWEEN(SYSDATE, :NEW.data_pagamento) > 12 THEN
            RAISE_APPLICATION_ERROR(-20010, 'ERRO: Data de pagamento invalida!');
        END IF;
    END IF;
END;
/

-- ------------------------------------------------------------
-- 3. PROCEDURES INVÁLIDAS
-- ------------------------------------------------------------

-- NOTA: faltava declarar a variável do ID. O formato 'RES' || 0000 dava 7
-- caracteres (a coluna é VARCHAR2(6)); usa-se 'RS-' || 000 como em Inserts.sql.
CREATE OR REPLACE PROCEDURE registrar_reserva (
    novo_id_turista IN VARCHAR2,
    novo_data_chegada IN DATE,
    novo_data_partida IN DATE,
    novo_status IN VARCHAR2
) AS
    novo_id_reserva Reserva.ID_reserva%TYPE;
BEGIN
    novo_id_reserva := 'RS-' || LPAD(ID_reserva.NEXTVAL, 3, '0');
    INSERT INTO reserva (ID_reserva, ID_turista, data_chegada, data_partida, status_reserva)
    VALUES (novo_id_reserva, novo_id_turista, novo_data_chegada, novo_data_partida, novo_status);

    COMMIT;
    DBMS_OUTPUT.PUT_LINE('Reserva ' || novo_id_reserva || ' gerada com sucesso!');
EXCEPTION
    WHEN OTHERS THEN
        ROLLBACK;
        DBMS_OUTPUT.PUT_LINE('Erro ao registrar reserva: ' || SQLERRM);
END;
/

-- NOTA: faltava declarar a variável do ID.
CREATE OR REPLACE PROCEDURE registrar_pagamento (
    novo_id_reserva IN VARCHAR2,
    novo_valor IN NUMBER,
    novo_data_pagamento DATE,
    novo_metodo_pagamento IN VARCHAR2,
    novo_status_pagamento IN VARCHAR2,
    novo_id_recibo IN VARCHAR2
) AS
    novo_id_pagamento Pagamento.ID_pagamento%TYPE;
BEGIN
    novo_id_pagamento := 'PG-' || LPAD(ID_pagamento.NEXTVAL, 3, '0');
    INSERT INTO pagamento (ID_pagamento, ID_reserva, valor, data_pagamento, metodo_pagamento, status_pagamento, ID_recibo)
    VALUES (novo_id_pagamento, novo_id_reserva, novo_valor, novo_data_pagamento, novo_metodo_pagamento, novo_status_pagamento, novo_id_recibo);

    COMMIT;
    DBMS_OUTPUT.PUT_LINE('Pagamento ' || novo_id_pagamento || ' gerado com sucesso!');
EXCEPTION
    WHEN OTHERS THEN
        ROLLBACK;
        DBMS_OUTPUT.PUT_LINE('Erro ao registrar pagamento: ' || SQLERRM);
END;
/

-- NOTA: faltava declarar a variável do ID e as horas eram DATE, mas as colunas
-- são CHAR(5) no formato HH24:MI; os parâmetros passam a VARCHAR2.
CREATE OR REPLACE PROCEDURE registrar_atracao (
    novo_id_destino IN VARCHAR2,
    novo_tipo_atracao IN VARCHAR2,
    novo_descricao IN VARCHAR2,
    novo_hora_abertura IN VARCHAR2,
    novo_hora_fecho IN VARCHAR2
) AS
    novo_id_atracao Atracao.ID_atracao%TYPE;
BEGIN
    novo_id_atracao := 'AT-' || LPAD(ID_atracao.NEXTVAL, 3, '0');
    INSERT INTO Atracao (ID_atracao, ID_destino, tipo_atracao, descricao, hora_abertura, hora_fecho)
    VALUES (novo_id_atracao, novo_id_destino, novo_tipo_atracao, novo_descricao, novo_hora_abertura, novo_hora_fecho);
    COMMIT;
    DBMS_OUTPUT.PUT_LINE('Atracao ' || novo_id_atracao || ' registrada com sucesso!');
EXCEPTION
    WHEN OTHERS THEN
        ROLLBACK;
        DBMS_OUTPUT.PUT_LINE('Erro ao registrar atracao: ' || SQLERRM);
END;
/

-- NOTA: o original inseria avaliacao_operador em Turista_Destino (coluna que
-- não existe) e não indicava data_visita, que é obrigatória e faz parte da
-- chave primária. O terceiro parâmetro passa a ser a data da visita.
CREATE OR REPLACE PROCEDURE registrar_visita (
    novo_id_destino IN VARCHAR2,
    novo_id_turista IN VARCHAR2,
    novo_data_visita IN DATE
) AS
BEGIN
    INSERT INTO turista_destino (ID_destino, ID_turista, data_visita)
    VALUES (novo_id_destino, novo_id_turista, TRUNC(novo_data_visita));

    COMMIT;
    DBMS_OUTPUT.PUT_LINE('Turista  ' || novo_id_turista || ' realiza visita ao destino ' || novo_id_destino || ' com sucesso!');
EXCEPTION
    WHEN OTHERS THEN
        ROLLBACK;
        DBMS_OUTPUT.PUT_LINE('Erro ao registrar visita: ' || SQLERRM);
END;
/

-- NOTA: as colunas chamam-se hora_abertura_especial / hora_fecho_especial
-- (sem o prefixo "Horario_"). A constraint CHECK da tabela exige que
-- detalhes_horario_especial seja preenchido junto com as horas especiais,
-- por isso foi acrescentado esse parâmetro.
CREATE OR REPLACE PROCEDURE alterar_horario_atracao (
    novo_id_atracao IN Atracao.ID_atracao%TYPE,
    novo_hora_abertura_especial IN Atracao.hora_abertura_especial%TYPE,
    novo_hora_fecho_especial IN Atracao.hora_fecho_especial%TYPE,
    novo_detalhes_horario_especial IN Atracao.detalhes_horario_especial%TYPE
) AS
BEGIN
    UPDATE Atracao
    SET hora_abertura_especial = novo_hora_abertura_especial,
        hora_fecho_especial = novo_hora_fecho_especial,
        detalhes_horario_especial = novo_detalhes_horario_especial
    WHERE ID_atracao = novo_id_atracao;

    IF SQL%FOUND THEN
        COMMIT;
        DBMS_OUTPUT.PUT_LINE('Horario de funcionamento da atracao ' || novo_id_atracao || ' atualizado com sucesso!');
    ELSE
        DBMS_OUTPUT.PUT_LINE('Atracao ' || novo_id_atracao || ' nao foi encontrada.');
    END IF;

EXCEPTION
    WHEN OTHERS THEN
        ROLLBACK;
        DBMS_OUTPUT.PUT_LINE('Erro ao atualizar horario de funcionamento: ' || SQLERRM);
END;
/

-- ------------------------------------------------------------
-- 4. VIEWS (cria as 24 views definidas em views.sql)
-- ------------------------------------------------------------
@@views.sql

-- ------------------------------------------------------------
-- 5. VERIFICAÇÃO: não deve devolver nenhuma linha
-- ------------------------------------------------------------
SELECT object_type, object_name, status
FROM user_objects
WHERE status <> 'VALID'
ORDER BY object_type, object_name;

SELECT COUNT(*) AS total_views FROM user_views;
