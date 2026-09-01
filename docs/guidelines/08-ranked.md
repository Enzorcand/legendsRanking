# Etapa 11 — Ranked

## Objetivo

Depois que uma `RiotAccount` é vinculada (etapa 10), buscar sua entrada de **Ranked Solo/Duo** na Riot API e persistir isso como `RankedStats` — o elo atual do jogador, sem histórico. Essa etapa fecha o fluxo "vincula conta → sincroniza dados" descrito no fluxo geral do projeto, mas só para o elo; partidas (etapa 12) e estatísticas agregadas (etapa 13) continuam fora do escopo aqui.

## Escopo

Implementar apenas:

- entidade `Season`, mapeando a tabela `seasons` (já criada na `V1`, mas nunca usada até agora) — mínimo necessário para `RankedStats` poder se associar a uma temporada;
- seed de uma temporada ativa via migration (o projeto exige "no MVP haverá uma temporada ativa" e hoje a tabela `seasons` está vazia);
- completar a entidade `RankedStats` (hoje é só um `@Id` solto) com os campos de elo: tier, divisão, LP, vitórias, derrotas e a temporada;
- enum `Tier`, normalizando os tiers da Riot na ordem oficial (`IRON` → `CHALLENGER`) — por ser um `enum` declarado nessa ordem, a comparação (`compareTo`) já dá a ordenação correta sem precisar de um comparador à parte;
- `RankedSyncService`, que busca as entradas ranked via `RiotApiClient.getLeagueEntriesByPuuid`, filtra somente `RANKED_SOLO_5x5` e faz upsert do `RankedStats` da conta;
- acionar essa sincronização uma vez, logo depois que `RiotAccountService.linkAccount` persiste a conta — sem quebrar o vínculo se a Riot API falhar nesse momento;
- testes unitários do novo serviço e do hook em `RiotAccountService`.

Não implementar nesta etapa:

- `MatchSyncService` ou qualquer persistência de partidas (etapa 12);
- `PlayerStats`, win rate calculado, lane principal ou campeão mais jogado (etapa 13) — essas contas dependem de partidas, que ainda não existem;
- `POST /api/me/riot-account/sync`, cooldown de 2 minutos, scheduler periódico (etapas 17 e 18) — a sincronização desta etapa acontece só uma vez, no momento do vínculo;
- qualquer endpoint HTTP novo — `RankedStats` ainda não é exposta por nenhuma rota (isso é `GET /api/players/{id}` na etapa 15, e `/api/ranking` na etapa 14);
- `WinRateCalculator`/`RankCalculator` como utilitários — sem um consumidor real ainda (nenhum endpoint expõe elo ou ranking), introduzi-los agora seria código morto. `Tier` já resolve a ordenação por si só; `WinRateCalculator` fica para a etapa 13, quando `PlayerStats` precisar calcular win rate de verdade;
- reimportar/recalcular elo em segundo plano — o sync daqui é síncrono e acontece uma única vez, no request de vínculo.

## Banco de dados

`V1`, `V2` e `V3` são imutáveis. Duas migrations novas:

`V4__add_ranked_stats_columns.sql` — adiciona as colunas de elo à tabela `ranked_stats` (hoje só tem `id`):

```sql
ALTER TABLE ranked_stats
    ADD COLUMN season_id BIGINT REFERENCES seasons (id),
    ADD COLUMN tier VARCHAR(20),
    ADD COLUMN division VARCHAR(5),
    ADD COLUMN league_points INT,
    ADD COLUMN wins INT,
    ADD COLUMN losses INT,
    ADD COLUMN updated_at TIMESTAMP;
```

Todas as colunas ficam nullable — uma `RankedStats` só existe quando o jogador tem uma entrada Solo/Duo; jogador unranked simplesmente não tem linha (a FK `riot_accounts.ranked_stats_id` já é opcional desde a `V1`).

`V5__seed_default_season.sql` — sem isso não existe temporada ativa para associar o elo sincronizado:

```sql
INSERT INTO seasons (name, start_date, active)
VALUES ('Temporada Atual', CURRENT_DATE, TRUE);
```

## Mapeamento das entidades

`Season` (nova):

