-- ============================================================
-- TRABALHO PRÁTICO - BASE DE DADOS I (ISCTEM 2026)
-- GRUPO 1: GESTÃO DE TURISMO EM MOÇAMBIQUE
-- contas.sql: contas de utilizador e sessões da aplicação (Oracle 10g XE)
--
-- Acrescenta ao esquema, SEM apagar nada do que já existe:
--   * sequência ID_conta;
--   * tabelas Conta_Utilizador e Sessao_Utilizador;
--   * views v_conta_credenciais e v_sessao_ativa (a API só lê destas views);
--   * procedures registrar_conta, registar_acesso_conta, iniciar_sessao e
--     terminar_sessao (a API só escreve através delas, como no resto do projeto).
--
-- Segurança:
--   * a palavra-passe NUNCA chega à base de dados: a API guarda apenas o hash
--     PBKDF2-HMAC-SHA256 com sal aleatório (formato pbkdf2-sha256$iteracoes$sal$hash);
--   * das sessões guarda-se só o SHA-256 do token; o token em si fica apenas
--     na aplicação desktop do utilizador;
--   * 5 tentativas falhadas seguidas bloqueiam a conta durante 5 minutos.
--
-- Pode correr-se sozinho numa BD que já tem o resto do projeto:
--   cd database
--   sqlplus yosef@//192.168.64.4:1521/XE @contas.sql
-- Se as tabelas já existirem, os CREATE TABLE/SEQUENCE dão ORA-00955 (normal);
-- as views e as procedures são substituídas (CREATE OR REPLACE).
-- ============================================================

SET SERVEROUTPUT ON
SET DEFINE OFF

-- ------------------------------------------------------------
-- 1. Sequência e tabelas
-- ------------------------------------------------------------
CREATE SEQUENCE ID_conta
INCREMENT BY 1
START WITH 1
NOMAXVALUE
NOCYCLE;

create table Conta_Utilizador(
	ID_conta number(8) not null,
	nome_utilizador varchar2(40) not null,
	nome_completo varchar2(100) not null,
	data_nascimento date not null,
	hash_palavra_passe varchar2(200) not null,
	data_criacao date default sysdate not null,
	ultimo_acesso date,
	tentativas_falhadas number(2) default 0 not null,
	bloqueada_ate date,
	constraint pk_conta_utilizador primary key (ID_conta),
	constraint uq_conta_nome_utilizador unique (nome_utilizador),
	constraint ck_conta_nome_minusculas check (nome_utilizador = lower(nome_utilizador)),
	constraint ck_conta_tentativas check (tentativas_falhadas >= 0)
);

create table Sessao_Utilizador(
	hash_token varchar2(64) not null,
	ID_conta number(8) not null references Conta_Utilizador(ID_conta) on delete cascade,
	data_inicio date default sysdate not null,
	data_expiracao date not null,
	data_fim date,
	constraint pk_sessao_utilizador primary key (hash_token)
);

create index idx_sessao_conta on Sessao_Utilizador(ID_conta);

-- ------------------------------------------------------------
-- 2. Views (leitura)
-- ------------------------------------------------------------

-- Dados necessários para validar um início de sessão.
create or replace view v_conta_credenciais as
select c.ID_conta,
       c.nome_utilizador,
       c.hash_palavra_passe,
       c.tentativas_falhadas,
       c.bloqueada_ate,
       -- minutos que faltam para o desbloqueio (0 = conta não bloqueada)
       case when c.bloqueada_ate > sysdate then ceil((c.bloqueada_ate - sysdate) * 1440) else 0 end as minutos_bloqueio
from Conta_Utilizador c;

-- Sessões em vigor (não terminadas e não expiradas) com os dados públicos da conta.
create or replace view v_sessao_ativa as
select s.hash_token,
       c.ID_conta,
       c.nome_utilizador,
       c.nome_completo,
       c.data_nascimento,
       c.data_criacao,
       c.ultimo_acesso,
       s.data_inicio,
       s.data_expiracao
from Sessao_Utilizador s
join Conta_Utilizador c on c.ID_conta = s.ID_conta
where s.data_fim is null
  and s.data_expiracao > sysdate;

-- ------------------------------------------------------------
-- 3. Procedures (escrita), no estilo de procedures.sql:
--    COMMIT e mensagem de sucesso; em erro, ROLLBACK e "Erro ao ...: ".
-- ------------------------------------------------------------

--1. Criar uma conta. O hash da palavra-passe é calculado pela API.
CREATE OR REPLACE PROCEDURE registrar_conta (
    novo_nome_utilizador IN Conta_Utilizador.nome_utilizador%TYPE,
    novo_nome_completo IN Conta_Utilizador.nome_completo%TYPE,
    novo_data_nascimento IN Conta_Utilizador.data_nascimento%TYPE,
    novo_hash_palavra_passe IN Conta_Utilizador.hash_palavra_passe%TYPE
) AS
    v_num NUMBER;
