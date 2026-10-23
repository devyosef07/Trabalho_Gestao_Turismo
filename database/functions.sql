-- A. Funcoes de consulta (podem ser usadas dentro de um SELECT)
-- B. Funcoes que gravam dados (chamam-se dentro de um bloco PL/SQL)
-- ============================================================

SET SERVEROUTPUT ON SIZE UNLIMITED;

-- A1. Total ja pago numa reserva (so pagamentos CONCLUIDO)
CREATE OR REPLACE FUNCTION Total_Pago_Reserva (p_reserva IN Reserva.ID_reserva%TYPE)
RETURN NUMBER
IS
  v_total NUMBER;
BEGIN
  SELECT NVL(SUM(valor), 0)
  INTO v_total
  FROM Pagamento
  WHERE ID_reserva = p_reserva
    AND status_pagamento = 'CONCLUIDO';
  RETURN v_total;
END Total_Pago_Reserva;
/

-- A2. Capacidade de um alojamento (vem do tipo de alojamento)
CREATE OR REPLACE FUNCTION Capacidade_Alojamento (p_alojamento IN Alojamento.ID_alojamento%TYPE)
RETURN NUMBER
IS
  v_cap Tipo_Alojamento.cap_alojamento%TYPE;
BEGIN
  SELECT t.cap_alojamento
  INTO v_cap
  FROM Alojamento a, Tipo_Alojamento t
  WHERE a.tipo_alojamento = t.tipo_alojamento
    AND a.ID_alojamento = p_alojamento;
  RETURN v_cap;
EXCEPTION
  WHEN NO_DATA_FOUND THEN
    RETURN NULL;
END Capacidade_Alojamento;
/

-- A3. Media das avaliacoes que um alojamento recebeu nas reservas
CREATE OR REPLACE FUNCTION Media_Avaliacao_Alojamento (p_alojamento IN Alojamento.ID_alojamento%TYPE)
RETURN NUMBER
IS
  v_media NUMBER;
BEGIN
  SELECT AVG(avaliacao_alojamento)
  INTO v_media
  FROM Reserva_Alojamento
  WHERE ID_alojamento = p_alojamento;
  RETURN ROUND(v_media, 1);
END Media_Avaliacao_Alojamento;
/

-- A4. Media das avaliacoes que um operador recebeu dos turistas
CREATE OR REPLACE FUNCTION Media_Avaliacao_Operador (p_operador IN Operador.ID_operador%TYPE)
RETURN NUMBER
IS
  v_media NUMBER;
BEGIN
  SELECT AVG(avaliacao_operador)
  INTO v_media
  FROM Operador_Turista
  WHERE ID_operador = p_operador;
  RETURN ROUND(v_media, 1);
END Media_Avaliacao_Operador;
/

-- A5. Converte meticais em dolares (como o exemplo ValorEmDolar da ficha)
CREATE OR REPLACE FUNCTION Valor_Em_Dolar (p_meticais IN NUMBER, p_cotacao IN NUMBER)
RETURN NUMBER
IS
BEGIN
  IF p_cotacao IS NULL OR p_cotacao <= 0 THEN
    RETURN NULL;
  END IF;
  RETURN ROUND(p_meticais / p_cotacao, 2);
END Valor_Em_Dolar;
/

-- A6. Informacao de um destino, com tratamento de erros (como Retorna_Info_Curso_Tratada da ficha)
CREATE OR REPLACE FUNCTION Retorna_Info_Destino (p_destino IN Destino.ID_destino%TYPE)
RETURN VARCHAR2
IS
  v_nome   Destino.nome_destino%TYPE;
  v_regiao Destino.regiao%TYPE;
  v_cap    Destino.capacidade_visitantes%TYPE;
  msg      VARCHAR2(100);
BEGIN
  SELECT nome_destino, regiao, capacidade_visitantes
  INTO v_nome, v_regiao, v_cap
  FROM Destino
  WHERE ID_destino = p_destino;
  RETURN ('Destino: ' || v_nome || CHR(10) ||
          'Regiao: ' || v_regiao || CHR(10) ||
          'Capacidade: ' || TO_CHAR(v_cap));
