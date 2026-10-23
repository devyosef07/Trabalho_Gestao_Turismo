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

Antes de ver qualquer dado é preciso **iniciar sessão**. As contas (nome completo, data de nascimento e palavra-passe) ficam no Oracle, a palavra-passe só como hash PBKDF2, e a API recusa pedidos sem uma sessão válida. Ver [Contas e sessão](#contas-e-sessão) e o documento [documents/AUTENTICACAO_E_INTERFACE.md](documents/AUTENTICACAO_E_INTERFACE.md).

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
   | `api.bindAddress` | `127.0.0.1` | Por omissão a API só aceita ligações locais |
   | `api.requireAuth` | `true` | Os endpoints de dados exigem sessão iniciada (`false` só para testes manuais com `curl`) |
   | `api.baseUrl` | `http://localhost:8083/api` | Endereço usado pela aplicação desktop |

2. Indique a palavra-passe da base de dados de uma destas formas (nunca no código nem em ficheiros versionados):
   - variável de ambiente `TURISMO_DB_PASSWORD` (recomendado);
   - ou `config/turismo.local.properties` com `db.password=...` (copie `turismo.local.properties.example`; o ficheiro está no `.gitignore`).

Qualquer chave pode ser sobreposta na linha de comandos, por exemplo `java -Ddb.host=10.0.0.5 -jar ...`.

## Executar

### macOS

```bash
export TURISMO_DB_PASSWORD='...'      # ou já definida no ~/.zshrc
./scripts/executar.sh
```

O script compila o projeto se for preciso, arranca a API (se ainda não estiver a correr, com o log em `backend/server.log`) e abre a aplicação. Quando a aplicação fecha, a API que o script arrancou também termina.

A aplicação abre no ecrã de boas-vindas. Na primeira vez escolha **Criar conta** (ver [Contas e sessão](#contas-e-sessão)).

### Windows

```bat
set TURISMO_DB_PASSWORD=...
scripts\executar.bat
```

A API arranca numa janela minimizada chamada "Turismo API". Feche essa janela para a parar.

### Scripts (pasta `scripts/`)

Todos podem ser chamados a partir de qualquer pasta: mudam primeiro para a raiz do projeto.

| Script | O que faz |
|---|---|
| `scripts/executar.sh` / `scripts/executar.bat` | Uso normal: compila se faltar algum `.jar`, arranca a API (se não estiver a correr) e abre a aplicação |
| `scripts/compilar.sh` | Compila os dois módulos sem correr os testes (`mvn -q -DskipTests package`) |
| `scripts/api.sh` | Compila o backend e arranca só a API (`--sem-compilar` para usar o `.jar` já compilado). Se a porta estiver ocupada, para o servidor anterior |
| `scripts/desktop.sh` | Abre só a aplicação desktop (a API tem de estar a correr) |

### Manualmente (qualquer sistema)

```bash
mvn package                                   # compila e corre os testes
java -jar backend/target/turismo-api.jar      # terminal 1: API
java -jar frontend/target/turismo-desktop.jar # terminal 2: aplicação
```

Ao arrancar, a API mostra o estado da base de dados, incluindo os objetos inválidos e as views em falta.

## Interface

- **Identidade azul**: azul principal `#2563EB` sobre azul profundo `#0F172A`, com símbolo próprio (sol sobre ondas), em tema claro e escuro (botão "Tema escuro" na barra lateral).
- **Ecrãs de autenticação**: boas-vindas, início de sessão e registo sobre um fundo expressivo (gradiente, luzes difusas, anéis e uma rota tracejada). Em janelas largas a marca fica à esquerda e o formulário numa folha clara à direita; em janelas estreitas fica só um cartão centrado.
- **Área de trabalho**: barra lateral em gradiente com a conta no rodapé (avatar com iniciais e ponto de sessão ativa), cartões com sombra suave, fundo discreto nas páginas e saudação com o primeiro nome no painel.
- **Animações**: entrada dos ecrãs e dos cartões, transições entre ecrãs, notificações, erros de campo, força da palavra-passe e confirmação de conta criada. São curtas (140 a 520 ms), param sozinhas e só mudam a opacidade e a posição da pintura, por isso os controlos continuam utilizáveis.
- **Movimento reduzido**: no macOS respeita a preferência "Reduzir movimento"; em qualquer sistema pode forçar-se com `java -Dturismo.reduzirMovimento=true -jar frontend/target/turismo-desktop.jar`.
- **Desempenho**: os fundos são desenhados em código e guardados numa imagem (só são refeitos quando muda o tamanho ou o tema); não há imagens, vídeos nem bibliotecas novas.

Detalhes, ficheiros alterados e testes: [documents/AUTENTICACAO_E_INTERFACE.md](documents/AUTENTICACAO_E_INTERFACE.md).

## Contas e sessão

1. **Primeira vez:** no ecrã de boas-vindas escolha **Criar conta** e preencha nome completo, data de nascimento, palavra-passe e confirmação. A palavra-passe precisa de pelo menos 8 caracteres, com letras e números; é preciso ter pelo menos 13 anos.
2. No fim, a aplicação mostra o **nome de utilizador** gerado a partir do nome (ex.: "Maria José Cossa" → `maria.cossa`; se já existir, `maria.cossa2`). É com ele que se inicia sessão.
3. **Iniciar sessão:** nome de utilizador e palavra-passe. Com "Manter sessão iniciada neste computador", a aplicação volta a entrar sozinha durante 7 dias; sem essa opção, a sessão dura até 12 horas e não fica guardada.
4. **A minha conta** e **Terminar sessão** estão no cartão da conta, no fundo da barra lateral. Terminar a sessão fecha a área de trabalho, apaga o acesso guardado e revoga a sessão na API; a conta e os dados mantêm-se.

Segurança, em resumo:
- a palavra-passe nunca é guardada: só o hash PBKDF2-HMAC-SHA256 (600 000 iterações, sal aleatório), calculado na API;
- 5 tentativas falhadas seguidas bloqueiam a conta durante 5 minutos, e a mensagem de erro nunca revela se o nome de utilizador existe;
- o token de sessão é aleatório (256 bits); a BD guarda só o seu SHA-256;
- limitação: com "manter sessão", o token fica nas preferências do utilizador do sistema operativo (macOS: `~/Library/Preferences`; Windows: registo), que não são cifradas. Quem tiver acesso à conta do computador pode usá-lo até expirar ou até a sessão ser terminada.

Requer os objetos de `database/contas.sql` (já incluído em `main.sql`). Numa BD que já tem o resto do projeto, basta correr esse script: não apaga nada.

## Base de dados: scripts e estado atual

Os scripts de `database/` foram corrigidos no próprio ficheiro (cada correção está comentada com `NOTA`). Há um único script de procedures, `procedures.sql`, com 43 procedures (16 do enunciado e 27 de manutenção) e um índice no topo que explica cada uma. Para executar os scripts há três ficheiros:

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

Para criar só as views numa BD que já tem dados (sem apagar nada), depois do `GRANT`: `sqlplus yosef@//192.168.64.4:1521/XE @views.sql`. Para acrescentar as contas de utilizador sem apagar nada: `sqlplus yosef@//192.168.64.4:1521/XE @contas.sql`.

Estado em 22-10-2026: na BD de desenvolvimento **todos os objetos estão válidos**: 41 views, 33 triggers, 47 procedures e 13 funções.

`contas.sql` (22-10-2026) acrescenta as tabelas `Conta_Utilizador` e `Sessao_Utilizador`, a sequência `ID_conta`, as views `v_conta_credenciais` e `v_sessao_ativa` e as procedures `registrar_conta`, `registar_acesso_conta`, `iniciar_sessao` e `terminar_sessao`. Segue as mesmas regras do resto do projeto: a API lê só das views e escreve só pelas procedures.

**Tudo o que a aplicação mostra é lido de views, nunca das tabelas.** O `views.sql` tem 39 views (mais 2 em `contas.sql`):

- 1 a 24: relatórios e painel;
- 25 a 39: uma view de listagem por tabela (`v_lista_destinos`, `v_lista_reservas`, …), usada pelas páginas e pelas listas de escolha dos formulários. Usam as funções de `functions.sql` (por exemplo `Total_Pago_Reserva`, `Capacidade_Alojamento`).

**Tudo o que a aplicação grava passa por procedures ou funções**: o Java nunca executa `INSERT`, `UPDATE` ou `DELETE` diretamente nas tabelas. Se uma view não existir, a página respetiva mostra "View em falta".

## Testes

```bash
mvn clean test
```

- **Testes unitários** (sem base de dados): validação, tradução de erros Oracle, JSON, configuração, cliente HTTP, definição dos módulos.
- **Testes de integração com o Oracle**: correm se a BD estiver acessível; senão ficam como *skipped*. Como as procedures fazem `COMMIT`, só testam caminhos de erro (registo inexistente, regra violada), que não gravam nada.
- **Testes de autenticação**: hash e regras das contas, endpoints `/api/auth` e filtro de sessão com um DAO em memória (`AuthHandlerHttpTest`, `PasswordHasherTest`); fluxo completo na janela real contra uma API falsa (`AuthFlowUiTest`: boas-vindas, registo, validação, login certo e errado, sessão guardada, sessão expirada, terminar sessão, janela estreita).
- **Testes de interface**: abrem a janela real; precisam de ambiente gráfico. Os testes ponta a ponta (`LiveApplicationE2ETest`) precisam da API a correr em `api.baseUrl` e de uma conta indicada em `TURISMO_E2E_UTILIZADOR` e `TURISMO_E2E_PALAVRA_PASSE` (variáveis de ambiente); gravam capturas em `frontend/target/screenshots/`.

O relatório de testes de 20-10-2026 está em [documents/RELATORIO_TESTES.md](documents/RELATORIO_TESTES.md).

## Estrutura

```
pom.xml                    projeto Maven (módulos backend e frontend)
README.md                  este ficheiro
config/                    configuração (sem palavras-passe)
scripts/                   executar.sh / executar.bat, compilar.sh, api.sh, desktop.sh
database/                  scripts SQL (corrigidos)
  main.sql                 corre todos os scripts; apagarAtualizar.sql apaga tudo e recria
  procedures.sql           as 43 procedures, com índice
  contas.sql               contas de utilizador e sessões (2 tabelas, 2 views, 4 procedures)
backend/                   API REST: model, dao, handler, util, auth (main: TurismoApiServer)
  auth                     hash das palavras-passe, tokens de sessão, filtro que exige sessão
frontend/                  aplicação Swing (main: MainApplication)
  auth                     boas-vindas, início de sessão, registo, serviço de sessão
  ui/theme                 cores (identidade azul, claro/escuro), tipografia, ícones
  ui/motion                animações (fade e deslocamento) com suporte a movimento reduzido
  ui/components            barra lateral, cartões, campos, fundos, avatar, estados, gráfico, notificações…
  modules                  ecrã genérico de CRUD + definição dos 15 módulos
  pages                    painel inicial e "A minha conta"
documents/                 manual técnico (README.md), enunciado, relatórios e diagramas
```

O projeto tem só dois programas (`main`): a API (`backend/.../TurismoApiServer.java`) e a aplicação desktop (`frontend/.../MainApplication.java`).

Mais detalhes:
- [documents/README.md](documents/README.md): manual técnico completo, com diagramas do funcionamento.
- [backend/README.md](backend/README.md): referência da API (inclui `/api/auth`).
- [documents/AUTENTICACAO_E_INTERFACE.md](documents/AUTENTICACAO_E_INTERFACE.md): redesign, autenticação, animações, testes e limitações (22-10-2026).
- [documents/IMPLEMENTATION_PLAN.md](documents/IMPLEMENTATION_PLAN.md) e [documents/PROGRESS_CHECKLIST.md](documents/PROGRESS_CHECKLIST.md): auditoria inicial e estado dos módulos (20-10-2026).

## Atalhos de teclado

| Atalho | Ação |
|---|---|
| ⌘N / Ctrl+N | Novo registo no módulo atual |
| ⌘F / Ctrl+F | Pesquisar |
| F5, ⌘R / Ctrl+R | Atualizar |
| Enter / duplo clique na tabela | Editar o registo selecionado |
| Delete | Eliminar o registo selecionado (pede confirmação) |
| Tab / Enter / Espaço na barra lateral | Navegar entre secções |
| Enter / Espaço no cartão da conta | Abrir o menu da conta (A minha conta, Terminar sessão) |
| Enter nos ecrãs de início de sessão e registo | Passar ao campo seguinte ou enviar |
| Esc | Fechar o formulário |
