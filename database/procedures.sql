-- ============================================================
-- TRABALHO PRÁTICO - BASE DE DADOS I (ISCTEM 2026)
-- GRUPO 1: GESTÃO DE TURISMO EM MOÇAMBIQUE
-- SCRIPT DE PROCEDURES (Oracle 10g XE)
-- Executar depois de tabelas.sql e Sequences.sql
--
-- As correções feitas ao script original estão assinaladas com "NOTA".
-- NOTA geral: no Oracle 10g não se pode usar sequencia.NEXTVAL diretamente numa
-- expressão PL/SQL (só a partir do 11g, PLS-00357); o número lê-se com
-- SELECT ... INTO ... FROM dual, como nas funções de functions.sql.
-- ============================================================

SET SERVEROUTPUT ON

--A. Registrar uma novo reserva realizada por um turista.
-- NOTA: faltava declarar a variável do ID e o parâmetro chama-se novo_status.
-- O formato 'RES' || 0000 dava 7 caracteres (a coluna é VARCHAR2(6)); usa-se
-- 'RS-' || 000 como em Inserts.sql.
CREATE OR REPLACE PROCEDURE registrar_reserva (
    novo_id_turista IN VARCHAR2, 
    novo_data_chegada IN DATE,
    novo_data_partida IN DATE,
    novo_status IN VARCHAR2 
) AS
    v_num NUMBER;
    novo_id_reserva Reserva.ID_reserva%TYPE;
BEGIN
    SELECT ID_reserva.NEXTVAL INTO v_num FROM dual;
    novo_id_reserva := 'RS-' || LPAD(v_num, 3, '0');
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

--B. Associar um ou mais alojamentos a uma reserva através da entidade Reserva_alojamento.
CREATE OR REPLACE PROCEDURE associar_alojamento_reserva (
    novo_id_reserva IN VARCHAR2,
    novo_id_alojamento IN VARCHAR2
) AS
BEGIN
    INSERT INTO reserva_alojamento (ID_reserva, ID_alojamento, avaliacao_alojamento)
    VALUES (novo_id_reserva, novo_id_alojamento, NULL);
    
    COMMIT;
    DBMS_OUTPUT.PUT_LINE('Alojamento  ' || novo_id_alojamento || ' associada a reserva ' || novo_id_reserva || ' com sucesso!');
EXCEPTION
    WHEN OTHERS THEN
        ROLLBACK;
        DBMS_OUTPUT.PUT_LINE('Erro ao associar alojamento: ' || SQLERRM);
END;
/

--C. Gerar e registrar um ou mais pagamentos vinculados a uma reserva.
-- NOTA: faltava declarar a variável do ID.
CREATE OR REPLACE PROCEDURE registrar_pagamento (
    novo_id_reserva IN VARCHAR2, 
    novo_valor IN NUMBER, 
    novo_data_pagamento DATE, 
    novo_metodo_pagamento IN VARCHAR2, 
    novo_status_pagamento IN VARCHAR2, 
    novo_id_recibo IN VARCHAR2
) AS
    v_num NUMBER;
    novo_id_pagamento Pagamento.ID_pagamento%TYPE;
BEGIN
    SELECT ID_pagamento.NEXTVAL INTO v_num FROM dual;
    novo_id_pagamento := 'PG-' || LPAD(v_num, 3, '0');
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

--D. Registar a realização de uma visita de um turista a um destino (Turista_destino).
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

--E. Associar novos atrações turísticas a um destino específico.
-- NOTA: faltava declarar a variável do ID e as horas eram DATE, mas as colunas
-- são CHAR(5) no formato HH24:MI; os parâmetros passam a VARCHAR2.
CREATE OR REPLACE PROCEDURE registrar_atracao (
    novo_id_destino IN VARCHAR2,
    novo_tipo_atracao IN VARCHAR2,
    novo_descricao IN VARCHAR2,
    novo_hora_abertura IN VARCHAR2,
    novo_hora_fecho IN VARCHAR2
) AS
    v_num NUMBER;
    novo_id_atracao Atracao.ID_atracao%TYPE;
BEGIN
    SELECT ID_atracao.NEXTVAL INTO v_num FROM dual;
    novo_id_atracao := 'AT-' || LPAD(v_num, 3, '0');
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

--F. Definir o Tipo_alojamento correspondente a cada alojamento de um destino.
CREATE OR REPLACE PROCEDURE definir_tipo_alojamento (
    novo_tipo_alojamento IN VARCHAR2,
    novo_cap_alojamento IN NUMBER
) AS
BEGIN
    INSERT INTO Tipo_Alojamento (tipo_alojamento, cap_alojamento)
    VALUES (novo_tipo_alojamento, novo_cap_alojamento);
    COMMIT;
    DBMS_OUTPUT.PUT_LINE('Tipo de alojamento ' || novo_tipo_alojamento || ' definido com sucesso!');
