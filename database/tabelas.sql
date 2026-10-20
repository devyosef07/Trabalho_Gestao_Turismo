-- ============================================================
-- TRABALHO PRÁTICO - BASE DE DADOS I (ISCTEM 2026)
-- GRUPO 1: GESTÃO DE TURISMO EM MOÇAMBIQUE
-- SCRIPT DDL: ELIMINAÇÃO E CRIAÇÃO DO ESQUEMA FÍSICO (CORRIGIDO)
-- ============================================================

--====================================================================================
-- APAGAR AS TABELAS EXISTENTES (para permitir re-execução limpa do script)
--====================================================================================
drop table Turista_Destino cascade constraints;
drop table Operador_Turista cascade constraints;
drop table Operador_Alojamento cascade constraints;
drop table Operador_Transporte cascade constraints;
drop table Reserva_Alojamento cascade constraints;
drop table Pagamento cascade constraints;
drop table Transporte cascade constraints;
drop table Alojamento cascade constraints;
drop table Operador cascade constraints;
drop table Reserva cascade constraints;
drop table Turista_Preferencia cascade constraints;
drop table Turista cascade constraints;
drop table Atracao cascade constraints;
drop table Destino cascade constraints;
drop table Tipo_Alojamento cascade constraints;

--==================================================
-- CRIAÇÃO DAS TABELAS
--==================================================

--1. Tabela Tipo_Alojamento
create table Tipo_Alojamento(
	tipo_alojamento varchar2(11) not null,
	cap_alojamento number(2) not null check (cap_alojamento > 0),
	primary key (tipo_alojamento)
);

--2. Tabela Destino
create table Destino(
	ID_destino varchar2(6) not null,
	nome_destino varchar2(80) not null,
	regiao varchar2(50) not null check(regiao in (
		'Maputo', 'Gaza', 'Inhambane', 'Sofala', 'Tete', 'Manica', 'Zambezia',
		'Nampula', 'Niassa', 'Cabo Delgado')),
	capacidade_visitantes number(10) not null check(capacidade_visitantes > 0),
	primary key(ID_destino)
);

--3. Tabela Atracao
create table Atracao(
	ID_atracao varchar2(6) not null,
	tipo_atracao varchar2(30) not null check (tipo_atracao in (
		'Atracoes Naturais', 'Atracoes Culturais', 'Praia', 
		'Parque', 'Montanha', 'Patrimonio historico', 
		'Evento ou Festival', 'Entretenimento urbano')),
	descricao varchar2(200),
	hora_abertura char(5) not null,
	hora_fecho char(5) not null,
	hora_abertura_especial char(5),
	hora_fecho_especial char(5),
	detalhes_horario_especial varchar2(200),
	ID_destino varchar2(6) not null references Destino(ID_destino),
	primary key (ID_atracao),
	check ((hora_abertura_especial is null and hora_fecho_especial is null
		and detalhes_horario_especial is null) 
		or (hora_abertura_especial is not null and hora_fecho_especial is not null
		and detalhes_horario_especial is not null
		and hora_fecho_especial > hora_abertura_especial))
);

--4. Tabela Turista
create table Turista(
	ID_turista varchar2(6) not null,
	nome_turista varchar2(80) not null,
	nacionalidade varchar2(50) not null,
	primary key (ID_turista)
);

--5. Tabela Turista_Preferencia
create table Turista_Preferencia(
	ID_turista varchar2(6) not null references Turista(ID_turista),
	preferencia varchar2(200) not null check(preferencia in (
		'Praias', 'Aventura', 'Natureza', 'Cultura', 'Gastronomia')),
	primary key (ID_turista, preferencia)
);

--6. Tabela Reserva
create table Reserva(
	ID_reserva varchar2(6) not null,
	ID_turista varchar2(6) not null references Turista(ID_turista),
	data_chegada date not null,
	data_partida date not null,
	status_reserva varchar2(10) default 'PENDENTE' not null check(status_reserva in (
		'PENDENTE', 'CONFIRMADA', 'CANCELADA', 'CONCLUIDA')),
	primary key (ID_reserva),
	check (data_partida >= data_chegada)
);

--7. Tabela Operador
create table Operador(
	ID_operador varchar2(6) not null,
	nome_operador varchar2(25) not null,
	especialidade varchar2(15) not null,
	class_operador number(2,1) check (class_operador between 0.0 and 5.0),
	primary key (ID_operador)
);

--8. Tabela Alojamento
create table Alojamento(
	ID_alojamento varchar2(6) not null,
	nome_alojamento varchar2(50) not null,
	tipo_alojamento varchar2(11) not null references Tipo_Alojamento(tipo_alojamento),
	class_alojamento number(2,1) check (class_alojamento between 0.0 and 5.0),
	ID_destino varchar2(6) not null references Destino(ID_destino),
	primary key (ID_alojamento)
);

--9. Tabela Transporte
create table Transporte(
	ID_transporte varchar2(6) not null,
	tipo_transporte varchar2(15) not null,
	cap_transporte number(9) not null check (cap_transporte > 0),
	disponibilidade char(3) not null check (disponibilidade in ('Sim', 'Nao')),
	primary key(ID_transporte)
);

--10. Tabela Pagamento
create table Pagamento(
	ID_pagamento varchar2(6) not null,
	valor number(10,2) not null check (valor > 0),
	data_pagamento date not null,
	metodo_pagamento varchar2(15) not null check(metodo_pagamento in (
		'DINHEIRO', 'CARTAO', 'TRANSFERENCIA')),
	status_pagamento varchar2(10) default 'PENDENTE' not null check (status_pagamento in (
		'PENDENTE', 'CONCLUIDO', 'FALHADO')),
	ID_recibo varchar2(6) unique,
	detalhes_transacao varchar2(200),
	ID_reserva varchar2(6) not null references Reserva(ID_reserva),
	primary key (ID_pagamento)
);

--11. Tabela Reserva_Alojamento
create table Reserva_Alojamento (
	ID_reserva varchar2(6) not null references Reserva(ID_reserva),
	ID_alojamento varchar2(6) not null references Alojamento(ID_alojamento),
	avaliacao_alojamento number(2,1) check (avaliacao_alojamento between 0.0 and 5.0),
	primary key (ID_reserva, ID_alojamento)
);

--12. Tabela Operador_Transporte
create table Operador_Transporte(
	ID_operador varchar2(6) not null references Operador(ID_operador),
	ID_transporte varchar2(6) not null references Transporte(ID_transporte),
	primary key (ID_operador, ID_transporte)
);

--13. Tabela Operador_Alojamento
create table Operador_Alojamento(
	ID_operador varchar2(6) not null references Operador(ID_operador),
	ID_alojamento varchar2(6) not null references Alojamento(ID_alojamento),
	primary key (ID_operador, ID_alojamento)
);

--14. Tabela Operador_Turista
create table Operador_Turista(
	ID_operador varchar2(6) not null references Operador(ID_operador),
	ID_turista varchar2(6) not null references Turista(ID_turista),
	avaliacao_operador number(2,1) check (avaliacao_operador between 0.0 and 5.0),
	primary key (ID_operador, ID_turista)
);

--15. Tabela Turista_Destino
create table Turista_Destino(
	ID_destino varchar2(6) not null references Destino(ID_destino),
	ID_turista varchar2(6) not null references Turista(ID_turista),
	data_visita date not null,
	primary key (ID_destino, ID_turista, data_visita)
);
