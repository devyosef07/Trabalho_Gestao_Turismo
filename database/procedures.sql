-- ============================================================
-- TRABALHO PRÁTICO - BASE DE DADOS I (ISCTEM 2026)
-- GRUPO 1: GESTÃO DE TURISMO EM MOÇAMBIQUE
-- SCRIPT DE PROCEDURES (Oracle 10g XE)
-- Executar depois de tabelas.sql e Sequences.sql
--
-- Este é o ÚNICO script de procedures do projeto (43 procedures), dividido em
-- duas partes:
--   PARTE 1 - as 16 procedures do enunciado (A a Q);
--   PARTE 2 - 27 procedures de manutenção (criar, alterar e eliminar) para as
--             operações que a Parte 1 não cobre. Com elas, a aplicação Java
--             nunca escreve diretamente nas tabelas: lê das views (views.sql)
--             e escreve só através destas procedures e das funções
--             (functions.sql).
--
-- Todas seguem o mesmo estilo:
--   * COMMIT no fim e mensagem de sucesso com DBMS_OUTPUT;
--   * em caso de erro, ROLLBACK e mensagem "Erro ao ...: " || SQLERRM;
--   * quando o registo não existe, mensagem "... nao foi encontrado(a).";
--   * os códigos novos usam a sequência com o formato de Inserts.sql
--     (ex.: 'DT-' || 000).
-- Os triggers delete* de Triggers.sql apagam os registos dependentes quando
-- se elimina um destino, alojamento, transporte, operador, reserva, etc.
--
-- As correções feitas ao script original estão assinaladas com "NOTA".
-- NOTA geral: no Oracle 10g não se pode usar sequencia.NEXTVAL diretamente numa
-- expressão PL/SQL (só a partir do 11g, PLS-00357); o número lê-se com
-- SELECT ... INTO ... FROM dual, como nas funções de functions.sql.
--
-- ------------------------------------------------------------
-- ÍNDICE: para que serve cada procedure
-- ------------------------------------------------------------
-- PARTE 1 - Procedures do enunciado
--   A. registrar_reserva            Cria uma reserva de um turista (gera RS-xxx).
--   B. associar_alojamento_reserva  Liga um alojamento a uma reserva.
--   C. registrar_pagamento          Cria um pagamento de uma reserva (gera PG-xxx);
--                                   o recibo é gerado depois por Concluir_Pagamento.
--   D. registrar_visita             Regista a visita de um turista a um destino
--                                   (recusa se o destino estiver lotado nesse dia).
--   E. registrar_atracao            Cria uma atração num destino (gera AT-xxx).
--   F. definir_tipo_alojamento      Cria um tipo de alojamento e a sua capacidade.
--   G. inserir_preferencia          Acrescenta uma preferência de viagem a um turista.
--   H. atualizar_pref_turista       Troca uma preferência do turista por outra.
--   I. associar_operador_turista    Atribui um operador ao acompanhamento de um turista.
--   J. associar_operador_transporte Atribui um operador à gestão de um transporte.
--   K. associar_operador_alojamento Atribui um operador a um alojamento.
--   L. cancelar_reserva             Passa o estado da reserva a CANCELADA.
--   M. alterar_horario_atracao      Define (ou limpa) o horário especial de uma
--                                   atração (feriado, evento).
--   N. alterar_datas_reserva        Muda as datas de chegada e partida de uma reserva.
--   P. avaliar_alojamento           Regista a avaliação (0 a 5) de um alojamento
--                                   numa reserva.
--   Q. avaliar_operador             Regista a avaliação (0 a 5) que um turista deu
--                                   a um operador.
--
-- PARTE 2 - Procedures de manutenção
--   Destino
--    1. registrar_destino           Cria um destino (gera DT-xxx).
--    2. atualizar_destino           Altera nome, região e capacidade de um destino.
--    3. eliminar_destino            Apaga um destino (e, pelo trigger, as suas
--                                   atrações, alojamentos e visitas).
--   Alojamento
--    4. registrar_alojamento        Cria um alojamento num destino (gera AL-xxx).
--    5. atualizar_alojamento        Altera os dados de um alojamento.
--    6. eliminar_alojamento         Apaga um alojamento e as suas associações.
--   Transporte
--    7. registrar_transporte        Cria um transporte (gera TP-xxx).
--    8. atualizar_transporte        Altera tipo, capacidade e disponibilidade.
--    9. eliminar_transporte         Apaga um transporte e as suas associações.
--   Operador
--   10. registrar_operador          Cria um operador turístico (gera OP-xxx).
--   11. atualizar_operador          Altera os dados de um operador.
--   12. eliminar_operador           Apaga um operador e as suas associações.
--   Atração
--   13. atualizar_atracao           Altera todos os dados de uma atração, incluindo
--                                   o horário especial.
--   14. eliminar_atracao            Apaga uma atração.
--   Reserva
--   15. alterar_turista_reserva     Muda o turista titular de uma reserva.
--   16. eliminar_reserva            Apaga uma reserva (e os seus pagamentos e
--                                   alojamentos associados).
--   Pagamento
--   17. atualizar_pagamento         Altera um pagamento que ainda não está CONCLUIDO.
--   18. eliminar_pagamento          Apaga um pagamento.
--   Tipo de alojamento
--   19. atualizar_tipo_alojamento   Altera a capacidade de um tipo de alojamento.
--   20. eliminar_tipo_alojamento    Apaga um tipo (falha se houver alojamentos dele).
--   Turista (criar e eliminar são as funções Insere_Turista e Elimina_Turista)
--   21. atualizar_turista           Altera o nome e a nacionalidade de um turista.
--   Associações
--   22. remover_preferencia         Retira uma preferência de um turista.
--   23. remover_visita              Apaga o registo de uma visita a um destino.
--   24. remover_alojamento_reserva  Retira um alojamento de uma reserva.
--   25. remover_operador_turista    Desfaz a ligação operador - turista.
--   26. remover_operador_transporte Desfaz a ligação operador - transporte.
--   27. remover_operador_alojamento Desfaz a ligação operador - alojamento.
-- ============================================================

