# Etapa 16 — Histórico de partidas

## Objetivo

Expor `GET /api/players/{id}/matches`, paginado, com as partidas Ranked Solo/Duo já sincronizadas (etapa 12) de um jogador, da mais recente para a mais antiga.

## Escopo

Implementar apenas:

- consulta paginada em `MatchRepository` (`findAllByRiotAccountIdOrderByGameStartDesc`) — diferente do ranking (etapa 14), aqui a ordenação é só por uma coluna (`game_start`), então o banco pagina e ordena sozinho, sem precisar do truque de ordenar em memória;
- `MatchHistoryEntryResponse` — DTO de uma partida;
- `MatchHistoryService`, que pagina e converte;
- `GET /api/players/{id}/matches` em `PlayerController` (mesmo controller da etapa 15, já que o path é aninhado em `/api/players/{id}`);
- reaproveitar a regra de "perfil existe" da etapa 15 (`id` inválido, usuário inativo ou sem `RiotAccount` → `404`) — expor um método público em `PlayerProfileService` para isso, em vez de duplicar a checagem;
- testes unitários do serviço e do controller.

Não implementar nesta etapa:

- filtros no histórico (por campeão, por resultado, etc.) — não foi pedido, só paginação;
- detalhe expandido da partida (outros participantes, itens, runas) — o `Match` armazenado (etapa 12) só guarda os campos do próprio jogador, de propósito;
- distinção público/autenticado — diferente do perfil (etapa 15), os dados de uma partida individual (campeão, lane, KDA, resultado) já são da mesma natureza dos dados agregados que o spec já definiu como públicos; não há campo privado aqui para esconder.

## Contrato HTTP

```http
GET /api/players/5/matches?page=0&size=20
```

Resposta `200 OK`, formato padrão de página do Spring, `content` como lista de `MatchHistoryEntryResponse`:

```text
matchId          // riot_match_id
gameStart
gameDuration
win
championId
lane              // Match.role
kills
deaths
assists
cs
```

`404` seguindo exatamente a mesma regra da etapa 15 (perfil inexistente/inativo/sem conta Riot). Público — já coberto pelo `permitAll` de `/api/players/**` adicionado na etapa 15, nenhuma mudança de segurança nova.

## Testes mínimos

- `MatchHistoryService`: retorna as partidas ordenadas da mais recente para a mais antiga, paginadas;
- `MatchHistoryService`: conta sem partidas retorna página vazia, não erro;
- controller: `GET /api/players/{id}/matches` propaga `404` quando o perfil não existe (reaproveitando a checagem da etapa 15);
- controller: parâmetros de paginação (`page`, `size`) chegam corretamente ao serviço.

## Critérios de aceite

- histórico paginado corretamente, mais recente primeiro;
- mesma regra de "perfil inexistente" da etapa 15, sem duplicar a lógica;
- os testes definidos para esta etapa passam.
