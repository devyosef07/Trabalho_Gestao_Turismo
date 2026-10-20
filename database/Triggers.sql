--TRIGGERS que mostram quem apagou ou alterou algo nas tabelas e quando

CREATE OR REPLACE TRIGGER auditoriaAlojamento
AFTER UPTADE OR DELETE ON alojamento
FOR EACH ROW
BEGIN
    IF DELETING THEN
         INSERT INTO auditoria_alojamento VALUES (:OLD.ID_alojamento, :OLD.nome_alojamento, 'DELETE', SYSDATE, USER);
    ELSIF UPDATING THEN
         INSERT INTO auditoria_alojamento VALUES (:OLD.ID_alojamento, :OLD.nome_alojamento, 'UPDATE', SYSDATE, USER);
    END IF;
END;
/

CREATE OR REPLACE TRIGGER auditoriaDestino
AFTER UPTADE OR DELETE ON destino
FOR EACH ROW
BEGIN
    IF DELETING THEN
         INSERT INTO auditoria_destino VALUES (:OLD.ID_destino, :OLD.nome_destino, 'DELETE', SYSDATE, USER);
    ELSIF UPDATING THEN
         INSERT INTO auditoria_destino VALUES (:OLD.ID_destino, :OLD.nome_destino, 'UPDATE', SYSDATE, USER);
    END IF;
END;
/

CREATE OR REPLACE TRIGGER auditoriaAtracao
AFTER UPTADE OR DELETE ON atracao
FOR EACH ROW
BEGIN
    IF DELETING THEN
         INSERT INTO auditoria_atracao VALUES (:OLD.ID_atracao, :OLD.tipo_atracao, 'DELETE', SYSDATE, USER);
    ELSIF UPDATING THEN
         INSERT INTO auditoria_atracao VALUES (:OLD.ID_atracao, :OLD.nome_atracao, 'UPDATE', SYSDATE, USER);
    END IF;
END;
/

CREATE OR REPLACE TRIGGER auditoriaTurista
AFTER UPTADE OR DELETE ON turista
FOR EACH ROW
BEGIN
    IF DELETING THEN
         INSERT INTO auditoria_turista VALUES (:OLD.ID_turista, :OLD.nome_turista, 'DELETE', SYSDATE, USER);
    ELSIF UPDATING THEN
         INSERT INTO auditoria_turista VALUES (:OLD.ID_turista, :OLD.nome_turista, 'UPDATE', SYSDATE, USER);
    END IF;
END;
/

CREATE OR REPLACE TRIGGER auditoriaTipoAlojamento
AFTER UPTADE OR DELETE ON tipo_alojamento
FOR EACH ROW
BEGIN
    IF DELETING THEN
        INSERT INTO auditoria_tipoAlojamento VALUES (:OLD.tipo_alojamento, :OLD.cap_alojamento, 'DELETE', SYSDATE, USER);
    ELSIF UPDATING THEN
         INSERT INTO auditoria_tipoAlojamento VALUES (:OLD.tipo_alojamento, :OLD.cap_alojamento, 'UPDATE', SYSDATE, USER);
    END IF;
END;
/

CREATE OR REPLACE TRIGGER auditoriaReserva
AFTER UPTADE OR DELETE ON reserva
FOR EACH ROW
BEGIN
    IF DELETING THEN
        INSERT INTO auditoria_reserva VALUES (:OLD.ID_reserva, :OLD.nome_reserva, 'DELETE', SYSDATE, USER);
    ELSIF UPDATING THEN
         INSERT INTO auditoria_reserva VALUES (:OLD.ID_reserva, :OLD.ID_turista, 'UPDATE', SYSDATE, USER);
    END IF;
END;
/

CREATE OR REPLACE TRIGGER auditoriaOperador
AFTER UPTADE OR DELETE ON operador
FOR EACH ROW
BEGIN
    IF DELETING THEN
         INSERT INTO auditoria_operador VALUES (:OLD.ID_operador, :OLD.nome_operador, 'DELETE', SYSDATE, USER);
    ELSIF UPDATING THEN
         INSERT INTO auditoria_operador VALUES (:OLD.ID_operador, :OLD.nome_operador, 'UPDATE', SYSDATE, USER);
    END IF;
END;
/