SET SERVEROUTPUT ON

-- ============================================================
-- PARTE 1 - PROCEDURES DO ENUNCIADO (A a Q)
-- ============================================================

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
-- NOTA: faltava declarar a variável do ID. Foi acrescentado o parâmetro
-- opcional novo_detalhes_transacao (a coluna existe e a aplicação usa-a); as
-- chamadas antigas, com 6 parâmetros, continuam a funcionar.
CREATE OR REPLACE PROCEDURE registrar_pagamento (
    novo_id_reserva IN VARCHAR2,
    novo_valor IN NUMBER,
    novo_data_pagamento DATE,
    novo_metodo_pagamento IN VARCHAR2,
    novo_status_pagamento IN VARCHAR2,
    novo_id_recibo IN VARCHAR2,
    novo_detalhes_transacao IN VARCHAR2 DEFAULT NULL
) AS
    v_num NUMBER;
    novo_id_pagamento Pagamento.ID_pagamento%TYPE;
BEGIN
    SELECT ID_pagamento.NEXTVAL INTO v_num FROM dual;
    novo_id_pagamento := 'PG-' || LPAD(v_num, 3, '0');
    INSERT INTO pagamento (ID_pagamento, ID_reserva, valor, data_pagamento, metodo_pagamento, status_pagamento, ID_recibo, detalhes_transacao)
    VALUES (novo_id_pagamento, novo_id_reserva, novo_valor, novo_data_pagamento, novo_metodo_pagamento, novo_status_pagamento, novo_id_recibo, novo_detalhes_transacao);
    
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
-- NOTA: o registo é feito pela função Regista_Visita (functions.sql), que
-- recusa a visita quando o destino já está lotado nessa data. Como
-- functions.sql corre depois deste script, a procedure é recompilada no fim
-- de functions.sql.
CREATE OR REPLACE PROCEDURE registrar_visita (
    novo_id_destino IN VARCHAR2,
    novo_id_turista IN VARCHAR2,
    novo_data_visita IN DATE
) AS
    v_resultado VARCHAR2(4000);
BEGIN
    v_resultado := Regista_Visita(novo_id_destino, novo_id_turista, novo_data_visita);
    IF v_resultado = 'Visita registada' THEN
        DBMS_OUTPUT.PUT_LINE('Turista  ' || novo_id_turista || ' realiza visita ao destino ' || novo_id_destino || ' com sucesso!');
    ELSE
        DBMS_OUTPUT.PUT_LINE('Erro ao registrar visita: ' || v_resultado);
    END IF;
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
-- NOTA: o WHERE era só por ID_turista, o que alterava TODAS as preferências do
-- turista e violava a chave primária (ID_turista, preferencia) quando ele tinha
-- mais do que uma. Foi acrescentado o parâmetro antiga_preferencia, para
-- trocar apenas essa.
CREATE OR REPLACE PROCEDURE atualizar_pref_turista (
    novo_id_turista       IN Turista_Preferencia.ID_turista%TYPE,
    antiga_preferencia    IN Turista_Preferencia.preferencia%TYPE,
    novo_preferencia      IN Turista_Preferencia.preferencia%TYPE
) AS
BEGIN
    -- Executa a atualização dos dados na tabela
    UPDATE Turista_Preferencia
    SET preferencia = novo_preferencia
    WHERE ID_turista = novo_id_turista
      AND preferencia = antiga_preferencia;

    -- Confirma a alteração se um registo for afetado
    IF SQL%FOUND THEN
        COMMIT;
        DBMS_OUTPUT.PUT_LINE('Preferencias do turista ' || novo_id_turista || ' atualizadas com sucesso!');
    ELSE
        DBMS_OUTPUT.PUT_LINE('Preferencia ' || antiga_preferencia || ' do turista ' || novo_id_turista || ' nao foi encontrada.');
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

-- ============================================================
-- PARTE 2 - PROCEDURES DE MANUTENÇÃO (1 a 27)
-- Criar, alterar e eliminar o que a Parte 1 não cobre, para que a aplicação
-- nunca escreva diretamente nas tabelas.
-- ============================================================

-- ============================================================
-- DESTINO
-- ============================================================

--1. Registar um novo destino turístico.
CREATE OR REPLACE PROCEDURE registrar_destino (
    novo_nome_destino IN Destino.nome_destino%TYPE,
    novo_regiao IN Destino.regiao%TYPE,
    novo_capacidade_visitantes IN Destino.capacidade_visitantes%TYPE
) AS
    v_num NUMBER;
    novo_id_destino Destino.ID_destino%TYPE;
