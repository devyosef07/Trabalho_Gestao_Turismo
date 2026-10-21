-- ============================================================
-- TRABALHO PRÁTICO - BASE DE DADOS I (ISCTEM 2026)
-- GRUPO 1: GESTÃO DE TURISMO EM MOÇAMBIQUE
-- DADOS DE EXEMPLO (Oracle 10g XE)
-- Executar depois de tabelas.sql, Sequences.sql e Triggers.sql
--
-- NOTA: as datas eram fixas (ex.: chegada a 15-10-2026). Os triggers
-- validardatachegada e validarpagamento recusam chegadas no passado e
-- pagamentos no futuro ou com mais de 12 meses, por isso as datas fixas
-- deixavam de ser aceites com o passar do tempo. Passaram a ser relativas
-- à data de execução (TRUNC(SYSDATE) + n dias).
-- ============================================================

-- 1. DESTINOS
INSERT INTO Destino (ID_destino, nome_destino, regiao, capacidade_visitantes)
VALUES ('DT-' || LPAD(ID_destino.NEXTVAL, 3, '0'), 'Arquipelago de Bazaruto', 'Inhambane', 500);

INSERT INTO Destino (ID_destino, nome_destino, regiao, capacidade_visitantes) 
VALUES ('DT-' || LPAD(ID_destino.NEXTVAL, 3, '0'), 'Reserva de Maputo', 'Maputo', 300);

-- 2. ATRACOES
INSERT INTO Atracao (ID_atracao, ID_destino, tipo_atracao, descricao, hora_abertura, hora_fecho) 
VALUES ('AT-' || LPAD(ID_atracao.NEXTVAL, 3, '0'), 'DT-001', 'Atracoes Naturais', 'Mergulho com tubaroes-baleia', '08:00', '16:00');

INSERT INTO Atracao (ID_atracao, ID_destino, tipo_atracao, descricao, hora_abertura, hora_fecho) 
VALUES ('AT-' || LPAD(ID_atracao.NEXTVAL, 3, '0'), 'DT-001', 'Atracoes Naturais', 'Passeio pelas dunas e corais', '07:00', '17:00');

-- 3. TURISTAS
INSERT INTO Turista (ID_turista, nome_turista, nacionalidade) 
VALUES ('TR-' || LPAD(ID_turista.NEXTVAL, 3, '0'), 'Carlos Silva', 'Mocambicana');

INSERT INTO Turista (ID_turista, nome_turista, nacionalidade) 
VALUES ('TR-' || LPAD(ID_turista.NEXTVAL, 3, '0'), 'John Smith', 'Sul-Africana');

INSERT INTO Turista (ID_turista, nome_turista, nacionalidade) 
VALUES ('TR-' || LPAD(ID_turista.NEXTVAL, 3, '0'), 'Ana Ferreira', 'Portuguesa');

INSERT INTO Turista (ID_turista, nome_turista, nacionalidade) 
VALUES ('TR-' || LPAD(ID_turista.NEXTVAL, 3, '0'), 'Emma Brown', 'Britanica');

-- 4. RESERVAS
INSERT INTO Reserva (ID_reserva, ID_turista, data_chegada, data_partida, status_reserva) 
VALUES ('RS-' || LPAD(ID_reserva.NEXTVAL, 3, '0'), 'TR-001', TRUNC(SYSDATE) + 7, TRUNC(SYSDATE) + 12, 'CONFIRMADA');

INSERT INTO Reserva (ID_reserva, ID_turista, data_chegada, data_partida, status_reserva) 
VALUES ('RS-' || LPAD(ID_reserva.NEXTVAL, 3, '0'), 'TR-002', TRUNC(SYSDATE) + 14, TRUNC(SYSDATE) + 18, 'PENDENTE');

-- 5. PAGAMENTOS
INSERT INTO Pagamento (ID_pagamento, ID_reserva, valor, data_pagamento, metodo_pagamento, status_pagamento, ID_recibo) 
VALUES ('PG-' || LPAD(ID_pagamento.NEXTVAL, 3, '0'), 'RS-001', 25000.00, TRUNC(SYSDATE) - 5, 'TRANSFERENCIA', 'CONCLUIDO', 'RC-001');

INSERT INTO Pagamento (ID_pagamento, ID_reserva, valor, data_pagamento, metodo_pagamento, status_pagamento, ID_recibo) 
VALUES ('PG-' || LPAD(ID_pagamento.NEXTVAL, 3, '0'), 'RS-002', 15000.00, TRUNC(SYSDATE) - 5, 'CARTAO', 'CONCLUIDO', 'RC-002');

INSERT INTO Pagamento (ID_pagamento, ID_reserva, valor, data_pagamento, metodo_pagamento, status_pagamento, ID_recibo) 
VALUES ('PG-' || LPAD(ID_pagamento.NEXTVAL, 3, '0'), 'RS-001', 5000.00, TRUNC(SYSDATE) - 30, 'DINHEIRO', 'CONCLUIDO', 'RC-003');