| Coluna | Campo Java | Regra |
| --- | --- | --- |
| `id` | `Long id` | `@Id` + `@GeneratedValue(IDENTITY)` |
| `name` | `String name` | obrigatório |
| `riot_season_id` | `String riotSeasonId` | opcional |
| `start_date` | `LocalDate startDate` | obrigatório |
| `end_date` | `LocalDate endDate` | opcional |
| `active` | `boolean active` | só uma `true` por vez (garantido pelo índice único parcial da `V1`) |

`RankedStats` (completar):

| Coluna | Campo Java | Regra |
| --- | --- | --- |
| `season_id` | `Season season` | `@ManyToOne`, preenchido no sync |
| `tier` | `Tier tier` | `@Enumerated(STRING)` |
| `division` | `String division` | `"I"`–`"IV"`; `null` para Master/GM/Challenger |
| `league_points` | `int leaguePoints` | |
| `wins` / `losses` | `int wins` / `int losses` | brutos, como a Riot retorna — win rate/total de partidas continuam sendo calculados sob demanda, não persistidos |
| `updated_at` | `LocalDateTime updatedAt` | atualizado a cada sync |

`Tier` (novo enum, mesmo pacote de `Region`):

```text
IRON, BRONZE, SILVER, GOLD, PLATINUM, EMERALD, DIAMOND, MASTER, GRANDMASTER, CHALLENGER
```

## Estrutura sugerida

```text
portfolio.pucrs.season
├── entity
│   └── Season
└── repository
    └── SeasonRepository

portfolio.pucrs.riot
├── entity
│   ├── RankedStats     (completar)
│   ├── Region          (não mexer)
│   ├── RiotAccount     (não mexer)
│   └── Tier            (novo)
├── repository
│   └── RankedStatsRepository
└── service
    ├── RankedSyncService    (novo)
    └── RiotAccountService   (chama RankedSyncService após o link)
```

## Regra de sincronização

```text
RiotAccountService.linkAccount persiste a RiotAccount
        ↓
RankedSyncService.syncRankedStats(riotAccount)
        ↓
RiotApiClient.getLeagueEntriesByPuuid(puuid)
        ↓
Existe entrada com queueType == RANKED_SOLO_5x5?
        ↓                              ↓
      não                             sim
        ↓                              ↓
  não faz nada                  cria ou atualiza a RankedStats
  (conta fica unranked,         da RiotAccount, associada à
   sem RankedStats)             temporada ativa
```

Se a chamada à Riot API falhar (erro de rede, rate limit, etc.), `RiotAccountService.linkAccount` deve capturar a exceção do sync, deixar a conta vinculada mesmo assim, e simplesmente não ter `RankedStats` ainda — sem propagar erro para quem chamou o `link`. Isso é consistente com a regra geral do projeto de nunca apagar/bloquear dados por indisponibilidade da Riot API; a etapa 17 (sync manual) e 18 (scheduler) são o mecanismo de retry.

Resync (chamado de novo para a mesma conta) atualiza a `RankedStats` existente em vez de criar uma nova linha — reforça a regra de "elo atual apenas, sem histórico".

## Testes mínimos

- `RankedSyncService`: cria uma `RankedStats` quando existe uma entrada `RANKED_SOLO_5x5`;
- `RankedSyncService`: ignora entradas de outras filas (ex.: `RANKED_FLEX_SR`) e não persiste nada quando só existem essas;
- `RankedSyncService`: não persiste nada quando a lista de entradas está vazia (jogador unranked);
- `RankedSyncService`: uma segunda sincronização atualiza a `RankedStats` existente em vez de criar outra;
- `RiotAccountService`: `linkAccount` aciona `RankedSyncService.syncRankedStats` depois de salvar a conta;
- `RiotAccountService`: `linkAccount` continua retornando sucesso mesmo se `RankedSyncService` lançar uma exceção (falha da Riot API não derruba o vínculo).

## Critérios de aceite

- toda `RiotAccount` recém-vinculada tenta sincronizar o elo Solo/Duo automaticamente;
- jogador sem entrada Solo/Duo permanece sem `RankedStats`, sem erro;
- nenhuma partida, estatística agregada ou endpoint novo foi antecipado;
- falha da Riot API durante o sync inicial não impede o vínculo da conta;
- a aplicação continua iniciando com `spring.jpa.hibernate.ddl-auto=validate`;
- os testes definidos para esta etapa passam.
