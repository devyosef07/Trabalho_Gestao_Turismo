# Plano de implementação e auditoria

> **Documento histórico (20-10-2026).** Desde 21-10-2026 os scripts de execução estão em `scripts/` (ex.: `./scripts/executar.sh`), a BD tem 0 objetos inválidos e as 39 views, e todas as escritas passam por procedures. O estado atual está no [manual técnico](README.md).

Documento das fases 1 e 2 (inspeção e plano), atualizado com o que foi feito. Data da auditoria: 20-10-2026.

## 1. Auditoria do projeto existente

### O que existia

| Área | Situação encontrada |
|---|---|
| Build | Sem `pom.xml` nos módulos (só um `demo/pom.xml` vazio); compilação com `javac` e `backend/lib/ojdbc11.jar`. Maven não estava instalado. |
| Java | JDK 26 instalado; o código já usava `Locale.of` (Java 19+). |
| Arquitetura | API REST em `com.turismo.api` (JDK `HttpServer`, porta 8083) + JDBC; frontend Swing em `com.turismo.frontend` que devia consumir a API. |
| Models | 15 classes, com tipos errados em alguns campos (avaliações e classificações como `String`, datas como `java.util.Date`, valores como `Double`). |
| DAOs | 15 classes com CRUD, mas **todas engoliam as exceções** e devolviam lista vazia ou `false`: um erro de BD aparecia como "sem dados". |
| Handlers | 15 classes, **só GET**: POST, PUT e DELETE devolviam 501. JSON montado à mão com `String.format("%f")`, que com o locale pt-PT gerava `25000,000000` (JSON inválido). A rota `/pagamentos/reserva/{id}` nunca era alcançada. |
| Ligação | URL e utilizador fixos no código; palavra-passe por variável de ambiente; uma ligação nova por pedido. |
| Frontend | `MainApplication` + `DashboardPanel` (contadores fixos a 0) + 3 painéis (`DestinoPanel`, `ReservaPanel`, `TuristaPanel`) que nunca carregavam dados; `ApiClient` apontava para a porta 8082 (a API usa a 8083). |
| Testes | `DestinoDAOTest` com `main()` (não JUnit), que já não compilava (IDs `int`) e tinha a palavra-passe da BD num comentário. |

### Base de dados real (verificada por consulta ao dicionário de dados)

- As 15 tabelas, colunas e constraints coincidem com `database/tabelas.sql`.
- As 9 sequências existem; os códigos seguem o formato `XX-000` (`'DT-' || LPAD(ID_destino.NEXTVAL, 3, '0')`).
- As 13 funções de `functions.sql` estão **VALID**.
- **5 procedures INVALID**: `registrar_reserva`, `registrar_pagamento`, `registrar_atracao`, `registrar_visita` e `alterar_horario_atracao` (variáveis não declaradas, colunas inexistentes).
- **12 triggers INVALID** (`REGEXP` em vez de `REGEXP_LIKE`, `END IF` em falta, colunas de outras tabelas). Confirmado em transação com ROLLBACK: qualquer INSERT em `Atracao`, `Alojamento`, `Reserva`, `Pagamento`, `Operador_Turista` e `Reserva_Alojamento`, e qualquer DELETE em `Turista` e `Reserva`, falha com `ORA-04098`.
- **Nenhuma das 24 views existe** (`user_views` vazia).
- As tabelas e os triggers de auditoria (`Tabelas_de_Historico.sql`, primeira parte de `Triggers.sql`) não existem (o script tem `UPTADE`).
- Dados reais: 2 destinos, 5 turistas, 2 tipos de alojamento, 2 transportes, 2 operadores, 2 preferências, 2 visitas, 2 associações operador-transporte. As restantes tabelas estão vazias.
- É obrigatório `oracle.jdbc.timezoneAsRegion=false` (sem isto: `ORA-01882`).
- O 10g XE tem poucos processos: abrir uma ligação por pedido esgotou o listener durante os testes (`ORA-12516`).

## 2. Decisões

