-- ============================================================
-- TRABALHO PRÁTICO - BASE DE DADOS I (ISCTEM 2026)
-- GRUPO 1: GESTÃO DE TURISMO EM MOÇAMBIQUE
-- SCRIPT DE VIEWS (Oracle 10g XE)
-- Executar depois de tabelas.sql e de functions.sql (as views de listagem
-- 25 a 39 usam funções de functions.sql)
--
-- Views 1 a 24: consultas e verificações (relatórios e painel da aplicação).
-- Views 25 a 39: uma view de listagem por tabela. A aplicação lê todos os
-- dados que mostra destas views e nunca diretamente das tabelas; as tabelas
-- só são usadas para inserir, alterar e apagar.
--
-- Criar views exige o privilégio CREATE VIEW (ver conceder_privilegios.sql).
-- ============================================================

SET LINESIZE 200
SET PAGESIZE 50

-- Datas no formato DD-MM-YYYY
alter session set nls_date_format = 'DD-MM-YYYY';


-- 1. Mostra cada atração com o destino e a região onde fica e os seus horários.
create or replace view v_atracoes_destino as
select d.nome_destino,
       d.regiao,
       a.ID_atracao,
       a.tipo_atracao,
       a.descricao,
       a.hora_abertura,
       a.hora_fecho,
       a.hora_abertura_especial,
       a.hora_fecho_especial,
       a.detalhes_horario_especial
from Destino d
join Atracao a on a.ID_destino = d.ID_destino
with read only;

-- 2. Conta quantas atrações tem cada destino (os destinos sem atrações aparecem com 0).
create or replace view v_total_atracoes_destino as
select d.ID_destino,
       d.nome_destino,
       count(a.ID_atracao) as total_atracoes
from Destino d
left join Atracao a on a.ID_destino = d.ID_destino
group by d.ID_destino, d.nome_destino
with read only;

-- 3. Conta quantos alojamentos tem cada destino (os destinos sem alojamentos aparecem com 0).
create or replace view v_total_alojamentos_destino as
select d.ID_destino,
       d.nome_destino,
       count(a.ID_alojamento) as total_alojamentos
from Destino d
left join Alojamento a on a.ID_destino = d.ID_destino
group by d.ID_destino, d.nome_destino
with read only;

-- 4. Mostra cada alojamento com o seu tipo, a capacidade desse tipo e o destino onde fica.
create or replace view v_alojamentos_detalhe as
select a.ID_alojamento,
       a.nome_alojamento,
       a.tipo_alojamento,
       t.cap_alojamento,
       a.class_alojamento,
       d.nome_destino,
       d.regiao
from Alojamento a
join Tipo_Alojamento t on t.tipo_alojamento = a.tipo_alojamento
join Destino d         on d.ID_destino = a.ID_destino
with read only;

-- 5. Mostra a média das avaliações que cada alojamento recebeu nas reservas
--    (os alojamentos ainda sem avaliações aparecem com a média vazia).
create or replace view v_media_avaliacao_alojamento as
select a.ID_alojamento,
       a.nome_alojamento,
       a.class_alojamento,
       avg(ra.avaliacao_alojamento) as media_avaliacoes
from Alojamento a
left join Reserva_Alojamento ra on ra.ID_alojamento = a.ID_alojamento
group by a.ID_alojamento, a.nome_alojamento, a.class_alojamento
with read only;

-- 6. Mostra as reservas com o nome do turista e o número de noites.
create or replace view v_reservas_detalhe as
select r.ID_reserva,
       t.nome_turista,
       t.nacionalidade,
       r.data_chegada,
       r.data_partida,
       r.data_partida - r.data_chegada as noites,
       r.status_reserva
from Reserva r
join Turista t on t.ID_turista = r.ID_turista
with read only;

