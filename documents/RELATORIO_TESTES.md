# Relatório de testes

Executado em 20-10-2026 em macOS (Darwin 27), OpenJDK 26.0.2, Maven 3.10.0 (o Maven corre sobre o OpenJDK 27 do Homebrew), contra o Oracle Database 10g Express Edition 10.2.0.1.0 na VM `192.168.64.4`.

## Comandos executados

| Comando | Resultado |
|---|---|
| `mvn clean test` (raiz, os dois módulos) | **BUILD SUCCESS**: 91 testes, 0 falhas, 0 erros, 0 ignorados |
| `mvn package` | **BUILD SUCCESS**: `backend/target/turismo-api.jar` e `frontend/target/turismo-desktop.jar` |
| `java -jar backend/target/turismo-api.jar` | Arranca, liga-se à BD e lista os 16 objetos inválidos e as 24 views em falta |
| `java -jar frontend/target/turismo-desktop.jar` | Janela aberta; a consola não mostra erros |
| `./executar.sh` com a API parada | Arranca a API, abre a aplicação e, ao fechar a aplicação, termina a API |
| `./executar.sh` com a API já a correr | Deteta a API e só abre a aplicação |
| `./executar.sh` sem palavra-passe | Mensagem clara, código de saída 1 |

Não existia Maven Wrapper; o Maven foi instalado com `brew install maven`.

## Resultado por classe de teste

| Módulo | Classe | Tipo | Testes | Resultado |
|---|---|---|---|---|
| backend | `DbErrorsTest` | unitário | 11 | ✅ |
| backend | `ValidatorTest` | unitário | 9 | ✅ |
| backend | `JsonTest` | unitário | 4 | ✅ |
| backend | `AppConfigTest` | unitário | 5 | ✅ |
| backend | `ConstraintCatalogTest` | unitário | 2 | ✅ |
| backend | `PlSqlOutputTest` | unitário | 6 | ✅ |
| backend | `TuristaHandlerHttpTest` | HTTP com DAO falso | 8 | ✅ |
| backend | `OracleIntegrationTest` | **integração com o Oracle real** | 18 | ✅ |
| frontend | `ApiClientTest` | HTTP com servidor falso | 7 | ✅ |
| frontend | `ModulesTest` | unitário | 8 | ✅ |
| frontend | `MainWindowUiTest` | **interface real** com API falsa | 9 | ✅ |
| frontend | `LiveApplicationE2ETest` | **interface real + API real + Oracle real** | 4 | ✅ |
| | **Total** | | **91** | **91 ✅** |

## O que cada nível prova

### Compilação
Os dois módulos compilam com `--release 21`.

### Integração com o Oracle (`OracleIntegrationTest`)
- Os 15 DAOs leem as suas tabelas sem erro. Cada registo das listas volta a ser encontrado pela sua chave, incluindo as chaves compostas com data.
- Os totais do painel coincidem com as listas.
- As 24 views: cada uma ou é lida, ou falha com `ORA-00942`, consoante exista na BD. Uma view que não está na lista permitida devolve 404.
- As listas de valores vêm das constraints CHECK reais.
- As funções de leitura (`Retorna_Info_Destino`) devolvem o texto esperado.
- **Escritas numa transação desfeita (ROLLBACK)**: criar, alterar e eliminar destino; criar e eliminar transporte e operador; trocar uma preferência; alterar turista e tipo de alojamento. Depois do teste, os dados são os originais.
- A constraint CHECK da BD rejeita uma região inválida e a mensagem mostra a regra (`regiao in (...)`).
- As escritas bloqueadas por triggers inválidos (atração, reserva, cancelar reserva) devolvem o erro `TRIGGER_INVALIDO` com o nome do trigger, e não um falso sucesso.
- Funções e procedures em caminhos que não gravam: `Concluir_Pagamento('ZZ-999')`, `Regista_Visita('ZZ-999', …)`, `Elimina_Turista('ZZ-999')`. Também `inserir_preferencia` com um turista inexistente, o que prova que a falha engolida pela procedure é detetada através do `DBMS_OUTPUT`.

Verificação manual depois dos testes (consulta direta): as contagens das 15 tabelas são iguais às iniciais, não há registos `[TESTE]` e as preferências e tipos de alojamento estão intactos.