BEGIN
    SELECT ID_destino.NEXTVAL INTO v_num FROM dual;
    novo_id_destino := 'DT-' || LPAD(v_num, 3, '0');
    INSERT INTO Destino (ID_destino, nome_destino, regiao, capacidade_visitantes)
    VALUES (novo_id_destino, novo_nome_destino, novo_regiao, novo_capacidade_visitantes);
    COMMIT;
    DBMS_OUTPUT.PUT_LINE('Destino ' || novo_id_destino || ' registado com sucesso!');
EXCEPTION
    WHEN OTHERS THEN
        ROLLBACK;
        DBMS_OUTPUT.PUT_LINE('Erro ao registar destino: ' || SQLERRM);
END;
/

--2. Atualizar os dados de um destino.
CREATE OR REPLACE PROCEDURE atualizar_destino (
    novo_id_destino IN Destino.ID_destino%TYPE,
    novo_nome_destino IN Destino.nome_destino%TYPE,
    novo_regiao IN Destino.regiao%TYPE,
    novo_capacidade_visitantes IN Destino.capacidade_visitantes%TYPE
) AS
BEGIN
    UPDATE Destino
    SET nome_destino = novo_nome_destino,
        regiao = novo_regiao,
        capacidade_visitantes = novo_capacidade_visitantes
    WHERE ID_destino = novo_id_destino;

    IF SQL%FOUND THEN
        COMMIT;
        DBMS_OUTPUT.PUT_LINE('Destino ' || novo_id_destino || ' atualizado com sucesso!');
    ELSE
        DBMS_OUTPUT.PUT_LINE('Destino ' || novo_id_destino || ' nao foi encontrado.');
    END IF;
EXCEPTION
    WHEN OTHERS THEN
        ROLLBACK;
        DBMS_OUTPUT.PUT_LINE('Erro ao atualizar destino: ' || SQLERRM);
END;
/

--3. Eliminar um destino (o trigger deleteDestino apaga atrações, alojamentos e visitas).
CREATE OR REPLACE PROCEDURE eliminar_destino (
    novo_id_destino IN Destino.ID_destino%TYPE
) AS
BEGIN
    DELETE FROM Destino WHERE ID_destino = novo_id_destino;

    IF SQL%FOUND THEN
        COMMIT;
        DBMS_OUTPUT.PUT_LINE('Destino ' || novo_id_destino || ' eliminado com sucesso!');
    ELSE
        DBMS_OUTPUT.PUT_LINE('Destino ' || novo_id_destino || ' nao foi encontrado.');
    END IF;
EXCEPTION
    WHEN OTHERS THEN
        ROLLBACK;
        DBMS_OUTPUT.PUT_LINE('Erro ao eliminar destino: ' || SQLERRM);
END;
/

-- ============================================================
-- ALOJAMENTO
-- ============================================================

--4. Registar um novo alojamento num destino.
CREATE OR REPLACE PROCEDURE registrar_alojamento (
    novo_nome_alojamento IN Alojamento.nome_alojamento%TYPE,
    novo_tipo_alojamento IN Alojamento.tipo_alojamento%TYPE,
    novo_class_alojamento IN Alojamento.class_alojamento%TYPE,
    novo_id_destino IN Alojamento.ID_destino%TYPE
) AS
    v_num NUMBER;
    novo_id_alojamento Alojamento.ID_alojamento%TYPE;
BEGIN
    SELECT ID_alojamento.NEXTVAL INTO v_num FROM dual;
    novo_id_alojamento := 'AL-' || LPAD(v_num, 3, '0');
    INSERT INTO Alojamento (ID_alojamento, nome_alojamento, tipo_alojamento, class_alojamento, ID_destino)
    VALUES (novo_id_alojamento, novo_nome_alojamento, novo_tipo_alojamento, novo_class_alojamento, novo_id_destino);
    COMMIT;
    DBMS_OUTPUT.PUT_LINE('Alojamento ' || novo_id_alojamento || ' registado com sucesso!');
EXCEPTION
    WHEN OTHERS THEN
        ROLLBACK;
        DBMS_OUTPUT.PUT_LINE('Erro ao registar alojamento: ' || SQLERRM);
END;
/

--5. Atualizar os dados de um alojamento.
CREATE OR REPLACE PROCEDURE atualizar_alojamento (
    novo_id_alojamento IN Alojamento.ID_alojamento%TYPE,
    novo_nome_alojamento IN Alojamento.nome_alojamento%TYPE,
    novo_tipo_alojamento IN Alojamento.tipo_alojamento%TYPE,
    novo_class_alojamento IN Alojamento.class_alojamento%TYPE,
    novo_id_destino IN Alojamento.ID_destino%TYPE
) AS
BEGIN
    UPDATE Alojamento
    SET nome_alojamento = novo_nome_alojamento,
        tipo_alojamento = novo_tipo_alojamento,
        class_alojamento = novo_class_alojamento,
        ID_destino = novo_id_destino
    WHERE ID_alojamento = novo_id_alojamento;

    IF SQL%FOUND THEN
        COMMIT;
        DBMS_OUTPUT.PUT_LINE('Alojamento ' || novo_id_alojamento || ' atualizado com sucesso!');
    ELSE
        DBMS_OUTPUT.PUT_LINE('Alojamento ' || novo_id_alojamento || ' nao foi encontrado.');
    END IF;