-- 7. Mostra só as reservas que ainda estão em curso (PENDENTE ou CONFIRMADA).
create or replace view v_reservas_ativas as
select r.ID_reserva,
       t.nome_turista,
       r.data_chegada,
       r.data_partida,
       r.status_reserva
from Reserva r
join Turista t on t.ID_turista = r.ID_turista
where r.status_reserva in ('PENDENTE', 'CONFIRMADA')
with read only;

-- 8. Mostra que alojamentos foram escolhidos em cada reserva.
create or replace view v_reservas_alojamento as
select r.ID_reserva,
       t.nome_turista,
       a.nome_alojamento,
       a.tipo_alojamento,
       ra.avaliacao_alojamento
from Reserva_Alojamento ra
join Reserva r    on r.ID_reserva = ra.ID_reserva
join Turista t    on t.ID_turista = r.ID_turista
join Alojamento a on a.ID_alojamento = ra.ID_alojamento
with read only;

-- 9. Mostra quanto já foi pago em cada reserva (só pagamentos concluídos).
--    As reservas ainda sem pagamentos concluídos aparecem com o total vazio.
create or replace view v_total_pago_reserva as
select r.ID_reserva,
       t.nome_turista,
       sum(p.valor) as total_pago
from Reserva r
join Turista t   on t.ID_turista = r.ID_turista
left join Pagamento p on p.ID_reserva = r.ID_reserva
                     and p.status_pagamento = 'CONCLUIDO'
group by r.ID_reserva, t.nome_turista
with read only;

-- 10. Lista os pagamentos que já têm recibo.
create or replace view v_recibos as
select p.ID_recibo,
       p.ID_pagamento,
       t.nome_turista,
       p.valor,
       p.data_pagamento,
       p.metodo_pagamento
from Pagamento p
join Reserva r on r.ID_reserva = p.ID_reserva
join Turista t on t.ID_turista = r.ID_turista
where p.ID_recibo is not null
with read only;

-- 11. Mostra quanto se recebeu em cada mês (só pagamentos concluídos).
--     A coluna mes_ordem serve para ordenar os meses por ordem certa.
create or replace view v_receita_mensal as
select to_char(data_pagamento, 'MM-YYYY') as mes,
       to_char(data_pagamento, 'YYYY-MM') as mes_ordem,
       sum(valor) as receita
from Pagamento
where status_pagamento = 'CONCLUIDO'
group by to_char(data_pagamento, 'MM-YYYY'), to_char(data_pagamento, 'YYYY-MM')
with read only;

-- 12. Mostra quanto cada nacionalidade pagou (só pagamentos concluídos).
create or replace view v_receita_nacionalidade as
select t.nacionalidade,
       sum(p.valor) as receita
from Pagamento p
join Reserva r on r.ID_reserva = p.ID_reserva
join Turista t on t.ID_turista = r.ID_turista
where p.status_pagamento = 'CONCLUIDO'
group by t.nacionalidade
with read only;

-- 13. Mostra os destinos que cada turista visitou e quando.
create or replace view v_turista_destinos as
select t.nome_turista,
       t.nacionalidade,
       d.nome_destino,
       d.regiao,
       td.data_visita
from Turista_Destino td
join Turista t on t.ID_turista = td.ID_turista
join Destino d on d.ID_destino = td.ID_destino
with read only;

-- 14. Mostra as preferências de viagem de cada turista.
create or replace view v_turista_preferencias as
select t.ID_turista,
       t.nome_turista,
       tp.preferencia
from Turista t
join Turista_Preferencia tp on tp.ID_turista = t.ID_turista
with read only;

-- 15. Mostra quantos turistas cada operador acompanhou e a média das avaliações que recebeu
--     (os operadores sem turistas aparecem com 0).
create or replace view v_avaliacao_operador as
select o.ID_operador,
       o.nome_operador,
       count(ot.ID_turista)         as total_turistas,
       avg(ot.avaliacao_operador)   as media_avaliacao
