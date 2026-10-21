# Gestão de Turismo em Moçambique

Aplicação desktop (Java Swing) para gerir destinos, atrações, alojamentos, transportes, turistas, operadores, reservas e pagamentos, ligada à base de dados Oracle 10g do trabalho prático de Base de Dados I (ISCTEM 2026, Grupo 1).

```
┌──────────────────────┐   HTTP/JSON    ┌──────────────────────┐    JDBC     ┌────────────────────┐
│ turismo-desktop      │ ─────────────▶ │ turismo-api          │ ──────────▶ │ Oracle 10g XE      │
│ Swing + FlatLaf      │  localhost:8083│ REST (JDK HttpServer)│  pool de 4  │ VM CentOS (UTM)    │
│ frontend/            │                │ backend/             │  ligações   │ 192.168.64.4:1521  │
└──────────────────────┘                └──────────────────────┘             └────────────────────┘
```

A aplicação desktop nunca acede diretamente à base de dados: usa a API REST que já existia no projeto (`com.turismo.api`), que foi completada para os 15 módulos.

## Requisitos

| Componente | Versão |
|---|---|
| Java (JDK) | 21 ou superior (testado com 26) |
| Maven | 3.9 ou superior (testado com 3.10) |
| Oracle | 10g Express Edition (driver `ojdbc11` 23.26.3, testado com 10.2.0.1) |
| Sistema | macOS ou Windows (sem APIs específicas de um sistema) |

As restantes dependências (FlatLaf, Ikonli, Gson, HikariCP) são descarregadas pelo Maven. A fonte IBM Plex Sans (licença OFL) está incluída no jar, por isso o aspeto é igual em macOS e Windows.

## Configuração

1. Edite `config/turismo.properties` (não tem palavras-passe):

   | Chave | Exemplo | Descrição |
   |---|---|---|
   | `db.host` | `192.168.64.4` | Endereço da VM com o Oracle |
   | `db.port` | `1521` | Porta do listener |
   | `db.service` / `db.sid` | `XE` | Service name (tem prioridade) ou SID |
   | `db.user` | `yosef` | Utilizador Oracle |
   | `db.poolSize` | `4` | Ligações simultâneas (o 10g XE tem poucos processos) |
   | `api.port` | `8083` | Porta da API |
   | `api.bindAddress` | `127.0.0.1` | Por omissão a API só aceita ligações locais (não tem autenticação) |
   | `api.baseUrl` | `http://localhost:8083/api` | Endereço usado pela aplicação desktop |

2. Indique a palavra-passe da base de dados de uma destas formas (nunca no código nem em ficheiros versionados):
   - variável de ambiente `TURISMO_DB_PASSWORD` (recomendado);
   - ou `config/turismo.local.properties` com `db.password=...` (copie `turismo.local.properties.example`; o ficheiro está no `.gitignore`).

Qualquer chave pode ser sobreposta na linha de comandos, por exemplo `java -Ddb.host=10.0.0.5 -jar ...`.

## Executar

### macOS

```bash
export TURISMO_DB_PASSWORD='...'      # ou já definida no ~/.zshrc
./executar.sh
```

O script compila o projeto se for preciso, arranca a API (se ainda não estiver a correr, com o log em `backend/server.log`) e abre a aplicação. Quando a aplicação fecha, a API que o script arrancou também termina.

### Windows

```bat
set TURISMO_DB_PASSWORD=...
executar.bat
```

A API arranca numa janela minimizada chamada "Turismo API". Feche essa janela para a parar.

### Manualmente (qualquer sistema)

```bash
mvn package                                   # compila e corre os testes
java -jar backend/target/turismo-api.jar      # terminal 1: API
java -jar frontend/target/turismo-desktop.jar # terminal 2: aplicação
```

Ao arrancar, a API mostra o estado da base de dados, incluindo os objetos inválidos e as views em falta.

## Base de dados: scripts e estado atual

Os scripts de `database/` foram corrigidos no próprio ficheiro (cada correção está comentada com `NOTA`). Para os executar há três scripts:

