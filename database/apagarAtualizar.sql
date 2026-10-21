-- ============================================================
-- TRABALHO PRÁTICO - BASE DE DADOS I (ISCTEM 2026)
-- GRUPO 1: GESTÃO DE TURISMO EM MOÇAMBIQUE
-- apagarAtualizar.sql: APAGA todo o esquema do projeto e volta a criá-lo
-- (Oracle 10g XE)
--
-- ATENÇÃO: perdem-se TODOS os dados, incluindo o histórico de auditoria.
-- Apaga só os objetos do projeto, pelo nome: as 39 views, as 15 tabelas,
-- as 15 tabelas de histórico (os triggers e índices vão com as tabelas),
-- as 9 sequências, as procedures e as funções. Depois corre main.sql.
--
-- Executar a partir de database/:
--   cd database
--   sqlplus yosef@//192.168.64.4:1521/XE @apagarAtualizar.sql
-- ============================================================

SET SERVEROUTPUT ON
SET DEFINE OFF

DECLARE
    -- Apaga um objeto; se ainda não existir, não faz nada.
    PROCEDURE apagar(p_tipo VARCHAR2, p_nome VARCHAR2) IS
    BEGIN
        IF p_tipo = 'TABLE' THEN
            -- PURGE: não fica na reciclagem (BIN$...) do Oracle 10g
            EXECUTE IMMEDIATE 'DROP TABLE ' || p_nome || ' CASCADE CONSTRAINTS PURGE';
        ELSE
            EXECUTE IMMEDIATE 'DROP ' || p_tipo || ' ' || p_nome;
        END IF;
        DBMS_OUTPUT.PUT_LINE('Apagado: ' || p_tipo || ' ' || p_nome);
    EXCEPTION
        WHEN OTHERS THEN
            -- -942 tabela/view, -2289 sequência, -4043 procedure/função: não existe
            IF SQLCODE IN (-942, -2289, -4043) THEN
                NULL;
            ELSE
                RAISE;
            END IF;
    END apagar;