| Decisão | Motivo |
|---|---|
| Manter a arquitetura Swing → API REST → JDBC | Já existia e o frontend já tinha sido pensado para consumir a API. Não foi introduzido nenhum framework novo. |
| Maven multi-módulo (`backend`, `frontend`), Java 21 | O código já precisava de Java 19+; 21 é a versão LTS mais próxima. |
| Pool de ligações HikariCP (máx. 4) | Evidência: `ORA-12516` com uma ligação por pedido. `DatabaseConnection.getConnection()` mantém a mesma assinatura. |
| Listas a partir das tabelas base, com joins | As views não têm as chaves estrangeiras necessárias para editar (ex.: `v_reservas_detalhe` não tem `ID_turista`; `v_turista_destinos` não tem nenhum ID). |
| As 24 views no separador "Relatórios" de cada módulo e no painel inicial | Os relatórios e indicadores são lidos diretamente das views (sem duplicar a lógica em Java). Se a view não existir, aparece o erro real e não dados de substituição. |
| Reutilizar as funções e procedures válidas | Ver a tabela da secção 3. As procedures escrevem os erros em `DBMS_OUTPUT` em vez de os lançar, por isso a API lê essa saída para não esconder falhas (`PlSqlOutput`). |
| SQL direto onde a procedure está inválida ou não serve | `registrar_*` estão INVALID; `atualizar_pref_turista` altera todas as preferências do turista (viola a chave composta); `associar_alojamento_reserva` faz COMMIT e impediria criar a reserva e os alojamentos de forma atómica. |
| Listas de valores (regiões, estados…) lidas das constraints CHECK | A BD é a fonte de verdade; não há listas duplicadas no Java (`/api/dominios`). |
| Corrigir a BD num script à parte (`database/correcoes.sql`), sem o aplicar | Alterar o esquema precisa de autorização do utilizador. |
| Testes de escrita só com ROLLBACK | Política segura enquanto não houver autorização para gravar dados de teste. |

## 3. Mapa dos módulos: leitura, escrita e views

| # | Módulo (tabela) | Lista | Criar | Editar | Eliminar | Views nos relatórios |
|---|---|---|---|---|---|---|
| 1 | Destinos (`Destino`) | tabela + contagens | SQL + seq. `ID_destino` | SQL | SQL (trigger `deleteDestino` em cascata, com aviso dos dependentes) | `v_total_atracoes_destino`, `v_total_alojamentos_destino` |
| 2 | Atrações (`Atracao`) | tabela + destino | SQL + seq. `ID_atracao` | SQL | SQL | `v_atracoes_destino` |
| 3 | Alojamentos (`Alojamento`) | tabela + tipo + destino + `Media_Avaliacao_Alojamento` | SQL + seq. | SQL | SQL (`deleteAlojamento`) | `v_alojamentos_detalhe`, `v_media_avaliacao_alojamento`, `v_alojamentos_acima_media` |
| 4 | Tipos de alojamento | tabela + contagem | procedure `definir_tipo_alojamento` | SQL (só capacidade) | SQL | — (capacidade em `v_alojamentos_detalhe`) |
| 5 | Transportes | tabela + contagem | SQL + seq. | SQL | SQL (`deleteTransporte`) | `v_transportes_disponiveis` |
| 6 | Turistas | tabela + nº de reservas | função `Insere_Turista` | SQL | função `Elimina_Turista` | `v_turistas_sem_reserva`, `v_receita_nacionalidade` |
| 7 | Preferências (chave composta) | tabela + turista | procedure `inserir_preferencia` | troca atómica (DELETE + INSERT numa transação) | SQL | `v_turista_preferencias` |
| 8 | Operadores | tabela + `Media_Avaliacao_Operador` | SQL + seq. | SQL | SQL (`deleteOperador`) | `v_avaliacao_operador` |
| 9 | Reservas | tabela + turista + noites + `Total_Pago_Reserva` + `Reserva_Completa` | transação: reserva + alojamentos escolhidos | SQL (uma instrução) | SQL (`deleteReserva`) · ação **Cancelar** = procedure `cancelar_reserva` | `v_reservas_detalhe`, `v_reservas_ativas`, `v_total_pago_reserva`, `v_reservas_sem_alojamento`, `v_reservas_sem_pagamento` |
| 10 | Pagamentos | tabela + turista | SQL + seq. (estado PENDENTE/FALHADO) | SQL (bloqueado se CONCLUIDO) | SQL · ação **Concluir e emitir recibo** = função `Concluir_Pagamento` · ação **Converter para dólares** = função `Valor_Em_Dolar` | `v_recibos`, `v_receita_mensal`, `v_aud_pagamento_data_futura`, `v_aud_recibo_sem_concluido` |
| 11 | Visitas (`Turista_Destino`, chave de 3 colunas) | tabela + nomes | função `Regista_Visita` (respeita a capacidade) | — (todas as colunas são chave) | SQL | `v_turista_destinos` |
| 12 | Alojamentos das reservas | tabela + nomes | procedure `associar_alojamento_reserva` | **Avaliar** = procedure `avaliar_alojamento` | SQL | `v_reservas_alojamento` |
| 13 | Operadores e turistas | tabela + nomes | procedure `associar_operador_turista` | **Avaliar** = procedure `avaliar_operador` | SQL | `v_avaliacao_operador` |
| 14 | Operadores e transportes | tabela + nomes | procedure `associar_operador_transporte` | — (todas as colunas são chave) | SQL | `v_operadores_transportes` |
| 15 | Operadores e alojamentos | tabela + nomes | procedure `associar_operador_alojamento` | — (todas as colunas são chave) | SQL | `v_operadores_alojamentos` |

