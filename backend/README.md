# Turismo API (backend)

API REST em Java (servidor HTTP do JDK, sem frameworks) que liga a aplicação desktop à base de dados Oracle 10g por JDBC.

## Executar

```bash
export TURISMO_DB_PASSWORD='...'
mvn -pl backend -am package          # na raiz do projeto
java -jar backend/target/turismo-api.jar
```

Ou use `./scripts/api.sh` na raiz do projeto. A configuração (endereço da BD, utilizador, porta) está em `../config/turismo.properties`. Veja o [README principal](../README.md).

No arranque, a API mostra o estado da base de dados, incluindo os objetos inválidos e as views em falta. A palavra-passe nunca é escrita nos logs.

## Convenções

- JSON em UTF-8. As datas usam o formato `AAAA-MM-DD`; os números usam ponto decimal.
- Os nomes dos campos são os dos models (`idDestino`, `nomeDestino`, …).
- As respostas de escrita têm a forma `{"message": "...", "data": {registo}}`.
- Os erros têm a forma `{"error": "mensagem em português", "code": "CODIGO", "fields": {"campo": "mensagem"}}`.

| Estado | Quando |
|---|---|
| 200 / 201 | Sucesso / registo criado |
| 400 | JSON inválido ou parâmetro mal formatado |
| 401 | Sem sessão, sessão expirada ou terminada, credenciais incorretas |
| 404 | Registo ou endpoint inexistente |
| 405 | Método não suportado no endpoint |
| 409 | Chave duplicada, registos dependentes, pagamento já concluído |
| 422 | Validação (`fields`), constraint CHECK, chave estrangeira inexistente, regra de um trigger ou de uma função |
| 423 | Conta bloqueada temporariamente (5 tentativas falhadas seguidas) |
| 503 | Base de dados indisponível, trigger INVALID, view em falta |
| 504 | Consulta demasiado lenta |

## Endpoints

Todos os endpoints de dados exigem o cabeçalho `Authorization: Bearer <token>`, obtido em `POST /api/auth/sessao`. Só `/api/estado` e `/api/auth/*` são públicos. Sem sessão válida a resposta é `401` com `"code": "SESSAO_INVALIDA"`; se a BD estiver em baixo, o filtro responde `503` (nunca `401`), para a aplicação não terminar a sessão por causa de uma falha de rede. `api.requireAuth=false` desliga esta exigência (só para testes manuais).

### Contas e sessão (públicos)
| Método | Caminho | Corpo / resposta |
|---|---|---|
| POST | `/api/auth/registo` | `{"nomeCompleto", "dataNascimento": "AAAA-MM-DD", "palavraPasse", "confirmacao"}` → `201 {"message", "data": {"nomeUtilizador", "nomeCompleto", "dataNascimento"}}` |
| POST | `/api/auth/sessao` | `{"nomeUtilizador", "palavraPasse", "manterSessao": true}` → `201 {"token", "iniciadaEm", "expiraEm", "conta": {...}}` (7 dias com `manterSessao`, 12 horas sem) |
| GET | `/api/auth/sessao` | Com `Authorization` → `200 {"iniciadaEm", "expiraEm", "conta": {...}}` |
| DELETE | `/api/auth/sessao` | Com `Authorization` → termina a sessão (`terminar_sessao`) |

O nome de utilizador é gerado a partir do nome completo (primeiro e último nome, sem acentos). A palavra-passe é guardada só como hash PBKDF2-HMAC-SHA256 (`auth/PasswordHasher`) e o token só como SHA-256 (`auth/SessionService`). Leituras: `v_conta_credenciais`, `v_sessao_ativa`; escritas: as 4 procedures de `database/contas.sql`.

### Estado e metadados
| Método | Caminho | Descrição |
|---|---|---|
| GET | `/api/estado` | Estado da API e da BD: versão, objetos INVALID, views em falta |
| GET | `/api/dominios` | Valores permitidos pelas constraints CHECK (`"DESTINO.REGIAO": [...]`) |
| GET | `/api/dashboard` | Totais das 15 tabelas, views do painel e views de verificação (cada secção com `ok` e `data`, ou `ok` e `error`) |
| GET | `/api/relatorios` | Nomes das 24 views disponíveis |
| GET | `/api/relatorios/{view}` | Colunas e linhas da view (máx. 1000) |

### Tabelas com chave simples
`destinos`, `atracoes`, `alojamentos`, `tipoAlojamentos`, `transportes`, `turistas`, `operadores`, `reservas`, `pagamentos`