BEGIN
    -- 1. Views
    apagar('VIEW', 'v_atracoes_destino');
    apagar('VIEW', 'v_total_atracoes_destino');
    apagar('VIEW', 'v_total_alojamentos_destino');
    apagar('VIEW', 'v_alojamentos_detalhe');
    apagar('VIEW', 'v_media_avaliacao_alojamento');
    apagar('VIEW', 'v_reservas_detalhe');
    apagar('VIEW', 'v_reservas_ativas');
    apagar('VIEW', 'v_reservas_alojamento');
    apagar('VIEW', 'v_total_pago_reserva');
    apagar('VIEW', 'v_recibos');
    apagar('VIEW', 'v_receita_mensal');
    apagar('VIEW', 'v_receita_nacionalidade');
    apagar('VIEW', 'v_turista_destinos');
    apagar('VIEW', 'v_turista_preferencias');
    apagar('VIEW', 'v_avaliacao_operador');
    apagar('VIEW', 'v_operadores_alojamentos');
    apagar('VIEW', 'v_operadores_transportes');
    apagar('VIEW', 'v_transportes_disponiveis');
    apagar('VIEW', 'v_aud_pagamento_data_futura');
    apagar('VIEW', 'v_aud_recibo_sem_concluido');
    apagar('VIEW', 'v_reservas_sem_alojamento');
    apagar('VIEW', 'v_reservas_sem_pagamento');
    apagar('VIEW', 'v_alojamentos_acima_media');
    apagar('VIEW', 'v_turistas_sem_reserva');
    apagar('VIEW', 'v_lista_destinos');
    apagar('VIEW', 'v_lista_atracoes');
    apagar('VIEW', 'v_lista_alojamentos');
    apagar('VIEW', 'v_lista_tipos_alojamento');
    apagar('VIEW', 'v_lista_transportes');
    apagar('VIEW', 'v_lista_turistas');
    apagar('VIEW', 'v_lista_preferencias');
    apagar('VIEW', 'v_lista_operadores');
    apagar('VIEW', 'v_lista_reservas');
    apagar('VIEW', 'v_lista_pagamentos');
    apagar('VIEW', 'v_lista_visitas');
    apagar('VIEW', 'v_lista_reserva_alojamentos');
    apagar('VIEW', 'v_lista_operador_turistas');
    apagar('VIEW', 'v_lista_operador_transportes');
    apagar('VIEW', 'v_lista_operador_alojamentos');

    -- 2. Tabelas (com CASCADE CONSTRAINTS a ordem não importa)
    apagar('TABLE', 'Turista_Destino');
    apagar('TABLE', 'Operador_Turista');
    apagar('TABLE', 'Operador_Alojamento');
    apagar('TABLE', 'Operador_Transporte');
    apagar('TABLE', 'Reserva_Alojamento');
    apagar('TABLE', 'Pagamento');
    apagar('TABLE', 'Transporte');
    apagar('TABLE', 'Alojamento');
    apagar('TABLE', 'Operador');
    apagar('TABLE', 'Reserva');
    apagar('TABLE', 'Turista_Preferencia');
    apagar('TABLE', 'Turista');
    apagar('TABLE', 'Atracao');
    apagar('TABLE', 'Destino');
    apagar('TABLE', 'Tipo_Alojamento');

    -- 3. Tabelas de histórico
    apagar('TABLE', 'auditoria_alojamento');
    apagar('TABLE', 'auditoria_destino');
    apagar('TABLE', 'auditoria_atracao');
    apagar('TABLE', 'auditoria_turista');
    apagar('TABLE', 'auditoria_tipoAlojamento');
    apagar('TABLE', 'auditoria_reserva');
    apagar('TABLE', 'auditoria_operador');
    apagar('TABLE', 'auditoria_transporte');
    apagar('TABLE', 'auditoria_pagamento');
    apagar('TABLE', 'auditoria_turistaDestino');
    apagar('TABLE', 'auditoria_operadorAlojamento');
    apagar('TABLE', 'auditoria_operadorTransporte');
    apagar('TABLE', 'auditoria_operadorTurista');
    apagar('TABLE', 'auditoria_reservaAlojamento');
    apagar('TABLE', 'auditoria_turistaPreferencia');

    -- 4. Sequências
    apagar('SEQUENCE', 'ID_destino');
    apagar('SEQUENCE', 'ID_atracao');
    apagar('SEQUENCE', 'ID_turista');
    apagar('SEQUENCE', 'ID_reserva');
    apagar('SEQUENCE', 'ID_operador');
    apagar('SEQUENCE', 'ID_alojamento');
    apagar('SEQUENCE', 'ID_transporte');
    apagar('SEQUENCE', 'ID_pagamento');
    apagar('SEQUENCE', 'ID_recibo');

    -- 5. Procedures (procedures.sql)
    apagar('PROCEDURE', 'registrar_reserva');
    apagar('PROCEDURE', 'associar_alojamento_reserva');
    apagar('PROCEDURE', 'registrar_pagamento');
    apagar('PROCEDURE', 'registrar_visita');
    apagar('PROCEDURE', 'registrar_atracao');
    apagar('PROCEDURE', 'definir_tipo_alojamento');
    apagar('PROCEDURE', 'inserir_preferencia');
    apagar('PROCEDURE', 'atualizar_pref_turista');
    apagar('PROCEDURE', 'associar_operador_turista');
    apagar('PROCEDURE', 'associar_operador_transporte');
    apagar('PROCEDURE', 'associar_operador_alojamento');
    apagar('PROCEDURE', 'cancelar_reserva');
    apagar('PROCEDURE', 'alterar_horario_atracao');
    apagar('PROCEDURE', 'alterar_datas_reserva');
    apagar('PROCEDURE', 'avaliar_alojamento');
    apagar('PROCEDURE', 'avaliar_operador');

    -- 6. Funções (functions.sql)
    apagar('FUNCTION', 'Total_Pago_Reserva');
    apagar('FUNCTION', 'Capacidade_Alojamento');
    apagar('FUNCTION', 'Media_Avaliacao_Alojamento');
    apagar('FUNCTION', 'Media_Avaliacao_Operador');
    apagar('FUNCTION', 'Valor_Em_Dolar');
    apagar('FUNCTION', 'Retorna_Info_Destino');
    apagar('FUNCTION', 'Lista_Atracoes_Destino');
    apagar('FUNCTION', 'Reserva_Completa');
    apagar('FUNCTION', 'Insere_Turista');
    apagar('FUNCTION', 'Regista_Visita');
    apagar('FUNCTION', 'Concluir_Pagamento');
    apagar('FUNCTION', 'Actualiza_Status_Reserva');
    apagar('FUNCTION', 'Elimina_Turista');
END;
/

-- Esvazia a reciclagem do utilizador (tabelas apagadas antes sem PURGE)
PURGE RECYCLEBIN;

-- Confirmação: não deve restar nenhum objeto do projeto
SELECT object_type, COUNT(*) AS total FROM user_objects GROUP BY object_type ORDER BY object_type;

-- Volta a criar tudo
@@main.sql