EXCEPTION
  WHEN NO_DATA_FOUND THEN
    RETURN ('Nenhum destino tem o identificador indicado');
  WHEN OTHERS THEN
    msg := 'Erro desconhecido: ' || TO_CHAR(SQLCODE);
    RETURN (msg);
END Retorna_Info_Destino;
/

-- A7. Lista as atracoes de um destino (cursor explicito com parametro)
CREATE OR REPLACE FUNCTION Lista_Atracoes_Destino (p_destino IN Destino.ID_destino%TYPE)
RETURN VARCHAR2
IS
  CURSOR c_atracoes (p_dest IN Atracao.ID_destino%TYPE) IS
    SELECT descricao, hora_abertura, hora_fecho
    FROM Atracao
    WHERE ID_destino = p_dest
    ORDER BY ID_atracao;
  v_descricao Atracao.descricao%TYPE;
  v_abre      Atracao.hora_abertura%TYPE;
  v_fecha     Atracao.hora_fecho%TYPE;
  v_lista     VARCHAR2(2000) := '';
BEGIN
  OPEN c_atracoes (p_destino);
  FETCH c_atracoes INTO v_descricao, v_abre, v_fecha;
  WHILE c_atracoes%FOUND LOOP
    v_lista := v_lista || v_descricao || ' (' || v_abre || '-' || v_fecha || ')' || CHR(10);
    FETCH c_atracoes INTO v_descricao, v_abre, v_fecha;
  END LOOP;
  CLOSE c_atracoes;
  IF v_lista IS NULL THEN
    RETURN 'Este destino nao tem atracoes registadas';
  END IF;
  RETURN v_lista;
EXCEPTION
  WHEN OTHERS THEN
    IF c_atracoes%ISOPEN THEN
      CLOSE c_atracoes;
    END IF;
    RETURN 'Erro desconhecido: ' || TO_CHAR(SQLCODE);
END Lista_Atracoes_Destino;
/

-- A8. Verifica a regra 4.1: uma reserva tem pelo menos um alojamentoe pelo menos um pagamento
CREATE OR REPLACE FUNCTION Reserva_Completa (p_reserva IN Reserva.ID_reserva%TYPE)
RETURN VARCHAR2
IS
  v_existe NUMBER;
  v_aloj   NUMBER;
  v_pag    NUMBER;
BEGIN
  SELECT COUNT(*) INTO v_existe FROM Reserva WHERE ID_reserva = p_reserva;
  IF v_existe = 0 THEN
    RETURN 'Reserva inexistente';
  END IF;
  SELECT COUNT(*) INTO v_aloj FROM Reserva_Alojamento WHERE ID_reserva = p_reserva;
  SELECT COUNT(*) INTO v_pag FROM Pagamento WHERE ID_reserva = p_reserva;
  IF v_aloj = 0 AND v_pag = 0 THEN
    RETURN 'Falta alojamento e pagamento';
  ELSIF v_aloj = 0 THEN
    RETURN 'Falta alojamento';
  ELSIF v_pag = 0 THEN
    RETURN 'Falta pagamento';
  ELSE
    RETURN 'Completa';
  END IF;
END Reserva_Completa;
/

-- B1. Insere um turista (usa a sequencia ID_turista; controla os erros)
CREATE OR REPLACE FUNCTION Insere_Turista (p_nome IN Turista.nome_turista%TYPE,
                                           p_nacionalidade IN Turista.nacionalidade%TYPE)
RETURN VARCHAR2
IS
  v_num NUMBER;
  v_id  Turista.ID_turista%TYPE;
BEGIN
  SELECT ID_turista.NEXTVAL INTO v_num FROM dual;
  v_id := 'TR-' || LPAD(v_num, 3, '0');
  INSERT INTO Turista (ID_turista, nome_turista, nacionalidade)
  VALUES (v_id, p_nome, p_nacionalidade);
  COMMIT;
  RETURN 'Turista inserido com o codigo ' || v_id;