from Operador o
left join Operador_Turista ot on ot.ID_operador = o.ID_operador
group by o.ID_operador, o.nome_operador
with read only;

-- 16. Mostra os alojamentos que cada operador pode vender.
create or replace view v_operadores_alojamentos as
select o.nome_operador,
       a.nome_alojamento,
       a.tipo_alojamento
from Operador_Alojamento oa
join Operador o   on o.ID_operador = oa.ID_operador
join Alojamento a on a.ID_alojamento = oa.ID_alojamento
with read only;

-- 17. Mostra os transportes de cada operador, com a capacidade e se estão disponíveis.
create or replace view v_operadores_transportes as
select o.nome_operador,
       tr.tipo_transporte,
       tr.cap_transporte,
       tr.disponibilidade
from Operador_Transporte ot
join Operador o    on o.ID_operador = ot.ID_operador
join Transporte tr on tr.ID_transporte = ot.ID_transporte
with read only;

-- 18. Mostra só os transportes que estão disponíveis.
create or replace view v_transportes_disponiveis as
select ID_transporte,
       tipo_transporte,
       cap_transporte
from Transporte
where disponibilidade = 'Sim'
with read only;

-- 19. Verificação: encontra pagamentos com data no futuro (não é permitido).
--     Se estiver tudo certo, não devolve nenhuma linha.
create or replace view v_aud_pagamento_data_futura as
select ID_pagamento,
       ID_reserva,
       valor,
       data_pagamento
from Pagamento
where data_pagamento > sysdate
with read only;

-- 20. Verificação: encontra pagamentos que têm recibo mas não estão concluídos.
--     Se estiver tudo certo, não devolve nenhuma linha.
create or replace view v_aud_recibo_sem_concluido as
select ID_pagamento,
       ID_recibo,
       status_pagamento
from Pagamento
where ID_recibo is not null
  and status_pagamento <> 'CONCLUIDO'
with read only;


-- 21. Verificação: mostra as reservas que ainda não têm nenhum alojamento.
--     Se estiver tudo certo, não devolve nenhuma linha.
create or replace view v_reservas_sem_alojamento as
select ID_reserva,
       ID_turista,
       data_chegada,
       data_partida,
       status_reserva
from Reserva
where ID_reserva not in (select ID_reserva from Reserva_Alojamento)
with read only;

-- 22. Verificação: mostra as reservas que ainda não têm nenhum pagamento.
--     Se estiver tudo certo, não devolve nenhuma linha.
create or replace view v_reservas_sem_pagamento as
select ID_reserva,
       ID_turista,
       data_chegada,
       data_partida,
       status_reserva
from Reserva
where ID_reserva not in (select ID_reserva from Pagamento)
with read only;

-- 23. Mostra os alojamentos com classificação acima da média de todos os alojamentos.
create or replace view v_alojamentos_acima_media as
select ID_alojamento,
       nome_alojamento,
       class_alojamento
from Alojamento
where class_alojamento > (select avg(class_alojamento) from Alojamento)
with read only;

-- 24. Mostra os turistas que ainda não fizeram nenhuma reserva.
create or replace view v_turistas_sem_reserva as
select ID_turista,
       nome_turista,
       nacionalidade
from Turista
where ID_turista not in (select ID_turista from Reserva)
with read only;


-- ============================================================
-- VIEWS DE LISTAGEM (uma por tabela, usadas pelas páginas da aplicação)
-- ============================================================

-- 25. Destinos, com o número de atrações, alojamentos e visitas de cada um.
create or replace view v_lista_destinos as
select d.ID_destino,
       d.nome_destino,
       d.regiao,
       d.capacidade_visitantes,
       (select count(*) from Atracao a where a.ID_destino = d.ID_destino)          as total_atracoes,
       (select count(*) from Alojamento a where a.ID_destino = d.ID_destino)       as total_alojamentos,
       (select count(*) from Turista_Destino td where td.ID_destino = d.ID_destino) as total_visitas