EXCEPTION
    WHEN OTHERS THEN
        ROLLBACK;
        DBMS_OUTPUT.PUT_LINE('Erro ao atualizar alojamento: ' || SQLERRM);
END;
/

--6. Eliminar um alojamento (o trigger deleteAlojamento apaga as associações).
CREATE OR REPLACE PROCEDURE eliminar_alojamento (
    novo_id_alojamento IN Alojamento.ID_alojamento%TYPE
) AS
BEGIN
    DELETE FROM Alojamento WHERE ID_alojamento = novo_id_alojamento;

    IF SQL%FOUND THEN
        COMMIT;
        DBMS_OUTPUT.PUT_LINE('Alojamento ' || novo_id_alojamento || ' eliminado com sucesso!');
    ELSE
        DBMS_OUTPUT.PUT_LINE('Alojamento ' || novo_id_alojamento || ' nao foi encontrado.');
    END IF;
EXCEPTION
    WHEN OTHERS THEN
        ROLLBACK;
        DBMS_OUTPUT.PUT_LINE('Erro ao eliminar alojamento: ' || SQLERRM);
END;
/

-- ============================================================
-- TRANSPORTE
-- ============================================================

--7. Registar um novo transporte.
CREATE OR REPLACE PROCEDURE registrar_transporte (
    novo_tipo_transporte IN Transporte.tipo_transporte%TYPE,
    novo_cap_transporte IN Transporte.cap_transporte%TYPE,
    novo_disponibilidade IN Transporte.disponibilidade%TYPE
) AS
    v_num NUMBER;
    novo_id_transporte Transporte.ID_transporte%TYPE;
BEGIN
    SELECT ID_transporte.NEXTVAL INTO v_num FROM dual;
    novo_id_transporte := 'TP-' || LPAD(v_num, 3, '0');
    INSERT INTO Transporte (ID_transporte, tipo_transporte, cap_transporte, disponibilidade)
    VALUES (novo_id_transporte, novo_tipo_transporte, novo_cap_transporte, novo_disponibilidade);
    COMMIT;
    DBMS_OUTPUT.PUT_LINE('Transporte ' || novo_id_transporte || ' registado com sucesso!');
EXCEPTION
    WHEN OTHERS THEN
        ROLLBACK;
        DBMS_OUTPUT.PUT_LINE('Erro ao registar transporte: ' || SQLERRM);
END;
/

--8. Atualizar os dados de um transporte.
CREATE OR REPLACE PROCEDURE atualizar_transporte (
    novo_id_transporte IN Transporte.ID_transporte%TYPE,
    novo_tipo_transporte IN Transporte.tipo_transporte%TYPE,
    novo_cap_transporte IN Transporte.cap_transporte%TYPE,
    novo_disponibilidade IN Transporte.disponibilidade%TYPE
) AS
BEGIN
    UPDATE Transporte
    SET tipo_transporte = novo_tipo_transporte,
        cap_transporte = novo_cap_transporte,
        disponibilidade = novo_disponibilidade
    WHERE ID_transporte = novo_id_transporte;

    IF SQL%FOUND THEN
        COMMIT;
        DBMS_OUTPUT.PUT_LINE('Transporte ' || novo_id_transporte || ' atualizado com sucesso!');
    ELSE
        DBMS_OUTPUT.PUT_LINE('Transporte ' || novo_id_transporte || ' nao foi encontrado.');
    END IF;
EXCEPTION
    WHEN OTHERS THEN
        ROLLBACK;
        DBMS_OUTPUT.PUT_LINE('Erro ao atualizar transporte: ' || SQLERRM);
END;
/

--9. Eliminar um transporte (o trigger deleteTransporte apaga as associações).
CREATE OR REPLACE PROCEDURE eliminar_transporte (
    novo_id_transporte IN Transporte.ID_transporte%TYPE
) AS
BEGIN
    DELETE FROM Transporte WHERE ID_transporte = novo_id_transporte;

    IF SQL%FOUND THEN
        COMMIT;
        DBMS_OUTPUT.PUT_LINE('Transporte ' || novo_id_transporte || ' eliminado com sucesso!');
    ELSE
        DBMS_OUTPUT.PUT_LINE('Transporte ' || novo_id_transporte || ' nao foi encontrado.');
    END IF;
EXCEPTION
    WHEN OTHERS THEN
        ROLLBACK;
        DBMS_OUTPUT.PUT_LINE('Erro ao eliminar transporte: ' || SQLERRM);
END;
/

-- ============================================================
-- OPERADOR
-- ============================================================

--10. Registar um novo operador turístico.
CREATE OR REPLACE PROCEDURE registrar_operador (
    novo_nome_operador IN Operador.nome_operador%TYPE,
    novo_especialidade IN Operador.especialidade%TYPE,
    novo_class_operador IN Operador.class_operador%TYPE
) AS
    v_num NUMBER;
    novo_id_operador Operador.ID_operador%TYPE;
