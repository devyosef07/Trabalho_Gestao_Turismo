-- O Oracle ja cria sozinho um indice para cada PRIMARY KEY e UNIQUE.
-- Por isso so se criam aqui os indices que ainda faltam: colunas de chave estrangeira que nao sao a primeira coluna de uma chave primaria.
-- ============================================================

DROP INDEX idx_atracao_dest;
DROP INDEX idx_reserva_tur;
DROP INDEX idx_pagamento_res;
DROP INDEX idx_alojamento_dest;
DROP INDEX idx_aloj_tipo;
DROP INDEX idx_turista_pref;      
DROP INDEX idx_res_aloj_res;      
DROP INDEX idx_res_aloj_aloj;
DROP INDEX idx_op_tur_op;
DROP INDEX idx_op_tur_tur;
DROP INDEX idx_tur_dest_dest;     
DROP INDEX idx_tur_dest_tur;
DROP INDEX idx_op_transp_transp;  
DROP INDEX idx_op_aloj_aloj;

-- 1. INDICES NAS CHAVES ESTRANGEIRAS (FKs) MAIS CONSULTADAS

-- Otimiza a busca de todas as atracoes de um determinado destino
CREATE INDEX idx_atracao_dest ON Atracao (ID_destino);

-- Otimiza a busca de todas as reservas efetuadas por um determinado turista
CREATE INDEX idx_reserva_tur ON Reserva (ID_turista);

-- Otimiza o cruzamento de pagamentos com as suas respetivas reservas
CREATE INDEX idx_pagamento_res ON Pagamento (ID_reserva);

-- Otimiza a listagem de alojamentos num determinado destino
CREATE INDEX idx_alojamento_dest ON Alojamento (ID_destino);

-- Otimiza a filtragem de alojamentos pelo seu tipo/categoria
CREATE INDEX idx_aloj_tipo ON Alojamento (tipo_alojamento);


-- 2. INDICES NAS TABELAS ASSOCIATIVAS (N:M)

-- Reserva_Alojamento: PK (ID_reserva, ID_alojamento) -> falta ID_alojamento
CREATE INDEX idx_res_aloj_aloj ON Reserva_Alojamento (ID_alojamento);

-- Operador_Turista: PK (ID_operador, ID_turista) -> falta ID_turista
CREATE INDEX idx_op_tur_tur ON Operador_Turista (ID_turista);

-- Turista_Destino: PK (ID_destino, ID_turista, data_visita) -> falta ID_turista
CREATE INDEX idx_tur_dest_tur ON Turista_Destino (ID_turista);

-- Operador_Transporte: PK (ID_operador, ID_transporte) -> falta ID_transporte
CREATE INDEX idx_op_transp_transp ON Operador_Transporte (ID_transporte);

-- Operador_Alojamento: PK (ID_operador, ID_alojamento) -> falta ID_alojamento
CREATE INDEX idx_op_aloj_aloj ON Operador_Alojamento (ID_alojamento);

Commit;