from Destino d
with read only;

-- 26. Atrações, com o nome do destino.
create or replace view v_lista_atracoes as
select a.ID_atracao,
       a.tipo_atracao,
       a.descricao,
       a.hora_abertura,
       a.hora_fecho,
       a.hora_abertura_especial,
       a.hora_fecho_especial,
       a.detalhes_horario_especial,
       a.ID_destino,
       d.nome_destino
from Atracao a
join Destino d on d.ID_destino = a.ID_destino
with read only;

-- 27. Alojamentos, com o destino, a capacidade (função Capacidade_Alojamento)
--     e a média das avaliações (função Media_Avaliacao_Alojamento).
create or replace view v_lista_alojamentos as
select a.ID_alojamento,
       a.nome_alojamento,
       a.class_alojamento,
       a.tipo_alojamento,
       a.ID_destino,
       d.nome_destino,
       Capacidade_Alojamento(a.ID_alojamento)      as cap_alojamento,
       Media_Avaliacao_Alojamento(a.ID_alojamento) as media_avaliacoes
from Alojamento a
join Destino d on d.ID_destino = a.ID_destino
with read only;

-- 28. Tipos de alojamento, com o número de alojamentos de cada tipo.
create or replace view v_lista_tipos_alojamento as
select t.tipo_alojamento,
       t.cap_alojamento,
       (select count(*) from Alojamento a where a.tipo_alojamento = t.tipo_alojamento) as total_alojamentos
from Tipo_Alojamento t
with read only;

-- 29. Transportes, com o número de operadores de cada um.
create or replace view v_lista_transportes as
select t.ID_transporte,
       t.tipo_transporte,
       t.cap_transporte,
       t.disponibilidade,
       (select count(*) from Operador_Transporte ot where ot.ID_transporte = t.ID_transporte) as total_operadores
from Transporte t
with read only;

-- 30. Turistas, com o número de reservas de cada um.
create or replace view v_lista_turistas as
select t.ID_turista,
       t.nome_turista,
       t.nacionalidade,
       (select count(*) from Reserva r where r.ID_turista = t.ID_turista) as total_reservas
from Turista t
with read only;

-- 31. Preferências de viagem, com o nome do turista.
create or replace view v_lista_preferencias as
select tp.ID_turista,
       tp.preferencia,
       t.nome_turista
from Turista_Preferencia tp
join Turista t on t.ID_turista = tp.ID_turista
with read only;

-- 32. Operadores, com o número de turistas acompanhados e a média das
--     avaliações (função Media_Avaliacao_Operador).
create or replace view v_lista_operadores as
select o.ID_operador,
       o.nome_operador,
       o.especialidade,
       o.class_operador,
       (select count(*) from Operador_Turista ot where ot.ID_operador = o.ID_operador) as total_turistas,
       Media_Avaliacao_Operador(o.ID_operador) as media_avaliacao
from Operador o
with read only;

-- 33. Reservas, com o turista, as noites, o total pago (função
--     Total_Pago_Reserva) e se está completa (função Reserva_Completa).
create or replace view v_lista_reservas as
select r.ID_reserva,
       r.data_chegada,
       r.data_partida,
       r.status_reserva,
       r.ID_turista,
       t.nome_turista,
       r.data_partida - r.data_chegada  as noites,
       Total_Pago_Reserva(r.ID_reserva) as total_pago,
       Reserva_Completa(r.ID_reserva)   as estado_completude
from Reserva r
join Turista t on t.ID_turista = r.ID_turista
with read only;

-- 34. Pagamentos, com o nome do turista da reserva.
create or replace view v_lista_pagamentos as
select p.ID_pagamento,
       p.valor,
       p.data_pagamento,
       p.metodo_pagamento,
       p.status_pagamento,
       p.ID_recibo,
       p.detalhes_transacao,
       p.ID_reserva,
       t.nome_turista