BEGIN
    SELECT ID_operador.NEXTVAL INTO v_num FROM dual;
    novo_id_operador := 'OP-' || LPAD(v_num, 3, '0');
    INSERT INTO Operador (ID_operador, nome_operador, especialidade, class_operador)
    VALUES (novo_id_operador, novo_nome_operador, novo_especialidade, novo_class_operador);
    COMMIT;
    DBMS_OUTPUT.PUT_LINE('Operador ' || novo_id_operador || ' registado com sucesso!');
EXCEPTION
    WHEN OTHERS THEN
        ROLLBACK;
        DBMS_OUTPUT.PUT_LINE('Erro ao registar operador: ' || SQLERRM);
END;
/

--11. Atualizar os dados de um operador.
CREATE OR REPLACE PROCEDURE atualizar_operador (
    novo_id_operador IN Operador.ID_operador%TYPE,
    novo_nome_operador IN Operador.nome_operador%TYPE,
    novo_especialidade IN Operador.especialidade%TYPE,
    novo_class_operador IN Operador.class_operador%TYPE
) AS
BEGIN
    UPDATE Operador
    SET nome_operador = novo_nome_operador,
        especialidade = novo_especialidade,
        class_operador = novo_class_operador
    WHERE ID_operador = novo_id_operador;

    IF SQL%FOUND THEN
        COMMIT;
        DBMS_OUTPUT.PUT_LINE('Operador ' || novo_id_operador || ' atualizado com sucesso!');
    ELSE
        DBMS_OUTPUT.PUT_LINE('Operador ' || novo_id_operador || ' nao foi encontrado.');
    END IF;
EXCEPTION
    WHEN OTHERS THEN
        ROLLBACK;
        DBMS_OUTPUT.PUT_LINE('Erro ao atualizar operador: ' || SQLERRM);
END;
/

--12. Eliminar um operador (o trigger deleteOperador apaga as associações).
CREATE OR REPLACE PROCEDURE eliminar_operador (
    novo_id_operador IN Operador.ID_operador%TYPE
) AS
BEGIN
    DELETE FROM Operador WHERE ID_operador = novo_id_operador;

    IF SQL%FOUND THEN
        COMMIT;
        DBMS_OUTPUT.PUT_LINE('Operador ' || novo_id_operador || ' eliminado com sucesso!');
    ELSE
        DBMS_OUTPUT.PUT_LINE('Operador ' || novo_id_operador || ' nao foi encontrado.');
    END IF;
EXCEPTION
    WHEN OTHERS THEN
        ROLLBACK;
        DBMS_OUTPUT.PUT_LINE('Erro ao eliminar operador: ' || SQLERRM);
END;
/

-- ============================================================
-- ATRACAO (registrar_atracao e alterar_horario_atracao estão em procedures.sql)
-- ============================================================

--13. Atualizar todos os dados de uma atração, incluindo o horário especial,
-- numa só instrução (a constraint CHECK vê os valores novos todos juntos).
CREATE OR REPLACE PROCEDURE atualizar_atracao (
    novo_id_atracao IN Atracao.ID_atracao%TYPE,
    novo_tipo_atracao IN Atracao.tipo_atracao%TYPE,
    novo_descricao IN Atracao.descricao%TYPE,
    novo_hora_abertura IN Atracao.hora_abertura%TYPE,
    novo_hora_fecho IN Atracao.hora_fecho%TYPE,
    novo_hora_abertura_especial IN Atracao.hora_abertura_especial%TYPE,
    novo_hora_fecho_especial IN Atracao.hora_fecho_especial%TYPE,
    novo_detalhes_horario_especial IN Atracao.detalhes_horario_especial%TYPE,
    novo_id_destino IN Atracao.ID_destino%TYPE
) AS
BEGIN
    UPDATE Atracao
    SET tipo_atracao = novo_tipo_atracao,
        descricao = novo_descricao,
        hora_abertura = novo_hora_abertura,
        hora_fecho = novo_hora_fecho,
        hora_abertura_especial = novo_hora_abertura_especial,
        hora_fecho_especial = novo_hora_fecho_especial,
        detalhes_horario_especial = novo_detalhes_horario_especial,
        ID_destino = novo_id_destino
    WHERE ID_atracao = novo_id_atracao;

    IF SQL%FOUND THEN
        COMMIT;
        DBMS_OUTPUT.PUT_LINE('Atracao ' || novo_id_atracao || ' atualizada com sucesso!');
    ELSE
        DBMS_OUTPUT.PUT_LINE('Atracao ' || novo_id_atracao || ' nao foi encontrada.');
    END IF;
EXCEPTION
    WHEN OTHERS THEN
        ROLLBACK;
        DBMS_OUTPUT.PUT_LINE('Erro ao atualizar atracao: ' || SQLERRM);
END;
/

--14. Eliminar uma atração.
CREATE OR REPLACE PROCEDURE eliminar_atracao (
    novo_id_atracao IN Atracao.ID_atracao%TYPE
) AS
BEGIN
    DELETE FROM Atracao WHERE ID_atracao = novo_id_atracao;

    IF SQL%FOUND THEN
        COMMIT;
        DBMS_OUTPUT.PUT_LINE('Atracao ' || novo_id_atracao || ' eliminada com sucesso!');
    ELSE
        DBMS_OUTPUT.PUT_LINE('Atracao ' || novo_id_atracao || ' nao foi encontrada.');
    END IF;