| Método | Caminho |
|---|---|
| GET, POST | `/api/{tabela}` |
| GET, PUT, DELETE | `/api/{tabela}/{id}` |

Endpoints adicionais:
- `GET /api/destinos/{id}/dependencias`: atrações, alojamentos e visitas que o trigger `deleteDestino` apagaria.
- `GET /api/destinos/{id}/info`: funções `Retorna_Info_Destino` e `Lista_Atracoes_Destino`.
- `GET /api/atracoes?idDestino=` e `GET /api/atracoes/destino/{idDestino}`.
- `GET /api/alojamentos?idDestino=`.
- `POST /api/reservas` aceita `"alojamentos": ["AL-001", ...]`; se a associação de um alojamento falhar, a reserva criada é eliminada (tudo ou nada).
- `POST /api/reservas/{id}/cancelar`: procedure `cancelar_reserva`.
- `GET /api/reservas?idTurista=` e `GET /api/reservas/turista/{idTurista}`.
- `POST /api/pagamentos/{id}/concluir`: função `Concluir_Pagamento` (gera o recibo).
- `GET /api/pagamentos/{id}/dolar?cotacao=64`: função `Valor_Em_Dolar`.
- `GET /api/pagamentos?idReserva=` e `GET /api/pagamentos/reserva/{idReserva}`.

### Tabelas associativas (chave composta no caminho)
| Recurso | Chave | Métodos sobre a chave | Filtros |
|---|---|---|---|
| `turistaPreferencias` | `/{idTurista}/{preferencia}` | GET, PUT (`{"preferencia": nova}`), DELETE | `?idTurista=`, `/turista/{id}` |
| `turistaDestinos` | `/{idDestino}/{idTurista}/{AAAA-MM-DD}` | GET, DELETE | `?idTurista=`, `?idDestino=`, `/turista/{id}`, `/destino/{id}` |
| `reservaAlojamentos` | `/{idReserva}/{idAlojamento}` | GET, PUT (avaliação), DELETE | `?idReserva=`, `?idAlojamento=`, `/reserva/{id}`, `/alojamento/{id}` |
| `operadorTuristas` | `/{idOperador}/{idTurista}` | GET, PUT (avaliação), DELETE | `?idOperador=`, `?idTurista=`, `/operador/{id}`, `/turista/{id}` |
| `operadorTransportes` | `/{idOperador}/{idTransporte}` | GET, DELETE | `?idOperador=`, `?idTransporte=`, `/operador/{id}`, `/transporte/{id}` |
| `operadorAlojamentos` | `/{idOperador}/{idAlojamento}` | GET, DELETE | `?idOperador=`, `?idAlojamento=`, `/operador/{id}`, `/alojamento/{id}` |

`POST /api/{recurso}` cria a associação. Todas as escritas são feitas por procedures ou funções PL/SQL (nunca SQL direto nas tabelas); a lista de qual é usada em cada escrita está na secção 14.7 do [manual técnico](../documents/README.md).

## Exemplos

```bash
curl http://localhost:8083/api/estado
# Iniciar sessão e guardar o token numa variável
TOKEN=$(curl -s -X POST http://localhost:8083/api/auth/sessao \
     -d '{"nomeUtilizador":"maria.cossa","palavraPasse":"..."}' | python3 -c 'import sys,json;print(json.load(sys.stdin)["token"])')
curl -H "Authorization: Bearer $TOKEN" http://localhost:8083/api/destinos
curl -X POST http://localhost:8083/api/destinos -H "Authorization: Bearer $TOKEN" \
     -H 'Content-Type: application/json' \
     -d '{"nomeDestino":"Ilha de Moçambique","regiao":"Nampula","capacidadeVisitantes":200}'
curl -X DELETE -H "Authorization: Bearer $TOKEN" http://localhost:8083/api/auth/sessao
```

## Estrutura

```
src/main/java/com/turismo/api/
  TurismoApiServer.java   arranque, rotas e pool de threads
  model/                  15 entidades
  dao/                    acesso aos dados (15 DAOs + painel, relatórios, estado)
  handler/                endpoints HTTP (AuthHandler: contas e sessões)
  auth/                   hash PBKDF2, regras das contas, tokens de sessão, filtro de autenticação
  util/                   configuração, ligação (pool), erros, validação, JSON
src/test/java/            testes unitários e de integração (ver ../documents/RELATORIO_TESTES.md)
```

O driver Oracle (`ojdbc11`) é descarregado pelo Maven a partir do Maven Central.