CREATE OR REPLACE TRIGGER Transporte
AFTER UPTADE OR DELETE ON transporte
FOR EACH ROW
BEGIN
    IF DELETING THEN
         INSERT INTO auditoria_transporte VALUES (:OLD.ID_transporte, :OLD.nome_transporte, 'DELETE', SYSDATE, USER);
    ELSIF UPDATING THEN
         INSERT INTO auditoria_transporte VALUES (:OLD.ID_transporte, :OLD.nome_transporte, 'UPDATE', SYSDATE, USER);
    END IF;
END;
/

CREATE OR REPLACE TRIGGER auditoriaPagamento
AFTER UPTADE OR DELETE ON pagamento
FOR EACH ROW
BEGIN
    IF DELETING THEN
        INSERT INTO auditoria_pagamento VALUES (:OLD.ID_pagamento, :OLD.metodo_pagamento, 'DELETE', SYSDATE, USER);
    ELSIF UPDATING THEN
         INSERT INTO auditoria_pagamento VALUES (:OLD.ID_pagamento, :OLD.metodo_pagamento, 'UPDATE', SYSDATE, USER);
    END IF;
END;
/

CREATE OR REPLACE TRIGGER auditoriaTuristaDestino
AFTER UPTADE OR DELETE ON turista_destino
FOR EACH ROW
BEGIN
    IF DELETING THEN
         INSERT INTO auditoria_turistaDestino VALUES (:OLD.ID_destino, :OLD.ID_turista, 'DELETE', SYSDATE, USER);
    ELSIF UPDATING THEN
         INSERT INTO auditoria_turistaDestino VALUES (:OLD.ID_turista, :OLD.ID_destino, 'UPDATE', SYSDATE, USER);
    END IF;
END;
/

CREATE OR REPLACE TRIGGER auditoriaOperadorAlojamento
AFTER UPTADE OR DELETE ON operador_alojamento
FOR EACH ROW
BEGIN
    IF DELETING THEN
         INSERT INTO auditoria_operadorAlojamento VALUES (:OLD.ID_operador, :OLD.ID_alojamento, 'DELETE', SYSDATE, USER);
    ELSIF UPDATING THEN
         INSERT INTO auditoria_operadorAlojamento VALUES (:OLD.ID_operador, :OLD.ID_alojamento, 'UPDATE', SYSDATE, USER);
    END IF;
END;
/

CREATE OR REPLACE TRIGGER auditoriaOperadorTransporte
AFTER UPTADE OR DELETE ON operador_transporte
FOR EACH ROW
BEGIN
    IF DELETING THEN
         INSERT INTO auditoria_operadorTransporte VALUES (:OLD.ID_operador, :OLD.ID_transporte, 'DELETE', SYSDATE, USER);
    ELSIF UPDATING THEN
         INSERT INTO auditoria_operadorTransporte VALUES (:OLD.ID_operador, :OLD.ID_transporte, 'UPDATE', SYSDATE, USER);
    END IF;
END;
/

CREATE OR REPLACE TRIGGER auditoriaOperadorTurista
AFTER UPTADE OR DELETE ON operador_turista
FOR EACH ROW
BEGIN
    IF DELETING THEN
         INSERT INTO auditoria_operadorTurista VALUES (:OLD.ID_operador, :OLD.ID_turista, 'DELETE', SYSDATE, USER);
    ELSIF UPDATING THEN
         INSERT INTO auditoria_operadorTurista VALUES (:OLD.ID_operador, :OLD.ID_turista, 'UPDATE', SYSDATE, USER);
    END IF;
END;
/

CREATE OR REPLACE TRIGGER auditoriaReservaAlojamento
AFTER UPTADE OR DELETE ON reserva_alojamento
FOR EACH ROW
BEGIN
    IF DELETING THEN
         INSERT INTO auditoria_reservaAlojamento VALUES (:OLD.ID_reserva, :OLD.ID_alojamento, 'DELETE', SYSDATE, USER);
    ELSIF UPDATING THEN
         INSERT INTO auditoria_reservaAlojamento VALUES (:OLD.ID_reserva, :OLD.ID_alojamento, 'UPDATE', SYSDATE, USER);
    END IF;
END;
/

