# Etapa 10 — RiotAccount

## Objetivo

Permitir que um usuário autenticado vincule e desvincule sua conta Riot ao perfil, usando o `RiotApiClient` (etapa 09) para resolver o `puuid` a partir do Riot ID informado, e persistindo esse vínculo com as garantias de unicidade exigidas pelo schema.

Esta etapa entrega a "porta de entrada" da conta Riot no sistema. Ela não sincroniza ranked, partidas ou estatísticas — isso é responsabilidade das etapas 11 a 13, que passam a ter uma `RiotAccount` persistida para trabalhar em cima.

## Escopo

Implementar apenas:

- ajustes na entidade `RiotAccount` para ficar consistente com as demais entidades do projeto (`id` gerado, nomes de campo em *camelCase*, timestamps);
- migration `V2` (V1 é imutável) apenas se novas colunas forem necessárias (ver seção de banco de dados);
- `RiotAccountRepository`;
- `RiotAccountService`, orquestrando `RiotApiClient` + persistência + regras de unicidade;
- exceções de domínio para conta já vinculada / puuid já usado por outro usuário;
- `RiotAccountController` com `POST /api/me/riot-account` e `DELETE /api/me/riot-account`;
- DTOs de request/response que não exponham `puuid` publicamente (ver privacidade);
- resolução do usuário autenticado a partir do JWT já validado pelo `JwtAuthenticationFilter`, apenas o suficiente para estes dois endpoints;
- testes unitários de serviço e testes de controller/segurança.

Não implementar nesta etapa:

- sincronização de ranked (`RankedSyncService`, etapa 11);
- importação/persistência de partidas (`MatchSyncService`, etapa 12);
- cálculo de `PlayerStats` (etapa 13);
- `POST /api/me/riot-account/sync`, cooldown de 2 minutos, scheduler (etapas 17 e 18);
- `GET /api/me`, `PATCH /api/me` completos ou perfil público (`GET /api/players/{id}`) — etapas 08/15;
- qualquer chamada HTTP direta à Riot API fora do `RiotApiClient` já existente.

## Fluxo desta etapa

```text
Usuário autenticado informa Riot ID (gameName#tagLine)
        ↓
RiotAccountService chama RiotApiClient.getAccountByRiotId
        ↓
Conta não encontrada? → 404 (RiotAccountNotFoundException, já existe)
        ↓
Obtém puuid
        ↓
puuid já vinculado a outro usuário? → 409 (conflito)
        ↓
usuário já possui uma RiotAccount? → 409 (conflito; deve remover antes de vincular outra)
        ↓
Persiste RiotAccount (puuid, gameName, tagLine, region, user)
        ↓
Retorna dados públicos da conta vinculada (sem puuid)
```

`DELETE /api/me/riot-account` apenas remove o vínculo do usuário autenticado (sem chamar a Riot API). Definir explicitamente nesta etapa se a remoção é um hard delete da linha em `riot_accounts` ou uma desvinculação lógica — recomendação: hard delete da `RiotAccount`, já que nenhuma partida/estatística ainda está associada a ela nesta etapa (isso muda a partir da etapa 12/13, quando `Match`/`PlayerStats` passarem a referenciar `RiotAccount`, e a remoção precisará ser revisitada).

## Banco de dados

A tabela `riot_accounts` já existe na migration `V1__create_core_schema.sql` com `user_id` único, `puuid` único (índice parcial) e FK opcional para `ranked_stats`.

**Não editar a V1.** Se for necessário registrar quando a conta foi vinculada/atualizada (recomendado, para consistência com `users` e `courses`), criar `V2__add_riot_accounts_timestamps.sql` adicionando `created_at` e `updated_at` com `DEFAULT CURRENT_TIMESTAMP`. Não alterar `ranked_stats_id`, `puuid` ou as constraints existentes nesta etapa — isso é da etapa 11 (`RankedStats`).

## Mapeamento da entidade

Corrigir `RiotAccount` (hoje incompleta) para o padrão usado em `User`/`Course`:

| Coluna | Campo Java sugerido | Regra |
| --- | --- | --- |
| `id` | `Long id` | `@Id` + `@GeneratedValue(strategy = IDENTITY)` (hoje falta) |
| `user_id` | `User user` | `@OneToOne`, obrigatório, único |
| `ranked_stats_id` | `RankedStats rankedStats` | opcional; manter mapeado mas **não popular/consultar** nesta etapa (pertence à 11) |
| `puuid` | `String puuid` | obrigatório após vínculo, único no sistema |
| `game_name` | `String gameName` | *camelCase* (hoje está `game_name`) |
| `tag_line` | `String tagLine` | *camelCase* (hoje está `tag_line`) |
| `region` | `Region region` | `@Enumerated(STRING)`, já existe |
| `created_at` / `updated_at` | `LocalDateTime` | se a migration `V2` for criada |