EXCEPTION
    WHEN OTHERS THEN
        ROLLBACK;
        DBMS_OUTPUT.PUT_LINE('Erro ao definir tipo de alojamento: ' || SQLERRM);
END;
/

--G. Inserir as preferências de viagem de um turista (Turista_preferencia).
CREATE OR REPLACE PROCEDURE inserir_preferencia (
    novo_id_turista IN VARCHAR2,
    novo_preferencia IN VARCHAR2
) AS
BEGIN
    INSERT INTO Turista_Preferencia (ID_turista, preferencia)
    VALUES (novo_id_turista, novo_preferencia);
    COMMIT;
    DBMS_OUTPUT.PUT_LINE('Preferencia do turista ' || novo_id_turista || ' registrada com sucesso!');
EXCEPTION
    WHEN OTHERS THEN
        ROLLBACK;
        DBMS_OUTPUT.PUT_LINE('Erro ao inserir preferencia: ' || SQLERRM);
END;
/

--H. Actualizar as preferências de viagem de um turista (Turista_preferencia).
CREATE OR REPLACE PROCEDURE atualizar_pref_turista (
    novo_id_turista       IN Turista_Preferencia.ID_turista%TYPE,
    novo_preferencia      IN Turista_Preferencia.preferencia%TYPE
) AS
BEGIN
    -- Executa a atualização dos dados na tabela
    UPDATE Turista_Preferencia
    SET preferencia = novo_preferencia
    WHERE ID_turista = novo_id_turista;

    -- Confirma a alteração se um registo for afetado
    IF SQL%FOUND THEN
        COMMIT;
        DBMS_OUTPUT.PUT_LINE('Preferencias do turista ' || novo_id_turista || ' atualizadas com sucesso!');
    ELSE
        DBMS_OUTPUT.PUT_LINE('Turista com o ID ' || novo_id_turista || ' nao foi encontrado.');
    END IF;

EXCEPTION
    WHEN OTHERS THEN
        ROLLBACK;
        DBMS_OUTPUT.PUT_LINE('Erro ao atualizar preferencias: ' || SQLERRM);
END;
/

--I. Associar um operador ao acompanhamento de clientes (Operador_Turista).
CREATE OR REPLACE PROCEDURE associar_operador_turista (
    novo_id_operador IN VARCHAR2,
    novo_id_turista IN VARCHAR2
) AS
BEGIN
    INSERT INTO Operador_Turista (ID_operador, ID_turista, avaliacao_operador)
    VALUES (novo_id_operador, novo_id_turista, NULL);
    
    COMMIT;
    DBMS_OUTPUT.PUT_LINE('Operador  ' || novo_id_operador || ' associado ao turista ' || novo_id_turista || ' com sucesso!');
EXCEPTION
    WHEN OTHERS THEN
        ROLLBACK;
        DBMS_OUTPUT.PUT_LINE('Erro ao associar operador: ' || SQLERRM);
END;
/

--J. Associar um operador à gestão de transporte.
CREATE OR REPLACE PROCEDURE associar_operador_transporte (
    novo_id_operador IN VARCHAR2,
    novo_id_transporte IN VARCHAR2
) AS
BEGIN
    INSERT INTO Operador_Transporte (ID_operador, ID_transporte)
    VALUES (novo_id_operador, novo_id_transporte);
    
    COMMIT;
    DBMS_OUTPUT.PUT_LINE('Operador  ' || novo_id_operador || ' associado ao transporte ' || novo_id_transporte || ' com sucesso!');
EXCEPTION
    WHEN OTHERS THEN
        ROLLBACK;
        DBMS_OUTPUT.PUT_LINE('Erro ao associar operador: ' || SQLERRM);
END;
/

--K. Associar um operador ao fornecimento de alojamento.
CREATE OR REPLACE PROCEDURE associar_operador_alojamento (
    novo_id_operador IN VARCHAR2,
    novo_id_alojamento IN VARCHAR2
) AS
BEGIN
    INSERT INTO Operador_Alojamento (ID_operador, ID_alojamento)
    VALUES (novo_id_operador, novo_id_alojamento);
    
    COMMIT;
    DBMS_OUTPUT.PUT_LINE('Alojamento  ' || novo_id_alojamento || ' associada ao operador ' || novo_id_operador || ' com sucesso!');
EXCEPTION
    WHEN OTHERS THEN
        ROLLBACK;
        DBMS_OUTPUT.PUT_LINE('Erro ao associar alojamento: ' || SQLERRM);
END;
/