EXCEPTION
    WHEN OTHERS THEN
        ROLLBACK;
        DBMS_OUTPUT.PUT_LINE('Erro ao eliminar atracao: ' || SQLERRM);
END;
/

-- ============================================================
-- RESERVA (registrar_reserva, alterar_datas_reserva e cancelar_reserva estão
-- em procedures.sql; o estado muda com a função Actualiza_Status_Reserva)
-- ============================================================

--15. Alterar o turista titular de uma reserva.
CREATE OR REPLACE PROCEDURE alterar_turista_reserva (
    novo_id_reserva IN Reserva.ID_reserva%TYPE,
    novo_id_turista IN Reserva.ID_turista%TYPE
) AS
BEGIN
    UPDATE Reserva
    SET ID_turista = novo_id_turista
    WHERE ID_reserva = novo_id_reserva;

    IF SQL%FOUND THEN
        COMMIT;
        DBMS_OUTPUT.PUT_LINE('Turista da reserva ' || novo_id_reserva || ' alterado com sucesso!');
    ELSE
        DBMS_OUTPUT.PUT_LINE('Reserva ' || novo_id_reserva || ' nao foi encontrada.');
    END IF;
EXCEPTION
    WHEN OTHERS THEN
        ROLLBACK;
        DBMS_OUTPUT.PUT_LINE('Erro ao alterar turista da reserva: ' || SQLERRM);
END;
/

--16. Eliminar uma reserva (o trigger deleteReserva apaga pagamentos e alojamentos).
CREATE OR REPLACE PROCEDURE eliminar_reserva (
    novo_id_reserva IN Reserva.ID_reserva%TYPE
) AS
BEGIN
    DELETE FROM Reserva WHERE ID_reserva = novo_id_reserva;

    IF SQL%FOUND THEN
        COMMIT;
        DBMS_OUTPUT.PUT_LINE('Reserva ' || novo_id_reserva || ' eliminada com sucesso!');
    ELSE
        DBMS_OUTPUT.PUT_LINE('Reserva ' || novo_id_reserva || ' nao foi encontrada.');
    END IF;
EXCEPTION
    WHEN OTHERS THEN
        ROLLBACK;
        DBMS_OUTPUT.PUT_LINE('Erro ao eliminar reserva: ' || SQLERRM);
END;
/

-- ============================================================
-- PAGAMENTO (registrar_pagamento está em procedures.sql; o recibo é gerado
-- pela função Concluir_Pagamento)
-- ============================================================

--17. Atualizar um pagamento ainda não concluído (um pagamento concluído já
-- tem recibo e não pode ser alterado). O recibo nunca é alterado aqui.
CREATE OR REPLACE PROCEDURE atualizar_pagamento (
    novo_id_pagamento IN Pagamento.ID_pagamento%TYPE,
    novo_valor IN Pagamento.valor%TYPE,
    novo_data_pagamento IN Pagamento.data_pagamento%TYPE,
    novo_metodo_pagamento IN Pagamento.metodo_pagamento%TYPE,
    novo_status_pagamento IN Pagamento.status_pagamento%TYPE,
    novo_detalhes_transacao IN Pagamento.detalhes_transacao%TYPE,
    novo_id_reserva IN Pagamento.ID_reserva%TYPE
) AS
BEGIN
    UPDATE Pagamento
    SET valor = novo_valor,
        data_pagamento = novo_data_pagamento,
        metodo_pagamento = novo_metodo_pagamento,
        status_pagamento = novo_status_pagamento,
        detalhes_transacao = novo_detalhes_transacao,
        ID_reserva = novo_id_reserva
    WHERE ID_pagamento = novo_id_pagamento
      AND status_pagamento <> 'CONCLUIDO';

    IF SQL%FOUND THEN
        COMMIT;
        DBMS_OUTPUT.PUT_LINE('Pagamento ' || novo_id_pagamento || ' atualizado com sucesso!');
    ELSE
        DBMS_OUTPUT.PUT_LINE('Pagamento ' || novo_id_pagamento || ' por concluir nao foi encontrado.');
    END IF;
EXCEPTION
    WHEN OTHERS THEN
        ROLLBACK;
        DBMS_OUTPUT.PUT_LINE('Erro ao atualizar pagamento: ' || SQLERRM);
END;
/

--18. Eliminar um pagamento.
CREATE OR REPLACE PROCEDURE eliminar_pagamento (
    novo_id_pagamento IN Pagamento.ID_pagamento%TYPE
) AS
BEGIN
    DELETE FROM Pagamento WHERE ID_pagamento = novo_id_pagamento;

    IF SQL%FOUND THEN
        COMMIT;
        DBMS_OUTPUT.PUT_LINE('Pagamento ' || novo_id_pagamento || ' eliminado com sucesso!');
    ELSE
        DBMS_OUTPUT.PUT_LINE('Pagamento ' || novo_id_pagamento || ' nao foi encontrado.');
    END IF;
EXCEPTION
    WHEN OTHERS THEN
        ROLLBACK;
        DBMS_OUTPUT.PUT_LINE('Erro ao eliminar pagamento: ' || SQLERRM);
END;
/