**Painel inicial:**
- totais das 15 tabelas (`COUNT` numa só consulta: não existe view com estes totais);
- receita total e por mês (`v_receita_mensal`);
- receita por nacionalidade (`v_receita_nacionalidade`);
- atrações por destino (`v_total_atracoes_destino`);
- reservas ativas (`v_reservas_ativas`);
- integridade: objetos inválidos e views em falta (dicionário de dados) e as quatro views de verificação.

Funções de leitura usadas no detalhe: `Retorna_Info_Destino` e `Lista_Atracoes_Destino` (ação "Ver ficha do destino").

## 4. Ficheiros

### Criados
- `pom.xml`, `backend/pom.xml`, `frontend/pom.xml`, `.gitignore`
- `config/turismo.properties`, `config/turismo.local.properties.example`
- `database/correcoes.sql`
- `executar.sh`, `executar.bat`
- Backend:
  - `util/AppConfig`, `ApiException`, `DbErrors`, `ConstraintCatalog`, `ConnectionProvider`, `Validator`, `Json`;
  - `dao/BaseDAO`, `IdGenerator`, `PlSqlOutput`, `RelatorioDAO`, `DashboardDAO`, `SchemaDAO`;
  - `handler/DashboardHandler`, `RelatorioHandler`, `EstadoHandler`, `DominioHandler`;
  - `resources/simplelogger.properties`.
- Frontend:
  - `api/ApiClient`, `ApiException`, `ClientConfig`;
  - `ui/async/Task`;
  - `ui/theme/Palette`, `Theme`, `Icons`;
  - `ui/components/*` (UI, Card, Surface, Scroll, WrapText, Spinner, StateView, Badge, Toast, SearchField, Sidebar, StatusBar, BarChart);
  - `modules/*` (ModuleSpec, ColumnSpec, FieldSpec, ActionSpec, ReportSpec, Modules, CrudPage, FormDialog, ReportPanel, RowsTableModel, CellRenderer, BadgePainter, Lookups, Format, Page);
  - `pages/DashboardPage`, `MainWindow`, fontes IBM Plex Sans.
- Testes: ver [RELATORIO_TESTES.md](RELATORIO_TESTES.md).

### Alterados
- `backend/.../util/DatabaseConnection`: configuração externa e pool. Mantém `getConnection()` e `closeConnection()`.
- `backend/.../TurismoApiServer`: porta e endereço configuráveis, pool de threads, novos endpoints, estado da BD no arranque.
- Os 15 models: tipos corrigidos (`LocalDate`, `BigDecimal`) e campos só de leitura para os nomes relacionados. Getters e setters existentes mantidos.
- Os 15 DAOs: propagam os erros, reutilizam as funções/procedures e mantêm os nomes dos métodos (`getAllX`, `getXById`, `addX`, `updateX`, `deleteX`).
- Os 15 handlers: GET/POST/PUT/DELETE, validação, códigos HTTP e JSON com Gson.
- `frontend/.../MainApplication`: o mesmo ponto de entrada, que agora abre a `MainWindow`.
- `backend/run_turismo.sh`, `compile.sh`, `run.sh` (backend e frontend): passam a usar o Maven.
- `README.md`, `backend/README.md`, este ficheiro, `PROGRESS_CHECKLIST.md`.

### Removidos
- `frontend/.../BasePanel`, `DashboardPanel`, `destino/DestinoPanel`, `reserva/ReservaPanel`, `turista/TuristaPanel`: protótipos que não carregavam dados, substituídos pelo ecrã genérico de módulo.
- `backend/src/test/.../DestinoDAOTest` (não compilava e tinha a palavra-passe), substituído por testes JUnit.
- Ficheiros `.class` que estavam misturados com o código-fonte.

## 5. Riscos e limitações conhecidas

- Enquanto `correcoes.sql` não for aplicado, as escritas listadas na secção 1 falham. A aplicação explica o motivo, mas não as pode concluir.
- Os testes com ROLLBACK consomem números das sequências (o Oracle não recua sequências), por isso os próximos códigos podem ter saltos de numeração.
- As sequências têm `MAXVALUE 999`: ao fim de 999 registos por tabela a criação falha com uma mensagem clara.
- As listas carregam todos os registos (volume atual pequeno). Os relatórios estão limitados a 1000 linhas por view.
- `turista_diagnosis_workflow.js` (na raiz) contém um utilizador e uma palavra-passe em texto simples. Recomenda-se apagar esse ficheiro e mudar a palavra-passe se o projeto for partilhado.
