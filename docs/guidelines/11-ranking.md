# Etapa 14 — Ranking

## Objetivo

Expor `GET /api/ranking`, público, listando os jogadores elegíveis ordenados pela regra oficial da Riot (tier → divisão → LP → win rate → vitórias), com paginação e filtros combináveis. É o primeiro endpoint que junta dados de `User`, `RiotAccount`, `RankedStats` e `PlayerStats` — tudo o que as etapas 10–13 sincronizaram, mas nunca haviam sido lidos de volta.

## Decisão de escopo: quando um usuário entra no ranking

O spec original diz que um jogador "ao obter rank, entra automaticamente após sincronização", mas a etapa 08 implementou `ranking_eligible` como um toggle manual que nasce `false` — ou seja, sem mudança nenhuma, ninguém jamais apareceria no ranking sozinho. Ficou decidido resolver essa contradição assim:

- `RankedSyncService` passa a marcar `user.rankingEligible = true` automaticamente **na primeira vez** que cria uma `RankedStats` para a conta (ou seja, quando o jogador é ranqueado pela primeira vez);
- em resyncs seguintes (a `RankedStats` já existe, só está sendo atualizada), esse campo não é mais tocado — assim, se o usuário sair manualmente do ranking depois (`PATCH /api/me/ranking`, etapa 08), um resync futuro não o coloca de volta à força;
- o toggle da etapa 08 continua existindo exatamente como está, só passa a funcionar como "saída e reentrada manual" em vez de "entrada".

Critério final de elegibilidade para aparecer no `/api/ranking`: `user.rankingEligible = true` E `user.active = true` E `user.emailVerified = true` (exigido pelo spec) E a conta tem `RankedStats` (jogador não é Unranked).

## Escopo

Implementar apenas:

- o ajuste em `RankedSyncService` descrito acima;
- mapear `RiotAccount.playerStats` (`@OneToOne(mappedBy = "riotAccount")`) — hoje só existe o lado dono em `PlayerStats`; o ranking precisa navegar nos dois sentidos;
- `RankCalculator`, comparador puro de `RankedStats` por tier → divisão → LP (prioridade de testes unitários do projeto, adiada das etapas 11 e 13 por falta de consumidor até agora);
- uma consulta em `RiotAccountRepository` que já aplica os filtros combináveis (`search`, `courseId`, `role`, `tier`, `championId`) via JPQL;
- `RankingService`, que roda a consulta, ordena em memória (tier/divisão/LP via `RankCalculator`, depois win rate e vitórias como desempate) e pagina o resultado;
- `RankingController` com `GET /api/ranking`, público;
- `RankingEntryResponse` — DTO público, sem `puuid`, sem e-mail, sem id interno;
- testes unitários do calculador e do serviço.

Não implementar nesta etapa:

- `GET /api/ranking/stats` (estatísticas da landing page) — não faz parte do endpoint principal de ranking, fica para quando for pedido;
- `icon` no retorno — o campo aparece no spec, mas nenhum `RiotApiClient` hoje busca ícone de invocador (isso é `summoner-v4`, endpoint que não existe no cliente); expor `null` sempre seria enganoso, então o campo fica de fora até existir uma fonte de dado real;
- qualquer identificador de jogador para navegação a um perfil (`GET /api/players/{id}`, etapa 15) — o esquema de ID desse endpoint futuro (`User` ou `RiotAccount`) ainda não foi decidido; adicionar um campo agora seria um palpite;
- cache do ranking — a consulta roda direto no banco a cada request; performance/cache é uma preocupação para quando houver volume real de usuários, não MVP;
- paginação/ordenação feita pelo banco (`ORDER BY` nativo) — ver nota de performance abaixo.

## Nota de performance (decisão consciente de MVP)

Ordenar corretamente por tier (`IRON`...`CHALLENGER`) exigiria ou uma coluna numérica derivada ou um `CASE WHEN` gigante no SQL. Para o tamanho de usuário esperado no MVP (comunidade de e-sports da PUCRS, não milhões de jogadores), é mais simples buscar todos os candidatos que batem com os filtros, ordenar em memória com `RankCalculator` e só então paginar. Documentado aqui para não ser lido como descuido — se o volume de usuários crescer a ponto de isso pesar, é a primeira coisa a revisitar.

## Mapeamento da consulta