EXCEPTION
  WHEN DUP_VAL_ON_INDEX THEN
    ROLLBACK;
    RETURN 'Erro: o turista ja existe';
  WHEN OTHERS THEN
    ROLLBACK;
    RETURN 'Erro desconhecido: ' || TO_CHAR(SQLCODE) || ' - ' || SQLERRM;
END Insere_Turista;
/

-- B2. Regista a visita de um turista a um destino, se o destino nao estiver cheio
CREATE OR REPLACE FUNCTION Regista_Visita (p_destino IN Destino.ID_destino%TYPE,
                                           p_turista IN Turista.ID_turista%TYPE,
                                           p_data    IN DATE)
RETURN VARCHAR2
IS
  destino_lotado EXCEPTION;
  v_cap          Destino.capacidade_visitantes%TYPE;
  v_visitantes   NUMBER;
BEGIN
  SELECT capacidade_visitantes
  INTO v_cap
  FROM Destino
  WHERE ID_destino = p_destino;

  SELECT COUNT(*)
  INTO v_visitantes
  FROM Turista_Destino
  WHERE ID_destino = p_destino
    AND data_visita = TRUNC(p_data);

  IF v_visitantes >= v_cap THEN
    RAISE destino_lotado;
  END IF;

  INSERT INTO Turista_Destino (ID_destino, ID_turista, data_visita)
  VALUES (p_destino, p_turista, TRUNC(p_data));
  COMMIT;
  RETURN 'Visita registada';
EXCEPTION
  WHEN destino_lotado THEN
    RETURN 'Destino lotado nessa data';
  WHEN NO_DATA_FOUND THEN
    RETURN 'Destino inexistente';
  WHEN DUP_VAL_ON_INDEX THEN
    ROLLBACK;
    RETURN 'Esse turista ja tem uma visita registada a este destino nessa data';
  WHEN OTHERS THEN
    ROLLBACK;
    RETURN 'Erro desconhecido: ' || TO_CHAR(SQLCODE) || ' - ' || SQLERRM;
END Regista_Visita;
/

-- B3. Conclui um pagamento pendente e gera o recibo
CREATE OR REPLACE FUNCTION Concluir_Pagamento (p_pagamento IN Pagamento.ID_pagamento%TYPE)
RETURN VARCHAR2
IS
  estado_invalido EXCEPTION;
  v_estado Pagamento.status_pagamento%TYPE;
  v_valor  Pagamento.valor%TYPE;
  v_metodo Pagamento.metodo_pagamento%TYPE;
  v_data   Pagamento.data_pagamento%TYPE;
  v_num    NUMBER;
  v_recibo Pagamento.ID_recibo%TYPE;
BEGIN
  SELECT status_pagamento, valor, metodo_pagamento, data_pagamento
  INTO v_estado, v_valor, v_metodo, v_data
  FROM Pagamento
  WHERE ID_pagamento = p_pagamento;

  IF v_estado <> 'PENDENTE' THEN
    RAISE estado_invalido;
  END IF;

  SELECT ID_recibo.NEXTVAL INTO v_num FROM dual;
  v_recibo := 'RC-' || LPAD(v_num, 3, '0');

  UPDATE Pagamento
  SET status_pagamento = 'CONCLUIDO',
      ID_recibo = v_recibo,
      detalhes_transacao = 'Recibo do pagamento ' || p_pagamento ||
                           ' | metodo: ' || v_metodo ||
                           ' | valor: ' || TO_CHAR(v_valor, 'FM999999990.00') ||
                           ' | data: ' || TO_CHAR(v_data, 'DD-MM-YYYY')
  WHERE ID_pagamento = p_pagamento;
  COMMIT;
  RETURN 'Pagamento concluido. Recibo ' || v_recibo;