--L. Cancelar uma reserva registrada.
CREATE OR REPLACE PROCEDURE cancelar_reserva (
    novo_id_reserva IN Reserva.ID_reserva%TYPE
) AS
BEGIN
    UPDATE Reserva
    SET status_reserva = 'CANCELADA'
    WHERE ID_reserva = novo_id_reserva;

    -- Confirma a alteração se um registo for afetado
    IF SQL%FOUND THEN
        COMMIT;
        DBMS_OUTPUT.PUT_LINE('Reserva ' || novo_id_reserva || ' cancelada com sucesso!');
    ELSE
        DBMS_OUTPUT.PUT_LINE('Reserva com o ID ' || novo_id_reserva || ' nao foi encontrada.');
    END IF;

EXCEPTION
    WHEN OTHERS THEN
        ROLLBACK;
        DBMS_OUTPUT.PUT_LINE('Erro ao cancelar reserva: ' || SQLERRM);
END;
/

--M. Alterar o horário de funcionamento de uma atração devido a um feriado ou evento.
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

--N. Alterar as datas de chegada e partida de uma reserva já existente a pedido do cliente.
CREATE OR REPLACE PROCEDURE alterar_datas_reserva (
    novo_id_reserva IN Reserva.ID_reserva%TYPE,
    nova_data_chegada IN Reserva.Data_chegada%TYPE,
    nova_data_partida IN Reserva.Data_partida%TYPE
) AS
BEGIN
    UPDATE Reserva
    SET data_chegada = nova_data_chegada,
        data_partida = nova_data_partida
    WHERE ID_reserva = novo_id_reserva;

    IF SQL%FOUND THEN
        COMMIT;
        DBMS_OUTPUT.PUT_LINE('Datas da reserva ' || novo_id_reserva || ' atualizadas com sucesso!');
    ELSE
        DBMS_OUTPUT.PUT_LINE('Reserva ' || novo_id_reserva || ' nao foi encontrada.');
    END IF;

EXCEPTION
    WHEN OTHERS THEN
        ROLLBACK;
        DBMS_OUTPUT.PUT_LINE('Erro ao atualizar datas da reserva: ' || SQLERRM);
END;
/

--P. Atualizar a classificação (estrelas) de um alojamento após uma avaliação.
CREATE OR REPLACE PROCEDURE avaliar_alojamento (
    novo_id_reserva IN VARCHAR2,
    novo_id_alojamento IN VARCHAR2,
    novo_avaliacao_alojamento IN NUMBER
) AS
BEGIN
    UPDATE Reserva_Alojamento
    SET avaliacao_alojamento = novo_avaliacao_alojamento
    WHERE ID_reserva = novo_id_reserva AND ID_alojamento = novo_id_alojamento;
    
    IF SQL%NOTFOUND THEN
        INSERT INTO Reserva_Alojamento (ID_reserva, ID_alojamento, avaliacao_alojamento)
        VALUES (novo_id_reserva, novo_id_alojamento, novo_avaliacao_alojamento);
    END IF;
    
    COMMIT;
    DBMS_OUTPUT.PUT_LINE('Alojamento  ' || novo_id_alojamento || ' na reserva ' || novo_id_reserva || ' foi avaliado com sucesso!');
EXCEPTION
    WHEN OTHERS THEN
        ROLLBACK;
        DBMS_OUTPUT.PUT_LINE('Erro ao avaliar alojamento: ' || SQLERRM);
END;
/

--Q. Registar a nota/avaliação que um turista deu para um operador após o término da viagem.
CREATE OR REPLACE PROCEDURE avaliar_operador (
    novo_id_operador IN VARCHAR2,
    novo_id_turista IN VARCHAR2,
    novo_avaliacao_operador IN NUMBER
) AS
BEGIN
    UPDATE Operador_Turista
    SET avaliacao_operador = novo_avaliacao_operador
    WHERE ID_operador = novo_id_operador AND ID_turista = novo_id_turista;  

    IF SQL%NOTFOUND THEN
        INSERT INTO Operador_Turista (ID_operador, ID_turista, avaliacao_operador)
        VALUES (novo_id_operador, novo_id_turista, novo_avaliacao_operador);
    END IF;
    
    COMMIT;
    DBMS_OUTPUT.PUT_LINE('Operador  ' || novo_id_operador || ' foi avaliado pelo turista  ' || novo_id_turista || ' com sucesso!');
EXCEPTION
    WHEN OTHERS THEN
        ROLLBACK;
        DBMS_OUTPUT.PUT_LINE('Erro ao avaliar operador: ' || SQLERRM);
END;
/

-- Verificação: não deve aparecer nenhuma procedure INVALID
SELECT object_name, status FROM user_objects WHERE object_type = 'PROCEDURE' ORDER BY object_name;