from Pagamento p
join Reserva r on r.ID_reserva = p.ID_reserva
join Turista t on t.ID_turista = r.ID_turista
with read only;

-- 35. Visitas de turistas a destinos, com os nomes.
create or replace view v_lista_visitas as
select td.ID_destino,
       td.ID_turista,
       td.data_visita,
       d.nome_destino,
       t.nome_turista
from Turista_Destino td
join Destino d on d.ID_destino = td.ID_destino
join Turista t on t.ID_turista = td.ID_turista
with read only;

-- 36. Alojamentos de cada reserva, com a avaliação e os nomes.
create or replace view v_lista_reserva_alojamentos as
select ra.ID_reserva,
       ra.ID_alojamento,
       ra.avaliacao_alojamento,
       a.nome_alojamento,
       t.nome_turista
from Reserva_Alojamento ra
join Alojamento a on a.ID_alojamento = ra.ID_alojamento
join Reserva r    on r.ID_reserva = ra.ID_reserva
join Turista t    on t.ID_turista = r.ID_turista
with read only;

-- 37. Operadores que acompanham cada turista, com a avaliação e os nomes.
create or replace view v_lista_operador_turistas as
select ot.ID_operador,
       ot.ID_turista,
       ot.avaliacao_operador,
       o.nome_operador,
       t.nome_turista
from Operador_Turista ot
join Operador o on o.ID_operador = ot.ID_operador
join Turista t  on t.ID_turista = ot.ID_turista
with read only;

-- 38. Transportes de cada operador, com os nomes.
create or replace view v_lista_operador_transportes as
select ot.ID_operador, 
       ot.ID_transporte,
       o.nome_operador,
       tr.tipo_transporte
from Operador_Transporte ot
join Operador o    on o.ID_operador = ot.ID_operador
join Transporte tr on tr.ID_transporte = ot.ID_transporte
with read only;

-- 39. Alojamentos de cada operador, com os nomes.
create or replace view v_lista_operador_alojamentos as
select oa.ID_operador,
       oa.ID_alojamento,
       o.nome_operador,
       a.nome_alojamento
from Operador_Alojamento oa
join Operador o   on o.ID_operador = oa.ID_operador
join Alojamento a on a.ID_alojamento = oa.ID_alojamento
with read only;


-- Testes: lista as views criadas e mostra o conteúdo de cada uma
select view_name from user_views order by view_name;

select * from v_atracoes_destino;
select * from v_total_atracoes_destino;
select * from v_total_alojamentos_destino;
select * from v_alojamentos_detalhe;
select * from v_media_avaliacao_alojamento;
select * from v_reservas_detalhe;
select * from v_reservas_ativas;
select * from v_reservas_alojamento;
select * from v_total_pago_reserva;
select * from v_recibos;
select mes, receita from v_receita_mensal order by mes_ordem;
select * from v_receita_nacionalidade;
select * from v_turista_destinos;
select * from v_turista_preferencias;
select * from v_avaliacao_operador;
select * from v_operadores_alojamentos;
select * from v_operadores_transportes;
select * from v_transportes_disponiveis;
select * from v_aud_pagamento_data_futura;
select * from v_aud_recibo_sem_concluido;
select * from v_reservas_sem_alojamento;
select * from v_reservas_sem_pagamento;
select * from v_alojamentos_acima_media;
select * from v_turistas_sem_reserva;
select * from v_lista_destinos;
select * from v_lista_atracoes;
select * from v_lista_alojamentos;
select * from v_lista_tipos_alojamento;
select * from v_lista_transportes;
select * from v_lista_turistas;
select * from v_lista_preferencias;
select * from v_lista_operadores;
select * from v_lista_reservas;
select * from v_lista_pagamentos;
select * from v_lista_visitas;
select * from v_lista_reserva_alojamentos;
select * from v_lista_operador_turistas;
select * from v_lista_operador_transportes;
select * from v_lista_operador_alojamentos;
