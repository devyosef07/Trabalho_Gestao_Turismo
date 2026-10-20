# Turismo API (backend)

API REST em Java (servidor HTTP do JDK, sem frameworks) que liga a aplicação desktop à base de dados Oracle 10g por JDBC.

## Executar

```bash
export TURISMO_DB_PASSWORD='...'
mvn -pl backend -am package          # na raiz do projeto
java -jar backend/target/turismo-api.jar
```

Ou use `./run_turismo.sh` nesta pasta. A configuração (endereço da BD, utilizador, porta) está em `../config/turismo.properties`. Veja o [README principal](../README.md).

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
| 404 | Registo ou endpoint inexistente |
| 405 | Método não suportado no endpoint |
| 409 | Chave duplicada, registos dependentes, pagamento já concluído |
| 422 | Validação (`fields`), constraint CHECK, chave estrangeira inexistente, regra de um trigger ou de uma função |
| 503 | Base de dados indisponível, trigger INVALID, view em falta |
| 504 | Consulta demasiado lenta |

## Endpoints

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
- `POST /api/reservas` aceita `"alojamentos": ["AL-001", ...]`, gravados na mesma transação.
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

`POST /api/{recurso}` cria a associação. Para saber que função, procedure ou SQL é usado em cada escrita, consulte a secção 3 do [plano de implementação](../IMPLEMENTATION_PLAN.md).

## Exemplos

```bash
curl http://localhost:8083/api/estado
curl http://localhost:8083/api/destinos
curl -X POST http://localhost:8083/api/destinos \
     -H 'Content-Type: application/json' \
     -d '{"nomeDestino":"Ilha de Moçambique","regiao":"Nampula","capacidadeVisitantes":200}'
curl -X DELETE http://localhost:8083/api/turistaPreferencias/TR-001/Praias
```

## Estrutura

```
src/main/java/com/turismo/api/
  TurismoApiServer.java   arranque, rotas e pool de threads
  model/                  15 entidades
  dao/                    acesso aos dados (15 DAOs + painel, relatórios, estado)
  handler/                endpoints HTTP
  util/                   configuração, ligação (pool), erros, validação, JSON
src/test/java/            testes unitários e de integração (ver ../RELATORIO_TESTES.md)
```

A pasta `lib/` (com `ojdbc11.jar`) e as pastas `out*/` são do processo de compilação antigo com `javac`. O Maven usa o driver do Maven Central, na mesma versão.