### Interface com API falsa (`MainWindowUiTest`)
A janela real abre e é conduzida programaticamente na EDT:
- navega pelos 16 ecrãs;
- mostra o estado vazio, a tabela e o detalhe com ações;
- um erro 503 aparece como erro (não como lista vazia) e "Tentar novamente" recupera;
- API desligada → estado "Sem ligação" e aviso na barra de estado;
- a pesquisa ignora acentos e mostra "Sem resultados";
- o formulário valida no cliente e não envia o pedido quando há erros;
- os erros de campo devolvidos pela API aparecem junto ao campo;
- **capacidade de resposta**: com um pedido de 2,5 s, a EDT respondeu sempre em menos de 250 ms, o indicador de carregamento estava animado e o seu temporizador parou quando deixou de estar visível;
- a troca de tema claro/escuro funciona.

### Ponta a ponta com dados reais (`LiveApplicationE2ETest`)
A aplicação real, ligada à API real e ao Oracle:
- os totais do painel são iguais aos da API;
- a barra de estado indica "Ligado a Oracle 10g…";
- **os 15 módulos carregam sem erro** e o número de linhas de cada tabela da interface é igual ao da API;
- o relatório de uma view em falta mostra a mensagem correta;
- o tema escuro funciona.

As capturas ficam em `frontend/target/screenshots/` e foram revistas visualmente. Essa revisão levou a várias correções: indicador da navegação, texto cortado, scroll provocado pelo caret, tamanho do formulário e bordas no tema escuro.

## Problemas encontrados pelos testes e corrigidos

| Problema | Como apareceu | Correção |
|---|---|---|
| `ORA-12516` (listener sem processos) | 13 testes de integração falharam em rajada | Pool de ligações (HikariCP, 4) |
| `ORA-17027` (fluxo já fechado) | A mensagem da constraint não aparecia | A coluna `search_condition` (LONG) passou a ser lida uma só vez; as falhas deixaram de ficar em cache |
| `NullPointerException` ao abrir a janela | Teste ponta a ponta | Contexto de acessibilidade nos componentes desenhados à mão |
| Painel deslocado ao abrir | Captura + rastreio do viewport | O caret do FlatLaf fazia scroll; os textos só de leitura usam agora um caret sem scroll |
| Aviso SLF4J e aviso de acesso nativo | Consola do jar | Versão do `slf4j-api` fixada; `Enable-Native-Access` no manifesto |

## O que não foi testado (e porquê)

| Não testado | Motivo |
|---|---|
| Escritas reais em Atrações, Alojamentos, Reservas, Pagamentos, avaliações e eliminação de Turistas/Reservas | Os triggers estão INVALID na BD; só foi testado que o erro é mostrado corretamente. Precisa de `correcoes.sql`. |
| As 24 views com conteúdo | Não existem na BD. |
| Funções e procedures que fazem COMMIT (`Insere_Turista`, `Concluir_Pagamento` em sucesso, `associar_*`, `inserir_preferencia` em sucesso, `definir_tipo_alojamento`) | Gravariam dados de forma permanente; a autorização para gravar dados de teste não foi dada. |
| `database/correcoes.sql` | Não foi executado (sem autorização para alterar o esquema). A sintaxe foi revista mas **não compilada no Oracle**. |
| Windows | Não há uma máquina Windows disponível neste ambiente. |
| Robot do AssertJ Swing (rato/teclado reais) | No macOS precisa da permissão de Acessibilidade; a interface é conduzida na EDT com os componentes reais. |
| Criar, editar e eliminar através da interface contra a BD real | Para não gravar dados; o caminho completo foi testado contra a API falsa, e a API contra o Oracle em separado. |

## Efeito secundário nos dados

As sequências do Oracle não recuam com ROLLBACK. Os testes de criação consumiram alguns números de `ID_destino`, `ID_transporte`, `ID_operador`, `ID_atracao` e `ID_reserva`, por isso os próximos códigos gerados vão ter saltos de numeração (por exemplo, o próximo destino pode não ser DT-003). Não foi alterado nenhum dado.

## Como repetir

```bash
export TURISMO_DB_PASSWORD='...'
java -jar backend/target/turismo-api.jar &   # para os testes ponta a ponta
mvn clean test
```

Sem BD acessível, os testes de integração ficam como *skipped*. Sem a API a correr, os testes ponta a ponta também ficam como *skipped*.
