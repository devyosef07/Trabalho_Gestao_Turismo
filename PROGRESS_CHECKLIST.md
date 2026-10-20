# Checklist de progresso

Estado em 20-10-2026. Legenda: ✅ concluído e testado · 🟡 implementado mas limitado ou não testado contra a BD real · ⛔ bloqueado pela base de dados (precisa de `database/correcoes.sql`).

## Fundações

- ✅ Projeto Maven multi-módulo (`mvn clean test`, `mvn package`), Java 21
- ✅ Configuração externa (`config/turismo.properties`); palavra-passe por variável de ambiente ou ficheiro local fora do controlo de versões
- ✅ Pool de ligações (HikariCP, 4 ligações), timeouts de ligação e de consulta
- ✅ Erros Oracle traduzidos para português, com o estado HTTP adequado; um erro nunca aparece como "lista vazia"
- ✅ Listas de valores lidas das constraints CHECK da BD
- ✅ Leitura da saída `DBMS_OUTPUT` das procedures para detetar falhas que elas próprias engolem
- ✅ API REST com GET/POST/PUT/DELETE para as 15 tabelas, e ainda `/estado`, `/dominios`, `/dashboard` e `/relatorios/{view}`
- ✅ Script de correção da BD (`database/correcoes.sql`) escrito; ⛔ **não aplicado** (aguarda autorização)

## Interface

- ✅ Janela com barra lateral agrupada (indicador animado), barra de estado com a ligação à BD e avisos do esquema
- ✅ Tema claro e escuro (FlatLaf), fonte incluída no jar (igual em macOS e Windows)
- ✅ Ecrã genérico por módulo: pesquisa sem acentos, ordenação, contagem, detalhe do registo, ações, separador de relatórios
- ✅ Estados distintos: a carregar, vazio, sem resultados, erro (com "Tentar novamente"), API indisponível, view em falta
- ✅ Formulários com validação por campo, erros da API junto ao campo e prevenção de submissões duplicadas
- ✅ Todos os pedidos fora da EDT (`SwingWorker`); testado: a EDT respondeu em menos de 250 ms durante um pedido de 2,5 s
- ✅ Confirmação antes de eliminar, com aviso das eliminações em cascata feitas pelos triggers
- ✅ Atalhos de teclado, foco visível, ordem de tabulação, nomes acessíveis nos componentes desenhados à mão
- ✅ Painel inicial com dados reais: totais, receita, reservas ativas, integridade dos dados
- 🟡 Testado em macOS. Windows: só dependências e scripts preparados (`executar.bat`), **não executado em Windows**

## Módulos

| # | Módulo | Ler | Criar | Editar | Eliminar | Notas |
|---|---|---|---|---|---|---|
| 1 | Destinos | ✅ | ✅ (rollback) | ✅ (rollback) | ✅ (rollback) | Cascata pelo trigger `deleteDestino` |
| 2 | Atrações | ✅ | ⛔ trigger inválido | ⛔ trigger inválido | 🟡 | A app mostra o erro do trigger (testado) |
| 3 | Alojamentos | ✅ | ⛔ `VALIDARCAPALOJAMENTO` | ⛔ | 🟡 | |
| 4 | Tipos de alojamento | ✅ | 🟡 procedure (não executada: faz COMMIT) | ✅ (rollback) | 🟡 | |
| 5 | Transportes | ✅ | ✅ (rollback) | 🟡 | ✅ (rollback) | |
| 6 | Turistas | ✅ | 🟡 `Insere_Turista` (não executada: faz COMMIT) | ✅ (rollback) | ⛔ `DELETERESERVA` inválido; caminho de erro testado | |
| 7 | Preferências | ✅ | 🟡 procedure; caminho de erro testado | ✅ troca atómica (rollback) | 🟡 | Chave composta |
| 8 | Operadores | ✅ | ✅ (rollback) | 🟡 | ✅ (rollback) | |
| 9 | Reservas | ✅ | ⛔ triggers de datas (erro tratado, testado) | ⛔ | ⛔ | Cancelar: ⛔ (erro tratado, testado) |
| 10 | Pagamentos | ✅ | ⛔ `VALIDARPAGAMENTO` | ⛔ | 🟡 | Concluir: caminho de erro testado |
| 11 | Visitas | ✅ | 🟡 `Regista_Visita`; caminho de erro testado | — (tudo é chave) | 🟡 | Chave de 3 colunas com data |
| 12 | Alojamentos das reservas | ✅ | ⛔ `VALIDARAVALOJAMENTO` | ⛔ (avaliar) | 🟡 | |
| 13 | Operadores e turistas | ✅ | ⛔ `VALIDARAVAOPERADOR` | ⛔ (avaliar) | 🟡 | |
| 14 | Operadores e transportes | ✅ | 🟡 procedure (não executada: faz COMMIT) | — | 🟡 | |
| 15 | Operadores e alojamentos | ✅ | 🟡 (ainda não há alojamentos) | — | 🟡 | |

"Ler ✅" significa que o teste ponta a ponta abriu o ecrã real com os dados reais e o número de linhas coincidiu com a API.

## Para fechar os itens 🟡 e ⛔

1. Autorizar e aplicar `database/correcoes.sql` (cria as 24 views e corrige os 17 objetos inválidos).
2. Autorizar testes que gravam dados (com limpeza no fim) para as funções e procedures que fazem COMMIT.
3. Correr `mvn clean test` e `executar.bat` num Windows.