| Script | Quem executa | O que faz |
|---|---|---|
| `conceder_privilegios.sql` | **SYSTEM**, uma vez | `GRANT CREATE VIEW TO yosef`. No Oracle 10g os roles CONNECT e RESOURCE não incluem este privilégio, por isso as 24 views nunca puderam ser criadas (`ORA-01031`). |
| `main.sql` | dono do esquema | Corre todos os scripts pela ordem certa: tabelas, sequências, histórico, índices, triggers, procedures, dados de exemplo, funções, views, cálculo de espaço (`relatorio_calculo2.txt`) e verificação final. |
| `apagarAtualizar.sql` | dono do esquema | **Apaga** todos os objetos do projeto (views, tabelas, histórico, sequências, procedures, funções) e depois corre `main.sql`. |

```
cd database
sqlplus system@//192.168.64.4:1521/XE @conceder_privilegios.sql
sqlplus yosef@//192.168.64.4:1521/XE @main.sql
```

Para criar só as views numa BD que já tem dados (sem apagar nada), depois do `GRANT`: `sqlplus yosef@//192.168.64.4:1521/XE @views.sql`.

Estado em 21-10-2026: na BD de desenvolvimento **todos os triggers, procedures e funções estão válidos** (33 triggers, 16 procedures, 13 funções) e as escritas em todas as tabelas funcionam. **As 39 views ainda não existem**: falta o SYSTEM executar `conceder_privilegios.sql` e depois correr `views.sql`.

**Tudo o que a aplicação mostra é lido de views, nunca das tabelas.** O `views.sql` tem 39 views:

- 1 a 24: relatórios e painel;
- 25 a 39: uma view de listagem por tabela (`v_lista_destinos`, `v_lista_reservas`, …), usada pelas páginas e pelas listas de escolha dos formulários. Usam as funções de `functions.sql` (por exemplo `Total_Pago_Reserva`, `Capacidade_Alojamento`).

As tabelas só são usadas para inserir, alterar e apagar. Enquanto as views não existirem, as páginas mostram "View em falta".

## Testes

```bash
mvn clean test
```

- **Testes unitários** (sem base de dados): validação, tradução de erros Oracle, JSON, configuração, cliente HTTP, definição dos módulos.
- **Testes de integração com o Oracle**: correm se a BD estiver acessível; senão ficam como *skipped*. As escritas correm numa transação desfeita com `ROLLBACK`.
- **Testes de interface**: abrem a janela real; precisam de ambiente gráfico. Os testes ponta a ponta (`LiveApplicationE2ETest`) precisam da API a correr em `api.baseUrl` e gravam capturas em `frontend/target/screenshots/`.

O relatório completo está em [RELATORIO_TESTES.md](RELATORIO_TESTES.md).

## Estrutura

```
pom.xml                    projeto Maven (módulos backend e frontend)
config/                    configuração (sem palavras-passe)
database/                  scripts SQL (corrigidos)
  main.sql                 corre todos os scripts; apagarAtualizar.sql apaga tudo e recria
backend/                   API REST: model, dao, handler, util
frontend/                  aplicação Swing
  ui/theme                 cores (claro/escuro), tipografia, ícones
  ui/components            barra lateral, cartões, estados, gráfico, notificações…
  modules                  ecrã genérico de CRUD + definição dos 15 módulos
  pages                    painel inicial
executar.sh / executar.bat arranque em macOS / Windows
```

Mais detalhes:
- [IMPLEMENTATION_PLAN.md](IMPLEMENTATION_PLAN.md): auditoria, decisões, mapa de ecrãs para views e operações de escrita.
- [PROGRESS_CHECKLIST.md](PROGRESS_CHECKLIST.md): estado de cada módulo.
- [backend/README.md](backend/README.md): referência da API.

## Atalhos de teclado

| Atalho | Ação |
|---|---|
| ⌘N / Ctrl+N | Novo registo no módulo atual |
| ⌘F / Ctrl+F | Pesquisar |
| F5, ⌘R / Ctrl+R | Atualizar |
| Enter / duplo clique na tabela | Editar o registo selecionado |
| Delete | Eliminar o registo selecionado (pede confirmação) |
| Tab / Enter / Espaço na barra lateral | Navegar entre secções |
| Esc | Fechar o formulário |