-- ============================================================
-- TIPO_ALOJAMENTO (definir_tipo_alojamento está em procedures.sql)
-- ============================================================

--19. Atualizar a capacidade de um tipo de alojamento (o nome do tipo é a chave
-- e é referenciado pelos alojamentos, por isso não muda).
CREATE OR REPLACE PROCEDURE atualizar_tipo_alojamento (
    novo_tipo_alojamento IN Tipo_Alojamento.tipo_alojamento%TYPE,
    novo_cap_alojamento IN Tipo_Alojamento.cap_alojamento%TYPE
) AS
BEGIN
    UPDATE Tipo_Alojamento
    SET cap_alojamento = novo_cap_alojamento
    WHERE tipo_alojamento = novo_tipo_alojamento;

    IF SQL%FOUND THEN
        COMMIT;
        DBMS_OUTPUT.PUT_LINE('Tipo de alojamento ' || novo_tipo_alojamento || ' atualizado com sucesso!');
    ELSE
        DBMS_OUTPUT.PUT_LINE('Tipo de alojamento ' || novo_tipo_alojamento || ' nao foi encontrado.');
    END IF;
EXCEPTION
    WHEN OTHERS THEN
        ROLLBACK;
        DBMS_OUTPUT.PUT_LINE('Erro ao atualizar tipo de alojamento: ' || SQLERRM);
END;
/

--20. Eliminar um tipo de alojamento (falha se houver alojamentos desse tipo).
CREATE OR REPLACE PROCEDURE eliminar_tipo_alojamento (
    novo_tipo_alojamento IN Tipo_Alojamento.tipo_alojamento%TYPE
) AS
BEGIN
    DELETE FROM Tipo_Alojamento WHERE tipo_alojamento = novo_tipo_alojamento;

    IF SQL%FOUND THEN
        COMMIT;
        DBMS_OUTPUT.PUT_LINE('Tipo de alojamento ' || novo_tipo_alojamento || ' eliminado com sucesso!');
    ELSE
        DBMS_OUTPUT.PUT_LINE('Tipo de alojamento ' || novo_tipo_alojamento || ' nao foi encontrado.');
    END IF;
EXCEPTION
    WHEN OTHERS THEN
        ROLLBACK;
        DBMS_OUTPUT.PUT_LINE('Erro ao eliminar tipo de alojamento: ' || SQLERRM);
END;
/

-- ============================================================
-- TURISTA (criar e eliminar com as funções Insere_Turista e Elimina_Turista)
-- ============================================================

--21. Atualizar os dados de um turista.
CREATE OR REPLACE PROCEDURE atualizar_turista (
    novo_id_turista IN Turista.ID_turista%TYPE,
    novo_nome_turista IN Turista.nome_turista%TYPE,
    novo_nacionalidade IN Turista.nacionalidade%TYPE
) AS
BEGIN
    UPDATE Turista
    SET nome_turista = novo_nome_turista,
        nacionalidade = novo_nacionalidade
    WHERE ID_turista = novo_id_turista;

    IF SQL%FOUND THEN
        COMMIT;
        DBMS_OUTPUT.PUT_LINE('Turista ' || novo_id_turista || ' atualizado com sucesso!');
    ELSE
        DBMS_OUTPUT.PUT_LINE('Turista ' || novo_id_turista || ' nao foi encontrado.');
    END IF;
EXCEPTION
    WHEN OTHERS THEN
        ROLLBACK;
        DBMS_OUTPUT.PUT_LINE('Erro ao atualizar turista: ' || SQLERRM);
END;
/

-- ============================================================
-- TABELAS DE ASSOCIAÇÃO: remover uma ligação
-- ============================================================

--22. Remover uma preferência de um turista.
CREATE OR REPLACE PROCEDURE remover_preferencia (
    novo_id_turista IN Turista_Preferencia.ID_turista%TYPE,
    novo_preferencia IN Turista_Preferencia.preferencia%TYPE
) AS
BEGIN
    DELETE FROM Turista_Preferencia
    WHERE ID_turista = novo_id_turista AND preferencia = novo_preferencia;

    IF SQL%FOUND THEN
        COMMIT;
        DBMS_OUTPUT.PUT_LINE('Preferencia ' || novo_preferencia || ' do turista ' || novo_id_turista || ' removida com sucesso!');
    ELSE
        DBMS_OUTPUT.PUT_LINE('Preferencia ' || novo_preferencia || ' do turista ' || novo_id_turista || ' nao foi encontrada.');
    END IF;
EXCEPTION
    WHEN OTHERS THEN
        ROLLBACK;
        DBMS_OUTPUT.PUT_LINE('Erro ao remover preferencia: ' || SQLERRM);
END;
/

--23. Remover o registo de uma visita de um turista a um destino.
CREATE OR REPLACE PROCEDURE remover_visita (
    novo_id_destino IN Turista_Destino.ID_destino%TYPE,
    novo_id_turista IN Turista_Destino.ID_turista%TYPE,
    novo_data_visita IN Turista_Destino.data_visita%TYPE
) AS
BEGIN
    DELETE FROM Turista_Destino
    WHERE ID_destino = novo_id_destino
      AND ID_turista = novo_id_turista
      AND data_visita = TRUNC(novo_data_visita);

    IF SQL%FOUND THEN
        COMMIT;
        DBMS_OUTPUT.PUT_LINE('Visita do turista ' || novo_id_turista || ' ao destino ' || novo_id_destino || ' removida com sucesso!');
    ELSE
        DBMS_OUTPUT.PUT_LINE('Visita do turista ' || novo_id_turista || ' ao destino ' || novo_id_destino || ' nao foi encontrada.');
    END IF;
