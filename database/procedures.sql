--A. Registrar uma novo reserva realizada por um turista.
CREATE OR REPLACE PROCEDURE registrar_reserva (
    novo_id_turista IN VARCHAR2, 
    novo_data_chegada IN DATE,
    novo_data_partida IN DATE,
    novo_status IN VARCHAR2 
) AS
BEGIN
    novo_id_reserva := 'RES' || TO_CHAR(ID_reserva.NEXTVAL, 'FM0000');
    INSERT INTO reserva (ID_reserva, ID_turista, data_chegada, data_partida, status_reserva)
    VALUES (novo_id_reserva, novo_id_turista, novo_data_chegada, novo_data_partida, novo_status_reserva);
    
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
CREATE OR REPLACE PROCEDURE registrar_pagamento (
    novo_id_reserva IN VARCHAR2, 
    novo_valor IN NUMBER, 
    novo_data_pagamento DATE, 
    novo_metodo_pagamento IN VARCHAR2, 
    novo_status_pagamento IN VARCHAR2, 
    novo_id_recibo IN VARCHAR2
) AS
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

--D. Registar a realização de uma visita de um turista a um destino (Turista_destino).
CREATE OR REPLACE PROCEDURE registrar_visita (
    novo_id_destino IN VARCHAR2,
    novo_id_turista IN VARCHAR2,
    avaliacao_operador IN NUMBER
) AS
BEGIN
    INSERT INTO turista_destino (ID_destino, ID_turista, avaliacao_operador)
    VALUES (novo_id_destino, novo_id_turista, avaliacao_operador);
    
    COMMIT;
    DBMS_OUTPUT.PUT_LINE('Turista  ' || novo_id_turista || ' realiza visita ao destino ' || novo_id_destino || ' com sucesso!');
EXCEPTION
    WHEN OTHERS THEN
        ROLLBACK;
        DBMS_OUTPUT.PUT_LINE('Erro ao registrar visita: ' || SQLERRM);
END;
/

--E. Associar novos atrações turísticas a um destino específico.
CREATE OR REPLACE PROCEDURE registrar_atracao (
    novo_id_destino IN VARCHAR2,
    novo_tipo_atracao IN VARCHAR2,
    novo_descricao IN VARCHAR2,
    novo_hora_abertura IN DATE,
    novo_hora_fecho IN DATE
) AS
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
CREATE OR REPLACE PROCEDURE alterar_horario_atracao (
    novo_id_atracao IN Atracao.ID_atracao%TYPE,
    novo_hora_abertura_especial IN Atracao.Horario_hora_abertura_especial%TYPE,
    novo_hora_fecho_especial IN Atracao.Horario_hora_fecho_especial%TYPE
) AS
BEGIN
    UPDATE Atracao
    SET horario_hora_abertura_especial = novo_hora_abertura_especial,
        horario_hora_fecho_especial = novo_hora_fecho_especial
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