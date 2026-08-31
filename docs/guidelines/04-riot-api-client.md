# Etapa 09 — RiotApiClient

## Objetivo

Criar um módulo isolado responsável por toda comunicação HTTP com a Riot Games API, servindo de base para os serviços de domínio (`RiotAccountService`, `RankedSyncService`, `MatchSyncService`) das etapas seguintes.

## Escopo

Implementar apenas:

- cliente HTTP isolado `RiotApiClient` (pacote `riot.client`);
- DTOs de resposta da Riot API (`riot.client.dto`);
- configuração de propriedades (`RiotApiProperties`) e do `RestClient` usado pelo cliente;
- exceções específicas de integração com a Riot API;
- testes unitários do cliente com servidor HTTP simulado.

Não implementar nesta etapa:

- persistência ou vínculo de `RiotAccount` a um `User`;
- endpoints `POST/DELETE /api/me/riot-account*`;
- `RiotAccountService`, `RankedSyncService`, `MatchSyncService` ou qualquer lógica de domínio;
- scheduler de sincronização;
- chamadas HTTP à Riot API a partir de controllers.

Essas funcionalidades pertencem às etapas 10 a 13 e ao scheduler (etapa 18), e dependem deste cliente já existir.

## Responsabilidades do RiotApiClient

- buscar conta por Riot ID (`gameName` + `tagLine`) — roteamento continental (`account-v1`);
- buscar conta por PUUID — roteamento continental (`account-v1`);
- obter entradas ranked por PUUID — roteamento de plataforma (`league-v4`), sem filtrar fila aqui (a etapa de domínio decide o que é Solo/Duo);
- obter IDs de partidas por PUUID, com filtros opcionais de fila e `startTime` para suportar sincronização incremental — roteamento continental (`match-v5`);
- obter detalhes de uma partida por `matchId` — roteamento continental (`match-v5`).

Região fixa no MVP: plataforma `br1`, continente `americas`, configuráveis via propriedades para não hardcodar o roteamento dentro do cliente.

## Configuração

Propriedades novas (prefixo `riot`):

```properties
riot.api-key=${RIOT_API_KEY:}
riot.platform=${RIOT_PLATFORM:br1}
riot.continent=${RIOT_CONTINENT:americas}
```

`RIOT_API_KEY` deve ser definido via variável de ambiente/`.env`, nunca commitado com valor real. A key nunca deve aparecer em logs.

## Estrutura sugerida

```text
portfolio.pucrs.riot
├── client
│   ├── RiotApiClient
│   ├── RiotApiClientConfig
│   ├── RiotApiProperties
│   └── dto
│       ├── LeagueEntryDto
│       ├── MatchDto
│       └── RiotAccountDto
└── exception
    ├── RiotAccountNotFoundException
    └── RiotApiException
```

O pacote `riot.client` não deve depender de `riot.entity`, controllers ou repositórios. Ele apenas traduz chamadas HTTP em DTOs simples.

## Tratamento de erros

- qualquer resposta HTTP de erro é traduzida para `RiotApiException` (status + mensagem), sem vazar a API key;
- um 404 ao buscar conta por Riot ID/PUUID é traduzido para `RiotAccountNotFoundException`, que os serviços de domínio tratam como "conta inexistente" em vez de falha de infraestrutura.

## Testes mínimos

- `getAccountByRiotId` retorna o DTO esperado para uma resposta 200;
- `getAccountByRiotId` lança `RiotAccountNotFoundException` para 404;
- `getLeagueEntriesByPuuid` retorna lista vazia quando o jogador é unranked (resposta `[]`);
- `getMatchIdsByPuuid` envia os parâmetros de fila/`startTime` na query string quando informados;
- `getMatchById` retorna um `MatchDto` com os campos necessários ao cálculo de estatísticas (fila, participantes, resultado).

Usar `MockRestServiceServer` para simular a Riot API; não realizar chamadas reais nos testes.

## Critérios de aceite

- nenhuma chamada HTTP à Riot API ocorre fora de `riot.client`;
- a API key nunca é logada nem exposta em exceções;
- o cliente não conhece `User`, `RiotAccount` ou qualquer entidade JPA;
- os testes do cliente passam sem acessar a rede real.
