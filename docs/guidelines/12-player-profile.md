# Etapa 15 — Perfil público / autenticado

## Objetivo

Expor `GET /api/players/{id}`, acessível com ou sem autenticação, retornando **conjuntos de dados diferentes** dependendo se quem chama está autenticado — a regra de privacidade mais explícita do spec do projeto: `PublicPlayerDTO` para visitante anônimo, `AuthenticatedPlayerDTO` para qualquer usuário logado (não só o dono do perfil).

## Correção de escopo encontrada nesta etapa (etapa 14)

Escrevendo esta guideline, percebi que `RankingEntryResponse` (etapa 14, `GET /api/ranking`, público) está expondo `tagLine` — mas o spec separa explicitamente:

- **Público**: "nickname" (o nome de exibição, `gameName`);
- **Somente autenticado**: "Riot ID" (o identificador completo `gameName#tagLine`, terminologia oficial da própria Riot).

Isso corrige o `RankingEntryResponse`: `tagLine` sai do DTO público do ranking (que é 100% anônimo, sem opção de autenticação). Não achei outro vazamento parecido — `RiotAccountResponse` (etapa 10) e `MeResponse.riotAccount` (etapa 08) só são vistos pelo próprio dono autenticado da conta, então expor `tagLine` ali continua correto.

## Escopo

Implementar apenas:

- `GET /api/players/{id}` — `{id}` é o `User.id` (mesma identidade de `/api/me`, não o `RiotAccount.id`, que nunca foi decidido/exposto até agora);
- `PublicPlayerResponse` e `AuthenticatedPlayerResponse` como classes **totalmente separadas** (não uma com campos condicionalmente nulos) — reforça estruturalmente que dado privado não pode vazar por engano;
- `PlayerProfileService`, que monta o DTO certo dependendo se a chamada está autenticada;
- posição no ranking do jogador (reaproveitando o mesmo algoritmo de ordenação do `RankingService`, sem duplicar a lógica);
- correção do `RankingEntryResponse` descrita acima;
- testes unitários do serviço e do controller.

Não implementar nesta etapa:

- `GET /api/players/{id}/matches` (histórico) — etapa 16;
- edição de perfil por aqui — isso é `PATCH /api/me` (etapa 08), e só o próprio dono pode editar o próprio perfil;
- `icon` — mesma lacuna já documentada na etapa 14 (não existe cliente para `summoner-v4` ainda);
- cache da posição no ranking — reaproveita a mesma leitura "on demand" já usada em `/api/ranking`.

## Regra de autenticação "opcional"

O endpoint é público (`permitAll` no `SecurityConfig`), mas o comportamento muda se a requisição chega com um JWT válido. Isso é diferente de todo endpoint autenticado feito até agora (que exige token ou nada). O parâmetro `Authentication` sempre chega preenchido — anônimo quando não há token (`AnonymousAuthenticationToken`, principal `"anonymousUser"`) ou com o `Long userId` de sempre quando há um token válido (`JwtAuthenticationFilter`, etapa 06).

Adicionar `AuthenticatedUser.optionalId(Authentication)` (retorna `Optional<Long>`, vazio para anônimo) — `AuthenticatedUser.id(...)` continua existindo para os endpoints que exigem autenticação e podem confiar no cast direto.

## Quando o perfil existe

`GET /api/players/{id}` retorna `404` quando:

- não existe `User` com esse id;
- o `User` está com `is_active = false`;
- o `User` não tem `RiotAccount` vinculada — sem ela não há identidade pública nenhuma para mostrar (nome completo é privado, e o "nickname" público *é* o `gameName` da conta Riot).

Não depende de `email_verified` nem de `ranking_eligible` — um jogador pode ter um perfil visível mesmo sem aparecer no ranking (Unranked, ou que saiu manualmente do ranking).

## `PublicPlayerResponse`

```text
nickname                 // RiotAccount.gameName
ranked                    // false quando não há RankedStats — "Unranked"
tier                       // null quando ranked = false
division
leaguePoints
wins
losses
winRate
lane                       // PlayerStats.primaryRole, pode ser null mesmo com ranked = true
mostPlayedChampionId
position                   // null quando o jogador não aparece no /api/ranking agora
```

## `AuthenticatedPlayerResponse`

Tudo do `PublicPlayerResponse`, mais:

```text
id                          // User.id
fullName
tagLine                     // completa o Riot ID junto com o nickname já público
courseName
```

Composição, não duplicação de campos: `AuthenticatedPlayerResponse` embute um `PublicPlayerResponse` (`profile`) em vez de repetir os mesmos onze campos.

## Posição no ranking

Reaproveitar a mesma consulta + `Comparator` do `RankingService` (etapa 14), sem paginação, procurando o índice do `RiotAccount` do jogador. Extrair a busca+ordenação para um método privado compartilhado dentro de `RankingService` (`getRanking` e o novo `findPosition` usam o mesmo), em vez de duplicar a lógica de filtro/ordenação em outro lugar.

## Contrato HTTP

```http
GET /api/players/42
```

Sem header `Authorization` → `200 OK` com `PublicPlayerResponse`.
Com `Authorization: Bearer <token>` válido (de qualquer usuário logado, não precisa ser o dono do perfil) → `200 OK` com `AuthenticatedPlayerResponse`.
`404` quando o perfil não existe pelas regras acima.

Adicionar em `SecurityConfig`:

```text
GET /api/players/{id} → público
```

## Testes mínimos

- `PlayerProfileService`: retorna `PublicPlayerResponse` sem campos privados quando não autenticado;
- `PlayerProfileService`: retorna `AuthenticatedPlayerResponse` com `fullName`/`tagLine`/`courseName` quando autenticado;
- `PlayerProfileService`: jogador sem `RankedStats` aparece com `ranked = false` e `position = null`, sem lançar erro;
- `PlayerProfileService`: jogador que está no ranking recebe a `position` correta (mesmo valor que apareceria em `/api/ranking`);
- `PlayerProfileService`: lança `PlayerNotFoundException` para id inexistente, usuário inativo, ou usuário sem `RiotAccount`;
- controller: `GET /api/players/{id}` delega para o serviço passando `Optional<Long>` de acordo com o `Authentication` recebido.

## Critérios de aceite

- visitante anônimo nunca recebe `fullName`, `tagLine` ou `courseName`;
- `puuid` nunca aparece em nenhum dos dois DTOs;
- a posição reportada aqui é sempre consistente com `/api/ranking`;
- `RankingEntryResponse` não expõe mais `tagLine`;
- os testes definidos para esta etapa passam.