EXCEPTION
  WHEN estado_invalido THEN
    RETURN 'O pagamento ja nao esta pendente (estado: ' || v_estado || ')';
  WHEN NO_DATA_FOUND THEN
    RETURN 'Pagamento inexistente';
  WHEN OTHERS THEN
    ROLLBACK;
    RETURN 'Erro desconhecido: ' || TO_CHAR(SQLCODE) || ' - ' || SQLERRM;
END Concluir_Pagamento;
/

-- B4. Actualiza o estado de uma reserva
CREATE OR REPLACE FUNCTION Actualiza_Status_Reserva (p_reserva IN Reserva.ID_reserva%TYPE,
                                                     p_status  IN Reserva.status_reserva%TYPE)
RETURN VARCHAR2
IS
BEGIN
  UPDATE Reserva
  SET status_reserva = p_status
  WHERE ID_reserva = p_reserva;
  IF SQL%NOTFOUND THEN
    RETURN 'Reserva inexistente';
  END IF;
  COMMIT;
  RETURN 'Reserva ' || p_reserva || ' actualizada para ' || p_status;
EXCEPTION
  WHEN OTHERS THEN
    ROLLBACK;
    IF SQLCODE = -2290 THEN
      RETURN 'Estado invalido: use PENDENTE, CONFIRMADA, CANCELADA ou CONCLUIDA';
    END IF;
    RETURN 'Erro desconhecido: ' || TO_CHAR(SQLCODE) || ' - ' || SQLERRM;
END Actualiza_Status_Reserva;
/

-- B5. Elimina um turista que nao tenha reservas
CREATE OR REPLACE FUNCTION Elimina_Turista (p_turista IN Turista.ID_turista%TYPE)
RETURN VARCHAR2
IS
  tem_reservas EXCEPTION;
  v_reservas   NUMBER;
BEGIN
  SELECT COUNT(*) INTO v_reservas FROM Reserva WHERE ID_turista = p_turista;
  IF v_reservas > 0 THEN
    RAISE tem_reservas;
  END IF;

  DELETE FROM Turista_Preferencia WHERE ID_turista = p_turista;
  DELETE FROM Operador_Turista WHERE ID_turista = p_turista;
  DELETE FROM Turista_Destino WHERE ID_turista = p_turista;
  DELETE FROM Turista WHERE ID_turista = p_turista;
  IF SQL%NOTFOUND THEN
    ROLLBACK;
    RETURN 'Turista inexistente';
  END IF;
  COMMIT;
  RETURN 'Turista eliminado';
EXCEPTION
  WHEN tem_reservas THEN
    RETURN 'Nao e possivel eliminar: o turista tem reservas';
  WHEN OTHERS THEN
    ROLLBACK;
    RETURN 'Erro desconhecido: ' || TO_CHAR(SQLCODE) || ' - ' || SQLERRM;
END Elimina_Turista;
/

-- A procedure registrar_visita (procedures.sql) usa a funcao Regista_Visita, que
-- so existe a partir deste script; recompila-se aqui para ficar VALID.
ALTER PROCEDURE registrar_visita COMPILE;

-- VERIFICAR AS FUNCOES CRIADAS
SELECT object_name, status FROM user_objects WHERE object_type = 'FUNCTION' ORDER BY object_name;

-- TESTES

-- A. Funcoes de consulta: usam-se dentro do SELECT
SELECT ID_reserva,
       Total_Pago_Reserva(ID_reserva) AS total_pago,
       Reserva_Completa(ID_reserva) AS estado
FROM Reserva;
-- esperado: RS-001 = 30000 e Completa; RS-002 = 15000 e Completa

SELECT ID_alojamento, nome_alojamento, Capacidade_Alojamento(ID_alojamento) AS capacidade
FROM Alojamento;
-- esperado: AL-001 = 2 (Casal); AL-002 = 4 (Familiar); AL-003 = 2 (Casal)

SELECT ID_pagamento, valor AS "Em MT", Valor_Em_Dolar(valor, 64) AS "Em US$"
FROM Pagamento;
-- esperado: 25000 -> 390,63; 15000 -> 234,38; 5000 -> 78,13