EXCEPTION
    WHEN OTHERS THEN
        ROLLBACK;
        DBMS_OUTPUT.PUT_LINE('Erro ao remover visita: ' || SQLERRM);
END;
/

--24. Remover um alojamento de uma reserva.
CREATE OR REPLACE PROCEDURE remover_alojamento_reserva (
    novo_id_reserva IN Reserva_Alojamento.ID_reserva%TYPE,
    novo_id_alojamento IN Reserva_Alojamento.ID_alojamento%TYPE
) AS
BEGIN
    DELETE FROM Reserva_Alojamento
    WHERE ID_reserva = novo_id_reserva AND ID_alojamento = novo_id_alojamento;

    IF SQL%FOUND THEN
        COMMIT;
        DBMS_OUTPUT.PUT_LINE('Alojamento ' || novo_id_alojamento || ' removido da reserva ' || novo_id_reserva || ' com sucesso!');
    ELSE
        DBMS_OUTPUT.PUT_LINE('Alojamento ' || novo_id_alojamento || ' da reserva ' || novo_id_reserva || ' nao foi encontrado.');
    END IF;
EXCEPTION
    WHEN OTHERS THEN
        ROLLBACK;
        DBMS_OUTPUT.PUT_LINE('Erro ao remover alojamento da reserva: ' || SQLERRM);
END;
/

--25. Remover a associação entre um operador e um turista.
CREATE OR REPLACE PROCEDURE remover_operador_turista (
    novo_id_operador IN Operador_Turista.ID_operador%TYPE,
    novo_id_turista IN Operador_Turista.ID_turista%TYPE
) AS
BEGIN
    DELETE FROM Operador_Turista
    WHERE ID_operador = novo_id_operador AND ID_turista = novo_id_turista;

    IF SQL%FOUND THEN
        COMMIT;
        DBMS_OUTPUT.PUT_LINE('Operador ' || novo_id_operador || ' desassociado do turista ' || novo_id_turista || ' com sucesso!');
    ELSE
        DBMS_OUTPUT.PUT_LINE('Associacao do operador ' || novo_id_operador || ' ao turista ' || novo_id_turista || ' nao foi encontrada.');
    END IF;
EXCEPTION
    WHEN OTHERS THEN
        ROLLBACK;
        DBMS_OUTPUT.PUT_LINE('Erro ao remover operador do turista: ' || SQLERRM);
END;
/

--26. Remover a associação entre um operador e um transporte.
CREATE OR REPLACE PROCEDURE remover_operador_transporte (
    novo_id_operador IN Operador_Transporte.ID_operador%TYPE,
    novo_id_transporte IN Operador_Transporte.ID_transporte%TYPE
) AS
BEGIN
    DELETE FROM Operador_Transporte
    WHERE ID_operador = novo_id_operador AND ID_transporte = novo_id_transporte;

    IF SQL%FOUND THEN
        COMMIT;
        DBMS_OUTPUT.PUT_LINE('Operador ' || novo_id_operador || ' desassociado do transporte ' || novo_id_transporte || ' com sucesso!');
    ELSE
        DBMS_OUTPUT.PUT_LINE('Associacao do operador ' || novo_id_operador || ' ao transporte ' || novo_id_transporte || ' nao foi encontrada.');
    END IF;
EXCEPTION
    WHEN OTHERS THEN
        ROLLBACK;
        DBMS_OUTPUT.PUT_LINE('Erro ao remover operador do transporte: ' || SQLERRM);
END;
/

--27. Remover a associação entre um operador e um alojamento.
CREATE OR REPLACE PROCEDURE remover_operador_alojamento (
    novo_id_operador IN Operador_Alojamento.ID_operador%TYPE,
    novo_id_alojamento IN Operador_Alojamento.ID_alojamento%TYPE
) AS
BEGIN
    DELETE FROM Operador_Alojamento
    WHERE ID_operador = novo_id_operador AND ID_alojamento = novo_id_alojamento;

    IF SQL%FOUND THEN
        COMMIT;
        DBMS_OUTPUT.PUT_LINE('Operador ' || novo_id_operador || ' desassociado do alojamento ' || novo_id_alojamento || ' com sucesso!');
    ELSE
        DBMS_OUTPUT.PUT_LINE('Associacao do operador ' || novo_id_operador || ' ao alojamento ' || novo_id_alojamento || ' nao foi encontrada.');
    END IF;
EXCEPTION
    WHEN OTHERS THEN
        ROLLBACK;
        DBMS_OUTPUT.PUT_LINE('Erro ao remover operador do alojamento: ' || SQLERRM);
END;
/

-- Verificação: devem existir 43 procedures e nenhuma INVALID
SELECT object_name, status FROM user_objects WHERE object_type = 'PROCEDURE' ORDER BY object_name;
SELECT COUNT(*) AS total_procedures FROM user_objects WHERE object_type = 'PROCEDURE';