BEGIN
    IF novo_data_nascimento > TRUNC(SYSDATE) OR novo_data_nascimento < DATE '1900-01-01' THEN
        DBMS_OUTPUT.PUT_LINE('Erro ao registar conta: a data de nascimento e invalida.');
        RETURN;
    END IF;
    SELECT ID_conta.NEXTVAL INTO v_num FROM dual;
    INSERT INTO Conta_Utilizador (ID_conta, nome_utilizador, nome_completo, data_nascimento, hash_palavra_passe)
    VALUES (v_num, LOWER(novo_nome_utilizador), novo_nome_completo, TRUNC(novo_data_nascimento), novo_hash_palavra_passe);
    COMMIT;
    DBMS_OUTPUT.PUT_LINE('Conta ' || v_num || ' (' || LOWER(novo_nome_utilizador) || ') registada com sucesso!');
EXCEPTION
    WHEN OTHERS THEN
        ROLLBACK;
        DBMS_OUTPUT.PUT_LINE('Erro ao registar conta: ' || SQLERRM);
END;
/

--2. Registar o resultado de uma tentativa de início de sessão ('S' ou 'N').
--   Um sucesso limpa as falhas; à 5.ª falha seguida a conta fica bloqueada 5 minutos.
CREATE OR REPLACE PROCEDURE registar_acesso_conta (
    novo_id_conta IN Conta_Utilizador.ID_conta%TYPE,
    novo_sucesso IN CHAR
) AS
BEGIN
    IF novo_sucesso = 'S' THEN
        UPDATE Conta_Utilizador
        SET tentativas_falhadas = 0, bloqueada_ate = NULL, ultimo_acesso = SYSDATE
        WHERE ID_conta = novo_id_conta;
    ELSE
        UPDATE Conta_Utilizador
        SET tentativas_falhadas = CASE WHEN tentativas_falhadas + 1 >= 5 THEN 0 ELSE tentativas_falhadas + 1 END,
            bloqueada_ate = CASE WHEN tentativas_falhadas + 1 >= 5 THEN SYSDATE + 5 / 1440 ELSE bloqueada_ate END
        WHERE ID_conta = novo_id_conta;
    END IF;

    IF SQL%FOUND THEN
        COMMIT;
        DBMS_OUTPUT.PUT_LINE('Acesso da conta ' || novo_id_conta || ' registado com sucesso!');
    ELSE
        DBMS_OUTPUT.PUT_LINE('Conta ' || novo_id_conta || ' nao foi encontrada.');
    END IF;
EXCEPTION
    WHEN OTHERS THEN
        ROLLBACK;
        DBMS_OUTPUT.PUT_LINE('Erro ao registar acesso da conta: ' || SQLERRM);
END;
/

--3. Abrir uma sessão (guarda só o SHA-256 do token) e limpar as sessões
--   da conta que acabaram há mais de 30 dias.
CREATE OR REPLACE PROCEDURE iniciar_sessao (
    novo_hash_token IN Sessao_Utilizador.hash_token%TYPE,
    novo_id_conta IN Sessao_Utilizador.ID_conta%TYPE,
    novo_minutos IN NUMBER
) AS
BEGIN
    DELETE FROM Sessao_Utilizador
    WHERE ID_conta = novo_id_conta
      AND NVL(data_fim, data_expiracao) < SYSDATE - 30;
    INSERT INTO Sessao_Utilizador (hash_token, ID_conta, data_inicio, data_expiracao)
    VALUES (novo_hash_token, novo_id_conta, SYSDATE, SYSDATE + novo_minutos / 1440);
    COMMIT;
    DBMS_OUTPUT.PUT_LINE('Sessao iniciada para a conta ' || novo_id_conta || ' com sucesso!');
EXCEPTION
    WHEN OTHERS THEN
        ROLLBACK;
        DBMS_OUTPUT.PUT_LINE('Erro ao iniciar sessao: ' || SQLERRM);
END;
/

--4. Terminar uma sessão (a conta e os seus dados mantêm-se).
CREATE OR REPLACE PROCEDURE terminar_sessao (
    novo_hash_token IN Sessao_Utilizador.hash_token%TYPE
) AS
BEGIN
    UPDATE Sessao_Utilizador
    SET data_fim = SYSDATE
    WHERE hash_token = novo_hash_token AND data_fim IS NULL;

    IF SQL%FOUND THEN
        COMMIT;
        DBMS_OUTPUT.PUT_LINE('Sessao terminada com sucesso!');
    ELSE
        DBMS_OUTPUT.PUT_LINE('Sessao nao foi encontrada.');
    END IF;
EXCEPTION
    WHEN OTHERS THEN
        ROLLBACK;
        DBMS_OUTPUT.PUT_LINE('Erro ao terminar sessao: ' || SQLERRM);
END;
/

-- Verificação: 2 tabelas, 2 views e 4 procedures, todas VALID
SELECT object_type, object_name, status FROM user_objects
WHERE object_name IN ('CONTA_UTILIZADOR', 'SESSAO_UTILIZADOR', 'V_CONTA_CREDENCIAIS', 'V_SESSAO_ATIVA',
                      'REGISTRAR_CONTA', 'REGISTAR_ACESSO_CONTA', 'INICIAR_SESSAO', 'TERMINAR_SESSAO')
ORDER BY object_type, object_name;