SELECT Retorna_Info_Destino('DT-001') AS info FROM dual;
SELECT Retorna_Info_Destino('DT-999') AS info FROM dual;
SELECT Lista_Atracoes_Destino('DT-001') AS atracoes FROM dual;
SELECT Lista_Atracoes_Destino('DT-002') AS atracoes FROM dual;
-- esperado: DT-999 -> mensagem de erro tratada; DT-002 -> "nao tem atracoes registadas"

SELECT ID_alojamento, Media_Avaliacao_Alojamento(ID_alojamento) AS media FROM Alojamento;
SELECT ID_operador, Media_Avaliacao_Operador(ID_operador) AS media FROM Operador;
-- esperado: vazio (NULL) enquanto nao houver avaliacoes. Para ver a media a funcionar:
--   UPDATE Reserva_Alojamento SET avaliacao_alojamento = 4.5 WHERE ID_reserva = 'RS-001';
--   SELECT Media_Avaliacao_Alojamento('AL-001') FROM dual;   -- 4,5
--   ROLLBACK;

-- B. Funcoes que gravam: chamam-se dentro de um bloco PL/SQL
-- B.1 Testes que NAO alteram dados (mostram os erros tratados)
BEGIN
  DBMS_OUTPUT.PUT_LINE(Concluir_Pagamento('PG-001'));                  -- ja nao esta pendente
  DBMS_OUTPUT.PUT_LINE(Concluir_Pagamento('PG-999'));                  -- inexistente
  DBMS_OUTPUT.PUT_LINE(Actualiza_Status_Reserva('RS-002', 'INVALIDO')); -- estado invalido
  DBMS_OUTPUT.PUT_LINE(Actualiza_Status_Reserva('RS-999', 'PENDENTE')); -- reserva inexistente
  DBMS_OUTPUT.PUT_LINE(Regista_Visita('DT-999', 'TR-001', SYSDATE));   -- destino inexistente
  DBMS_OUTPUT.PUT_LINE(Regista_Visita('DT-001', 'TR-001', TRUNC(SYSDATE) + 7)); -- visita repetida
  DBMS_OUTPUT.PUT_LINE(Elimina_Turista('TR-001'));                     -- tem reservas
END;
/

-- B.2 Testes que gravam e desfazem (o turista inserido e eliminado a seguir)
BEGIN
  DBMS_OUTPUT.PUT_LINE(Insere_Turista('Pedro Machava', 'Mocambicana'));  -- codigo gerado pela sequencia (TR-005)
  DBMS_OUTPUT.PUT_LINE(Regista_Visita('DT-002', 'TR-001', TRUNC(SYSDATE) + 20));
  DBMS_OUTPUT.PUT_LINE(Elimina_Turista('TR-005'));                       -- troque TR-005 pelo codigo mostrado acima
END;
/
DELETE FROM Turista_Destino
WHERE ID_destino = 'DT-002' AND ID_turista = 'TR-001' AND data_visita = TRUNC(SYSDATE) + 20;
COMMIT;

-- B.3 Concluir um pagamento pendente (acrescenta um pagamento de teste)
INSERT INTO Pagamento (ID_pagamento, ID_reserva, valor, data_pagamento, metodo_pagamento, status_pagamento)
VALUES ('PG-' || LPAD(ID_pagamento.NEXTVAL, 3, '0'), 'RS-002', 3000.00, TRUNC(SYSDATE), 'DINHEIRO', 'PENDENTE');
COMMIT;
SELECT ID_pagamento, status_pagamento, ID_recibo FROM Pagamento ORDER BY ID_pagamento;
BEGIN
  DBMS_OUTPUT.PUT_LINE(Concluir_Pagamento('PG-004'));                    -- o pagamento pendente acabado de inserir
END;
/
SELECT ID_pagamento, status_pagamento, ID_recibo, detalhes_transacao FROM Pagamento ORDER BY ID_pagamento;