CREATE OR REPLACE TRIGGER auditoriaTuristaPreferencia
AFTER UPTADE OR DELETE ON turista_preferencia
FOR EACH ROW
BEGIN
    IF DELETING THEN
         INSERT INTO auditoria_turistaPreferencia VALUES (:OLD.ID_turista, :OLD.preferencia, 'DELETE', SYSDATE, USER);
    ELSIF UPDATING THEN
         INSERT INTO auditoria_turistaPreferencia VALUES (:OLD.ID_turista, :OLD.preferencia, 'UPDATE', SYSDATE, USER);
    END IF;
END;
/

--TRIGGERS para manter a integridade, garantem que ao apagar algo tudo relacionado a isso seja eliminado tambem

CREATE OR REPLACE TRIGGER deleteTurista
AFTER DELETE ON turista
FOR EACH ROW
BEGIN
    DELETE FROM reserva WHERE ID_turista  = :OLD.ID_turista;
    DELETE FROM operador_turista WHERE ID_turista  = :OLD.ID_turista;
    DELETE FROM turista_preferencia WHERE ID_turista  = :OLD.ID_turista;
    DELETE FROM turista_destino WHERE ID_turista  = :OLD.ID_turista;
END;
/

CREATE OR REPLACE TRIGGER deleteDestino
AFTER DELETE ON destino
FOR EACH ROW
BEGIN
    DELETE FROM atracao WHERE ID_destino  = :OLD.ID_destino;
    DELETE FROM alojamento WHERE ID_destino  = :OLD.ID_destino;
    DELETE FROM turista_destino WHERE ID_destino  = :OLD.ID_destino;
END;
/

CREATE OR REPLACE TRIGGER deleteReserva
AFTER DELETE ON reserva
FOR EACH ROW
BEGIN
    DELETE FROM turista WHERE ID_reserva  = :OLD.ID_reserva;    
    DELETE FROM pagamento WHERE ID_reserva  = :OLD.ID_reserva;
    DELETE FROM reserva_alojamento WHERE ID_reserva  = :OLD.ID_reserva;
END;
/

CREATE OR REPLACE TRIGGER deleteOperador
AFTER DELETE ON operador
FOR EACH ROW
BEGIN
    DELETE FROM operador_turista WHERE ID_operador  = :OLD.ID_operador;
    DELETE FROM operador_transporte WHERE ID_operador  = :OLD.ID_operador;
    DELETE FROM operador_alojamento WHERE ID_operador  = :OLD.ID_operador;
END;
/

CREATE OR REPLACE TRIGGER deleteAlojamento
AFTER DELETE ON alojamento
FOR EACH ROW
BEGIN
    DELETE FROM operador_alojamento WHERE ID_alojamento  = :OLD.ID_alojamento;
    DELETE FROM reserva_alojamento WHERE ID_alojamento  = :OLD.ID_alojamento;
END;
/

CREATE OR REPLACE TRIGGER deleteTransporte
AFTER DELETE ON transporte
FOR EACH ROW
BEGIN
    DELETE FROM operador_transporte WHERE ID_transporte  = :OLD.ID_transporte;
END;
/

--TRIGGERS para validacao

CREATE OR REPLACE TRIGGER validarcaptransporte
BEFORE INSERT OR UPDATE ON transporte
FOR EACH ROW
BEGIN
    IF :NEW.cap_transporte <= 0 THEN
        RAISE_APPLICATION_ERROR(-20001, 'ERRO: Capacidade de transporte invalida!');
    END IF;
END;
/

CREATE OR REPLACE TRIGGER validarcapvisitantes
BEFORE INSERT OR UPDATE ON destino
FOR EACH ROW
BEGIN
    IF :NEW.capacidade_visitantes <= 0 THEN
        RAISE_APPLICATION_ERROR(-20002, 'ERRO: Capacidade de visitantes invalida!');
    END IF;
END;
/

CREATE OR REPLACE TRIGGER validarcapalojamento
BEFORE INSERT OR UPDATE ON alojamento
FOR EACH ROW
BEGIN
    IF :NEW.cap_alojamento <= 0 THEN
        RAISE_APPLICATION_ERROR(-20003, 'ERRO: Capacidade de alojamento invalida!');
    END IF;
END;
/

CREATE OR REPLACE TRIGGER validaravaoperador
BEFORE INSERT OR UPDATE ON operador_turista
FOR EACH ROW
BEGIN
    IF :NEW.avaliacao_operador IS NOT BETWEEN 0.0 AND 5.9 THEN
        RAISE_APPLICATION_ERROR(-20004, 'ERRO: Avaliacao do operador invalida!');
    END IF;    