```text
RiotAccount (INNER JOIN FETCH user, course, rankedStats / LEFT JOIN FETCH playerStats)
WHERE user.rankingEligible = true
  AND user.active = true
  AND user.emailVerified = true
  AND gameName LIKE searchPattern   (case-insensitive; ver nota abaixo)
  AND (courseId informado? course.id = courseId)
  AND (tier informado?     rankedStats.tier = tier)
  AND (role informado?     playerStats.primaryRole = role)
  AND (championId informado? playerStats.mostPlayedChampionId = championId)
```

**Pegadinha de Hibernate/Postgres encontrada testando contra o banco real**: o padrão `(:search IS NULL OR LOWER(col) LIKE LOWER(CONCAT('%', :search, '%')))` quebra em runtime com `function lower(bytea) does not exist` quando `:search` é `null` — o Postgres/Hibernate não consegue inferir o tipo do parâmetro dentro de `CONCAT`/`LOWER` no caso nulo e cai para `bytea`. A correção foi montar o padrão `%...%` já pronto (não nulo) no `RankingService` antes de passar pro repositório — quando não há busca, o padrão vira `%%`, que casa com tudo. Isso elimina o `IS NULL` ambíguo para esse parâmetro específico; os demais filtros (`courseId`, `tier`, `role`, `championId`) não tiveram esse problema por serem comparações diretas (`=`), sem função envolvida.

O `JOIN FETCH ... rankedStats` (obrigatório, não `LEFT`) já resolve sozinho a regra "jogador Unranked não entra no leaderboard" — sem `RankedStats`, a linha nem aparece no resultado.

## `RankingEntryResponse`

```text
position                 // calculado pelo backend, 1-based, considerando a página atual
nickname                 // RiotAccount.gameName
tagLine                  // RiotAccount.tagLine (nickname sozinho não é garantidamente único)
tier
division
leaguePoints
wins
losses
winRate                  // calculado via WinRateCalculator a partir de wins/losses do RankedStats
lane                     // PlayerStats.primaryRole, null se ainda não houver partidas importadas
mostPlayedChampionId     // PlayerStats.mostPlayedChampionId, null pelo mesmo motivo
```

`wins`/`losses`/`winRate` vêm do `RankedStats` (contagem oficial da Riot para a fila, sempre completa), não do `PlayerStats` (que reflete só as partidas que este backend conseguiu importar, limitado pela etapa 12). `lane`/`mostPlayedChampionId` só existem no `PlayerStats`, então vêm de lá.

## Contrato HTTP

```http
GET /api/ranking?page=0&size=20&search=raposa&courseId=1&role=MID&tier=GOLD&championId=103
```

Todos os parâmetros são opcionais e combináveis. Resposta `200 OK`, formato padrão de página do Spring (`content`, `totalElements`, `totalPages`, `number`, `size`, ...), com `content` sendo uma lista de `RankingEntryResponse`.

Público — adicionar em `SecurityConfig`:

```text
GET /api/ranking → público
```

## Testes mínimos

- `RankCalculator`: tier maior vence, independentemente de LP/divisão;
- `RankCalculator`: mesma tier, divisão maior (`I` > `IV`) vence;
- `RankCalculator`: mesma tier e divisão, mais LP vence;
- `RankCalculator`: `MASTER`/`GRANDMASTER`/`CHALLENGER` (sem divisão real) ainda comparam corretamente por tier e LP;
- `RankingService`: calcula a posição corretamente considerando a página (`page=1&size=20` começa em 21, não em 1);
- `RankingService`: aplica cada filtro (search, courseId, role, tier, championId) isoladamente e em combinação;
- `RankingService`: jogador sem `RankedStats` nunca aparece no resultado, mesmo que `rankingEligible = true`;
- `RankedSyncService`: primeira sincronização com uma entrada Solo/Duo marca `user.rankingEligible = true`;
- `RankedSyncService`: uma segunda sincronização (atualizando `RankedStats` já existente) não sobrescreve `rankingEligible` se o usuário já tiver saído manualmente do ranking.

## Critérios de aceite

- `GET /api/ranking` é acessível sem autenticação;
- a ordenação segue exatamente tier → divisão → LP → win rate → vitórias;
- jogadores Unranked, inativos, com e-mail não verificado ou que saíram manualmente do ranking nunca aparecem;
- nenhum dado privado (`puuid`, e-mail, curso do usuário, id interno) vaza na resposta;
- os testes definidos para esta etapa passam.
