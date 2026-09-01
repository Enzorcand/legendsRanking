# Etapa 17 — Sincronização manual

## Objetivo

Expor `POST /api/me/riot-account/sync`, permitindo que o próprio usuário force uma nova sincronização (ranked stats + partidas + player stats) da conta Riot vinculada, sem esperar o scheduler automático (etapa 18, ainda não implementado). Protegido por um cooldown de 2 minutos para evitar abuso da Riot API.

## Escopo

Implementar apenas:

- coluna `last_synced_at` em `riot_accounts` (migration V9) — controla o cooldown; diferente de `updated_at`, que muda em qualquer alteração da entidade e não serviria como marcador confiável de "última sincronização";
- `RiotAccount.lastSyncedAt`;
- `RiotAccountService.syncAccount(userId)`:
  - busca a `RiotAccount` do usuário (senão `404`, mesma exceção do unlink);
  - se `lastSyncedAt` existir e o cooldown de 2 minutos não tiver passado, lança `SyncCooldownException` (`429`) informando quantos segundos faltam;
  - senão, chama `rankedSyncService.syncRankedStats`, `matchSyncService.syncMatches`, `playerStatsService.recalculate` **sem** engolir exceções (diferente do `linkAccount`, que é best-effort) — aqui é uma ação explícita do usuário, então uma falha da Riot API deve ser reportada, não silenciada;
  - atualiza `lastSyncedAt = now` **antes** de propagar qualquer falha de sync (o cooldown vale mesmo que a sincronização falhe, para não permitir retry imediato em loop contra uma API fora do ar);
  - retorna `RiotAccountResponse` (dados já existentes) — não é preciso um DTO novo, o endpoint só confirma que a sincronização rodou.
- `POST /api/me/riot-account/sync` em `RiotAccountController`;
- `SyncCooldownException`, mapeada para `429 Too Many Requests` no `GlobalExceptionHandler`;
- testes unitários do serviço e do controller.

Não implementar nesta etapa:

- scheduler automático (etapa 18, próxima etapa);
- rate limiting por IP/usuário além do cooldown de conta (etapa 20 trata segurança/rate limiting de forma mais ampla);
- histórico de sincronizações.

## Banco de dados

```sql
ALTER TABLE riot_accounts ADD COLUMN last_synced_at TIMESTAMP;
```

Nula até a primeira sincronização manual (o link inicial, etapa 09, não conta como "sincronização" para efeito de cooldown — só ajustes explícitos via este endpoint atualizam a coluna).

## Contrato HTTP

```http
POST /api/me/riot-account/sync
Authorization: Bearer <token>
```

- `200 OK` com `RiotAccountResponse` quando a sincronização roda;
- `404` se a conta Riot não estiver vinculada;
- `429` se o cooldown de 2 minutos ainda não tiver passado, com mensagem indicando os segundos restantes;
- `502`/erro repassado se a Riot API falhar (via `RiotApiException`, já existente).

## Testes mínimos

- `RiotAccountService.syncAccount`: dispara os três syncs e atualiza `lastSyncedAt` quando não há cooldown ativo;
- `RiotAccountService.syncAccount`: lança `SyncCooldownException` quando `lastSyncedAt` é recente (< 2 min);
- `RiotAccountService.syncAccount`: permite sync quando `lastSyncedAt` é `null` ou mais antigo que 2 min;
- `RiotAccountService.syncAccount`: lança `RiotAccountNotLinkedException` quando o usuário não tem conta vinculada;
- controller: `POST /api/me/riot-account/sync` delega para o service com o id do usuário autenticado.

## Critérios de aceite

- sincronização manual funciona fim a fim contra a Riot API real;
- cooldown de 2 minutos é respeitado (segunda chamada imediata retorna `429`);
- os testes definidos para esta etapa passam.
