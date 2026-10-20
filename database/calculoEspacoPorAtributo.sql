SET SERVEROUTPUT ON SIZE UNLIMITED;

PROMPT ====================================================================
PROMPT      ESTUDO DE ESPACO EM DISCO POR ATRIBUTO E POR TABELA (ORACLE)
PROMPT      (executar depois de tabelas.sql)
PROMPT ====================================================================

DECLARE
    v_soma_atributos NUMBER;
    v_qtd_atributos   NUMBER;
    v_tam_linha       NUMBER;
    v_total_bd_bytes  NUMBER := 0;

    PROCEDURE calcular_tabela_detalhada(p_nome_tabela VARCHAR2, p_linhas NUMBER) IS
        v_tam_atributo NUMBER;
    BEGIN
        v_soma_atributos := 0;
        v_qtd_atributos := 0;

        DBMS_OUTPUT.PUT_LINE('--------------------------------------------------------------------');
        DBMS_OUTPUT.PUT_LINE('TABELA: ' || UPPER(p_nome_tabela) || ' (Projecao: ' || p_linhas || ' linhas)');
        DBMS_OUTPUT.PUT_LINE('--------------------------------------------------------------------');

        FOR c IN (SELECT column_name, data_type, data_length, data_precision   -- CORRIGIDO: le tambem a precisao
                  FROM user_tab_cols 
                  WHERE table_name = UPPER(p_nome_tabela)
                  ORDER BY column_id) LOOP
            
            v_qtd_atributos := v_qtd_atributos + 1;
            
            IF c.data_type = 'DATE' THEN
                v_tam_atributo := 7 + 1; -- 7 bytes de dado + 1 byte header
            ELSIF c.data_type = 'CHAR' THEN
                v_tam_atributo := c.data_length + 1; -- N bytes de dado + 1 byte header
            ELSIF c.data_type = 'NUMBER' THEN
                -- CORRIGIDO: o Oracle guarda um NUMBER em CEIL(precisao/2)+1 bytes
                -- (e nao sempre 5): NUMBER(2,1) ocupa 3 bytes, NUMBER(10,2) ocupa 7.
                IF c.data_precision IS NOT NULL THEN
                    v_tam_atributo := CEIL(c.data_precision / 2) + 1 + 1; -- dado + 1 byte header
                ELSE
                    v_tam_atributo := 5 + 1;
                END IF;
            ELSIF SUBSTR(c.column_name, 1, 3) = 'ID_' THEN
                -- CORRIGIDO: os identificadores (DT-001, RS-001, ...) ocupam sempre
                -- os 6 caracteres, e nao 50% do tamanho maximo.
                v_tam_atributo := c.data_length + 1;
            ELSE
                -- Para VARCHAR2, estimamos utilizacao media de 50% do tamanho maximo
                v_tam_atributo := ROUND(c.data_length * 0.5) + 1;
            END IF;

            v_soma_atributos := v_soma_atributos + v_tam_atributo;

            DBMS_OUTPUT.PUT_LINE('   > Atributo: ' || RPAD(c.column_name, 28) || 
                                 ' | Tipo: ' || RPAD(c.data_type, 10) || 
                                 ' | Tam. Medio: ' || LPAD(v_tam_atributo, 3) || ' Bytes');
        END LOOP;

        IF v_qtd_atributos > 0 THEN
            v_tam_linha := v_soma_atributos + 3; -- 3 bytes para Row Header
            v_total_bd_bytes := v_total_bd_bytes + (v_tam_linha * p_linhas);

            DBMS_OUTPUT.PUT_LINE('   -----------------------------------------------------------------');
            DBMS_OUTPUT.PUT_LINE('   > Cabecalho da Linha (Row Header)  : 3 Bytes');
            DBMS_OUTPUT.PUT_LINE('   > TAMANHO MEDIO POR LINHA          : ' || v_tam_linha || ' Bytes/linha');
            DBMS_OUTPUT.PUT_LINE('   > ESPACO UTIL REAL DE DADOS        : ' || ROUND((v_tam_linha * p_linhas) / 1024 / 1024, 4) || ' MB');
            DBMS_OUTPUT.PUT_LINE(' ');
        ELSE
            DBMS_OUTPUT.PUT_LINE('   [AVISO] Tabela ' || UPPER(p_nome_tabela) || ' nao encontrada no dicionario do Oracle.');
            DBMS_OUTPUT.PUT_LINE(' ');
        END IF;
    END;

BEGIN
    calcular_tabela_detalhada('TIPO_ALOJAMENTO', 20);
    calcular_tabela_detalhada('DESTINO', 100);
    calcular_tabela_detalhada('ATRACAO', 2000);
    calcular_tabela_detalhada('TURISTA', 50000);
    calcular_tabela_detalhada('TURISTA_PREFERENCIA', 50000);
    calcular_tabela_detalhada('RESERVA', 100000);
    calcular_tabela_detalhada('OPERADOR', 500);
    calcular_tabela_detalhada('ALOJAMENTO', 1000);
    calcular_tabela_detalhada('TRANSPORTE', 1000);
    calcular_tabela_detalhada('PAGAMENTO', 100000);
    calcular_tabela_detalhada('RESERVA_ALOJAMENTO', 100000);
    calcular_tabela_detalhada('OPERADOR_TRANSPORTE', 2000);
    calcular_tabela_detalhada('OPERADOR_ALOJAMENTO', 3000);
    calcular_tabela_detalhada('OPERADOR_TURISTA', 50000);
    calcular_tabela_detalhada('TURISTA_DESTINO', 100000);

    DBMS_OUTPUT.PUT_LINE('====================================================================');
    DBMS_OUTPUT.PUT_LINE('SOMA TOTAL UTIL DA BASE DE DADOS      : ' || ROUND(v_total_bd_bytes / 1024 / 1024, 2) || ' MB');
    DBMS_OUTPUT.PUT_LINE('ESPACO FISICO RECOMENDADO EM DISCO    : ' || ROUND((v_total_bd_bytes / 1024 / 1024) / 0.85, 2) || ' MB');
    -- CORRIGIDO: pressupostos do calculo, para constarem do relatorio
    DBMS_OUTPUT.PUT_LINE('--------------------------------------------------------------------');
    DBMS_OUTPUT.PUT_LINE('Pressupostos: VARCHAR2 com 50% do tamanho maximo (identificadores a 100%);');
    DBMS_OUTPUT.PUT_LINE('15% de reserva por bloco (PCTFREE e cabecalho do bloco);');
    DBMS_OUTPUT.PUT_LINE('NAO inclui indices nem espaco de undo/redo.');
    DBMS_OUTPUT.PUT_LINE('====================================================================');
END;
/