END;
/

CREATE OR REPLACE TRIGGER validaravalojamento
BEFORE INSERT OR UPDATE ON reserva_alojamento
FOR EACH ROW
BEGIN
    IF :NEW.avaliacao_operador IS NOT BETWEEN 0.0 AND 5.9 THEN
        RAISE_APPLICATION_ERROR(-20005, 'ERRO: Avaliacao do alojamento invalida!');
    END IF;    
END;
/

CREATE OR REPLACE TRIGGER validarhorabertura
BEFORE INSERT OR UPDATE ON atracao
FOR EACH ROW
BEGIN
    IF NOT REGEXP(:NEW.hora_abertura, '^([01][0-9]|2[0-3]):[0-5[0-9]$') THEN
        RAISE_APPLICATION_ERROR(-20006, 'ERRO: Hora invalida!');
    END IF;   
END;
/

CREATE OR REPLACE TRIGGER validarhorafecho
BEFORE INSERT OR UPDATE ON atracao
FOR EACH ROW
BEGIN
    IF NOT REGEXP(:NEW.hora_fecho, '^([01][0-9]|2[0-3]):[0-5[0-9]$') THEN
        RAISE_APPLICATION_ERROR(-20007, 'ERRO: Hora invalida!');
    END IF;  
    IF :NEW.hora_fecho <= :NEW.hora_abertura THEN    
        RAISE_APPLICATION_ERROR(-20007, 'ERRO: Hora de fecho invalida!');
    END IF;  
END;
/

CREATE OR REPLACE TRIGGER validarhoraberturaesp
BEFORE INSERT OR UPDATE ON atracao
FOR EACH ROW
BEGIN
    IF NOT REGEXP(:NEW.hora_abertura_especial, '^([01][0-9]|2[0-3]):[0-5[0-9]$') THEN
        RAISE_APPLICATION_ERROR(-20006, 'ERRO: Hora invalida!');
    END IF;   
END;
/

CREATE OR REPLACE TRIGGER validarhorafechoesp
BEFORE INSERT OR UPDATE ON atracao
FOR EACH ROW
BEGIN
    IF NOT REGEXP(:NEW.hora_fecho_especial, '^([01][0-9]|2[0-3]):[0-5[0-9]$') THEN
        RAISE_APPLICATION_ERROR(-20007, 'ERRO: Hora invalida!');
    END IF;  
    IF :NEW.hora_fecho_especial <= :NEW.hora_abertura_especial THEN    
        RAISE_APPLICATION_ERROR(-20007, 'ERRO: Hora de fecho especial invalida!');
    END IF;  
END;
/

CREATE OR REPLACE TRIGGER validardatachegada
BEFORE INSERT OR UPDATE ON reserva
FOR EACH ROW
BEGIN
    IF TRUNC (:NEW.data_chegada) < TRUNC (SYSDATE) THEN
        RAISE_APPLICATION_ERROR(-20008, 'ERRO: Data de chegada invalida!');
END;
/

CREATE OR REPLACE TRIGGER validardatapartida
BEFORE INSERT OR UPDATE ON reserva
FOR EACH ROW
BEGIN
    IF :NEW.data_partida <= :NEW.data_chegada THEN
        RAISE_APPLICATION_ERROR(-20009, 'ERRO: Data de partida invalida!');    
END;
/

CREATE OR REPLACE TRIGGER validarpagamento
BEFORE INSERT OR UPDATE ON pagamento
FOR EACH ROW
BEGIN
    IF TRUNC (:NEW.data_pagamento) > TRUNC (SYSDATE) THEN
        RAISE_APPLICATION_ERROR(-20010, 'ERRO: Data de pagamento invalida!');
    END IF;
    IF TRUNC (:NEW.data_chegada) < SELECT( data_entrada FROM reserva WHERE ID = :NEW.ID_reserva) THEN
        RAISE_APPLICATION_ERROR(-20010, 'ERRO: Data de pagamento invalida!');
    END IF;
    IF MONTHS_BETWEEN (SYSDATE, :NEW.data_pagamento) >12 THEN
        RAISE_APPLICATION_ERROR(-20010, 'ERRO: Data de chegada invalida!');
    END IF;
END;
/
