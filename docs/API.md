# API TechFeed BR

## Visão geral

A API usa Java 17, Spring Boot, PostgreSQL, Flyway e Resilience4j. Consulta RSS e APIs externas, normaliza itens, remove duplicidades, mantém o estado das fontes e entrega um feed paginado ao Android e à demonstração web. O conteúdo integral permanece na publicação original; o TechFeed BR guarda título, resumo, metadados, miniatura quando disponível e link canônico.

Base local: `http://localhost:8080/`. Base pública do APK: `https://api.brenopaiva.dev/techfeed/`. A demonstração web consome `https://tcc.brenopaiva.dev/api/v1/` pela mesma origem. A rota de avaliação web só é publicada pelo domínio da demonstração.

## Executar e testar

Na raiz do repositório, execute `docker compose up --build`. Isso inicia PostgreSQL e API, aplica as migrações Flyway e publica a API na porta 8080. Verifique `http://localhost:8080/actuator/health`.

Para executar a API com um PostgreSQL já iniciado, entre em `backend/` e rode `./mvnw spring-boot:run`. Testes: `./mvnw verify` (no Windows, `mvnw.cmd`). `.env.example` contém apenas valores de **desenvolvimento**; nunca coloque senhas de produção nele.

Variáveis principais: `DATABASE_URL` (JDBC PostgreSQL), `POSTGRES_USER`, `POSTGRES_PASSWORD`, `PORT` e, se GNews for explicitamente reativado, `GNEWS_API_KEY`. GNews e TabNews vêm desabilitados; informar uma chave isoladamente não os ativa. URLs e intervalos das fontes ficam em `backend/src/main/resources/application.yml`. Para alterar o banco em produção, crie uma nova migração em `backend/src/main/resources/db/migration/`; não edite uma versão já aplicada.

## Feed e fontes

| Método e rota | Finalidade |
| --- | --- |
| `GET /api/v1/feed` | Notícias paginadas, filtros e estado das fontes. |
| `POST /api/v1/feed/refresh` | Inicia atualização elegível e retorna `202` sem esperar todas as fontes. |
| `GET /api/v1/sources/status` | Disponibilidade, última tentativa/sucesso, quantidade e atualização em andamento. |
| `GET /api/v1/catalog` | Fontes ativas, categorias e tecnologias disponíveis. |
| `GET /actuator/health` | Saúde da API. |

`GET /api/v1/feed` aceita `limit` (1 a 50; padrão 20), `cursor`, `query`, `category` repetível, `source` repetível, `language=pt` e `sort=date|relevance`. Sem `language`, todos os idiomas são elegíveis. O aplicativo inicia com `language=pt` e não mostra filtro de fonte na interface. GNews e TabNews são excluídos do feed mesmo se houver registros históricos no banco.

```sh
curl 'http://localhost:8080/api/v1/feed?language=pt&category=Seguran%C3%A7a&limit=20&sort=date'
```

A resposta contém `items`, `nextCursor`, `generatedAt`, `stale` e `sourceStatuses`. Os itens trazem ID, título, resumo, URL canônica, imagem, idioma, publicação, origem, categorias, tags e pontuação. Use `nextCursor` na página seguinte com **os mesmos filtros**; `null` encerra a paginação. O cursor se baseia na posição ordenada e rejeita combinação com outros filtros.

As fontes ativas são Agência Brasil/EBC, DEV Community, Tecnoblog, Olhar Digital, TudoCelular, Hardware.com.br, Mundo Conectado, MacMagazine e Adrenaline. Os veículos brasileiros de tecnologia usam RSS publicado pelos próprios sites. Falhas são isoladas por fonte e informadas no status. Adaptadores e testes de contrato ficam em `backend/src/main/java/br/com/techfeedbr/integration/` e `backend/src/test/java/br/com/techfeedbr/integration/`.

## Avaliação da demonstração web

A sessão web exige aceite explícito e expira em **30 dias**; uma tarefa diária remove sessões vencidas. O cliente guarda o token da própria sessão para consultar, exportar ou excluir os eventos. O APK possui avaliação local independente. Nenhum endpoint aceita o texto de busca como metadado.

| Método e rota | Resposta e acesso |
| --- | --- |
| `POST /api/v1/web-evaluations` | Cria sessão com `consented: true`; `201` com ID, token e datas. |
| `POST /api/v1/web-evaluations/{id}/events` | Envia 1 a 25 eventos; `202`; exige token. |
| `POST /api/v1/web-evaluations/{id}/finish` | Encerra sessão; `204`; exige token. |
| `GET /api/v1/web-evaluations/{id}` | Consulta a própria sessão; exige token. |
| `DELETE /api/v1/web-evaluations/{id}` | Exclui sessão e eventos; `204`; exige token. |

Criação: `{"sessionCode":"Banca01","taskCode":"BuscaA","consented":true}`. Os quatro endpoints ligados ao ID usam `Authorization: Bearer <token>`. Um lote de eventos tem o formato:

```json
{"events":[{"id":"613a3cc3-9d6d-43eb-99f5-29237458972b","type":"SEARCH","occurredAt":"2026-10-19T12:00:00Z","elapsedMs":12000,"metadata":null}]}
```

Cada evento mantém o mesmo UUID entre tentativas; IDs recebidos anteriormente são ignorados. Tipos aceitos: `SESSION_ENDED`, `TASK_MARKED_COMPLETE`, `FEED_REFRESHED`, `SEARCH`, `FILTER_APPLIED`, `FAVORITE_TOGGLED`, `READ_LATER_TOGGLED` e `ITEM_OPENED`. `FILTER_APPLIED` aceita apenas uma categoria do catálogo em `metadata`; `ITEM_OPENED` aceita um código de fonte ativo; os demais tipos exigem `null`. A API cria `CONSENT_ACCEPTED` e `SESSION_STARTED` ao iniciar. Códigos de sessão/tarefa aceitam 1 a 80 caracteres alfanuméricos, `_` ou `-`.

A exportação consolidada do pesquisador não tem painel público: [`deploy/export-web-evaluations.sh`](../deploy/export-web-evaluations.sh) roda por SSH e gera CSV/JSON privados, sem tokens. O proxy limita chamadas de atualização e avaliação. Implantação e reversão estão em [deploy/README.md](../deploy/README.md).

## Estrutura do backend

- `api/`: contratos HTTP e avaliação web.
- `application/`: feed, cursor, atualização, normalização e deduplicação.
- `integration/`: adaptadores RSS/API.
- `persistence/` e `resources/db/migration/`: banco PostgreSQL.
- `src/test/`: contratos, paginação, falhas parciais e avaliação.