Usar `@Getter`/`@Setter` (não `@Data`, para evitar `equals`/`hashCode`/`toString` recursivos com `User`), seguindo o padrão já adotado em `User`.

## Estrutura sugerida

```text
portfolio.pucrs.riot
├── client        (etapa 09, não mexer)
├── controller
│   └── RiotAccountController
├── dto
│   ├── LinkRiotAccountRequest
│   └── RiotAccountResponse
├── entity
│   ├── RankedStats   (ajustar apenas se necessário para compilar)
│   ├── Region
│   └── RiotAccount
├── exception
│   ├── RiotAccountAlreadyLinkedException
│   ├── DuplicateRiotAccountException
│   └── ... (client, etapa 09)
├── repository
│   └── RiotAccountRepository
└── service
    └── RiotAccountService
```

## Contrato HTTP

```http
POST /api/me/riot-account
Authorization: Bearer <token>
Content-Type: application/json

{
  "gameName": "Raposa",
  "tagLine": "BR1",
  "region": "BR1"
}
```

Resposta `201 Created`:

```json
{
  "gameName": "Raposa",
  "tagLine": "BR1",
  "region": "BR1"
}
```

```http
DELETE /api/me/riot-account
Authorization: Bearer <token>
```

Resposta `204 No Content`.

Ambos exigem autenticação (`401` sem token válido). Erros de negócio:

- `404` — Riot ID não existe na Riot API;
- `409` — `puuid` já vinculado a outro usuário, ou usuário já possui uma conta Riot vinculada;
- `404`/`400` — `DELETE` quando o usuário não possui conta vinculada (definir um dos dois e manter consistente com o padrão de erros já usado por `GlobalExceptionHandler`).

## Privacidade

- `RiotAccountResponse` nunca inclui `puuid` nem o `id` interno da conta;
- não criar nesta etapa nenhum endpoint público que exponha `RiotAccount` — isso é do perfil público (etapa 15), que deve usar seu próprio DTO público.

## Segurança

Adicionar em `SecurityConfig`:

```text
POST   /api/me/riot-account    → autenticado
DELETE /api/me/riot-account    → autenticado
```

Como `/api/me/**` ainda não tem um controller de perfil completo (etapa 08 não implementada), resolver o usuário autenticado localmente nesta etapa via o `Authentication`/`principal` (userId) já populado por `JwtAuthenticationFilter`, sem construir um mecanismo novo de "usuário atual" — reaproveitar o que a etapa de auth já fornece.

## Testes mínimos

- `RiotAccountService`: vincula com sucesso quando o Riot ID existe e nem puuid nem usuário já têm vínculo;
- `RiotAccountService`: propaga `RiotAccountNotFoundException` quando a Riot API não encontra o Riot ID;
- `RiotAccountService`: rejeita vínculo quando o `puuid` já pertence a outro usuário;
- `RiotAccountService`: rejeita vínculo quando o usuário autenticado já possui uma `RiotAccount`;
- `RiotAccountService`: remove o vínculo existente e é idempotente/gera erro claro se não houver conta para remover (escolher um comportamento e testar);
- controller: `401` sem autenticação nos dois endpoints;
- persistência: `puuid` duplicado é rejeitado pelo banco (constraint), não apenas pela aplicação;
- persistência: dois usuários não conseguem ter a mesma `RiotAccount` (já coberto parcialmente em `UserTest`, revisitar aqui do lado de `RiotAccount`).

Usar Testcontainers + PostgreSQL para os testes de persistência/constraint, como já indicado nas etapas anteriores.

## Critérios de aceite

- usuário autenticado consegue vincular e desvincular sua conta Riot pelos dois endpoints;
- nenhuma etapa de sincronização (ranked/match/stats) foi antecipada;
- `puuid` nunca aparece em resposta HTTP;
- unicidade `1 User ↔ 1 RiotAccount` e `1 RiotAccount ↔ 1 puuid` é garantida tanto na aplicação quanto no banco;
- nenhuma chamada à Riot API ocorre fora do `RiotApiClient` já existente;
- a aplicação continua iniciando com `spring.jpa.hibernate.ddl-auto=validate`;
- os testes definidos para esta etapa passam.