-- 6. TIPOS DE ALOJAMENTO (a capacidade depende do tipo)
INSERT INTO Tipo_Alojamento (tipo_alojamento, cap_alojamento) VALUES ('Casal', 2);
INSERT INTO Tipo_Alojamento (tipo_alojamento, cap_alojamento) VALUES ('Familiar', 4);

-- 7. ALOJAMENTOS
INSERT INTO Alojamento (ID_alojamento, ID_destino, tipo_alojamento, nome_alojamento, class_alojamento)
VALUES ('AL-' || LPAD(ID_alojamento.NEXTVAL, 3, '0'), 'DT-001', 'Casal', 'Hotel Bazaruto Paradise', 4.5);

INSERT INTO Alojamento (ID_alojamento, ID_destino, tipo_alojamento, nome_alojamento, class_alojamento)
VALUES ('AL-' || LPAD(ID_alojamento.NEXTVAL, 3, '0'), 'DT-002', 'Familiar', 'Pousada Reserva Maputo', 4.0);

INSERT INTO Alojamento (ID_alojamento, ID_destino, tipo_alojamento, nome_alojamento, class_alojamento)
VALUES ('AL-' || LPAD(ID_alojamento.NEXTVAL, 3, '0'), 'DT-001', 'Casal', 'Pousada do Sol', 3.0);

-- 8. TRANSPORTES (dois Autocarros com capacidades diferentes)
INSERT INTO Transporte (ID_transporte, tipo_transporte, cap_transporte, disponibilidade)
VALUES ('TP-' || LPAD(ID_transporte.NEXTVAL, 3, '0'), 'Autocarro', 50, 'Sim');

INSERT INTO Transporte (ID_transporte, tipo_transporte, cap_transporte, disponibilidade)
VALUES ('TP-' || LPAD(ID_transporte.NEXTVAL, 3, '0'), 'Autocarro', 40, 'Sim');

-- 9. OPERADORES
INSERT INTO Operador (ID_operador, nome_operador, especialidade, class_operador)
VALUES ('OP-' || LPAD(ID_operador.NEXTVAL, 3, '0'), 'Bazaruto Tours', 'Mergulho', 4.8);

INSERT INTO Operador (ID_operador, nome_operador, especialidade, class_operador)
VALUES ('OP-' || LPAD(ID_operador.NEXTVAL, 3, '0'), 'Safari Moz', 'Safari', 4.5);

-- 10. PREFERENCIAS DE VIAGEM
INSERT INTO Turista_Preferencia (ID_turista, preferencia) VALUES ('TR-001', 'Praias');
INSERT INTO Turista_Preferencia (ID_turista, preferencia) VALUES ('TR-002', 'Aventura');

-- 11. RESERVA_ALOJAMENTO (alojamento escolhido em cada reserva; ainda sem avaliacao)
INSERT INTO Reserva_Alojamento (ID_reserva, ID_alojamento, avaliacao_alojamento) VALUES ('RS-001', 'AL-001', NULL);
INSERT INTO Reserva_Alojamento (ID_reserva, ID_alojamento, avaliacao_alojamento) VALUES ('RS-002', 'AL-002', NULL);

-- 12. OPERADOR_TRANSPORTE
INSERT INTO Operador_Transporte (ID_operador, ID_transporte) VALUES ('OP-001', 'TP-001');
INSERT INTO Operador_Transporte (ID_operador, ID_transporte) VALUES ('OP-002', 'TP-002');

-- 13. OPERADOR_ALOJAMENTO (parcerias)
INSERT INTO Operador_Alojamento (ID_operador, ID_alojamento) VALUES ('OP-001', 'AL-001');
INSERT INTO Operador_Alojamento (ID_operador, ID_alojamento) VALUES ('OP-002', 'AL-002');

-- 14. OPERADOR_TURISTA (guias; ainda sem avaliacao)
INSERT INTO Operador_Turista (ID_operador, ID_turista, avaliacao_operador) VALUES ('OP-001', 'TR-001', NULL);
INSERT INTO Operador_Turista (ID_operador, ID_turista, avaliacao_operador) VALUES ('OP-002', 'TR-002', NULL);

-- 15. TURISTA_DESTINO (visitas planeadas, no dia de chegada de cada reserva)
INSERT INTO Turista_Destino (ID_destino, ID_turista, data_visita) VALUES ('DT-001', 'TR-001', TRUNC(SYSDATE) + 7);
INSERT INTO Turista_Destino (ID_destino, ID_turista, data_visita) VALUES ('DT-002', 'TR-002', TRUNC(SYSDATE) + 14);

COMMIT;