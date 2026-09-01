# Etapa 08 — Perfil

## Objetivo

Dar ao usuário autenticado acesso e controle sobre seu próprio perfil: consultar seus dados privados, editar nome e curso, e sair do ranking preservando o histórico.

Esta etapa não expõe nada publicamente — o perfil público (`GET /api/players/{id}`) é a etapa 15 e usa um DTO próprio, sem reaproveitar `MeResponse`.

## Escopo

Implementar apenas:

- `MeController` com `GET /api/me`, `PATCH /api/me` e `DELETE /api/me/ranking`;
- `MeResponse` — DTO privado com os dados que só o próprio usuário pode ver;
- `UpdateMeRequest` — DTO de edição (nome e curso);
- métodos novos em `UserService` para buscar o perfil completo, aplicar a edição e desativar a participação no ranking;
- um helper único para extrair o `userId` autenticado a partir do `Authentication` (hoje duplicado como `(Long) authentication.getPrincipal()` dentro de `RiotAccountController`) — pequena extração, não uma camada nova, só para não repetir o cast em cada controller de `/api/me/**`;
- testes unitários de serviço e controller.

Não implementar nesta etapa:

- alteração de e-mail ou senha pelo `PATCH /api/me` (fora do escopo definido pelo projeto: "usuário pode editar nome e curso");
- exclusão de conta (`is_active = false` ou hard delete) — não foi pedido, só saída do ranking;
- reativação do ranking depois de um `DELETE /api/me/ranking` — a regra descreve remoção, não um toggle; se for necessário reativar, é uma decisão de produto para revisitar depois;
- perfil público (`GET /api/players/{id}`, etapa 15) ou histórico de partidas (`GET /api/players/{id}/matches`, etapa 16);
- qualquer edição de `RiotAccount` por aqui — isso já existe em `/api/me/riot-account/link` e `/unlink` (etapa 10).

## Banco de dados

Nenhuma migration nova. `users.ranking_eligible` e `users.is_active` já existem desde a `V1`. `DELETE /api/me/ranking` apenas seta `ranking_eligible = false`; é idempotente (chamar de novo com o valor já `false` não é erro).

## Estrutura sugerida

```text
portfolio.pucrs.user
├── controller
│   └── MeController
├── dto
│   ├── MeResponse
│   ├── UpdateMeRequest
│   └── UserSummaryResponse      (já existe, não mexer)
├── entity
│   └── User                     (não mexer nesta etapa)
├── repository
│   └── UserRepository
└── service
    └── UserService              (adicionar os métodos novos)

portfolio.pucrs.common.security
└── AuthenticatedUser            (helper: extrai o userId do Authentication)
```

`RiotAccountController` deve passar a usar o mesmo helper, para não manter duas formas de resolver o usuário autenticado no código.

## `MeResponse`

Campos sugeridos (dados privados, nunca expostos por endpoints públicos):

```text
id
fullName
email
course { id, name }
emailVerified
rankingEligible
riotAccount { gameName, tagLine, region }   // null se não houver conta vinculada
```

O bloco `riotAccount`, quando presente, expõe apenas o que já é público-adjacente (`gameName`/`tagLine`/`region`) — nunca `puuid`, seguindo a mesma regra de privacidade já aplicada em `RiotAccountResponse` (etapa 10).

## Contrato HTTP

```http
GET /api/me
Authorization: Bearer <token>
```

Resposta `200 OK` com `MeResponse`.

```http
PATCH /api/me
Authorization: Bearer <token>
Content-Type: application/json

{
  "fullName": "Ana Raposa",
  "courseId": 2
}
```

Resposta `200 OK` com o `MeResponse` atualizado. Os dois campos são opcionais — aplicar somente o que vier preenchido; enviar os dois `null`/ausentes é um `400`. `courseId` inválido ou de curso inativo reaproveita `InvalidCourseException` (já usada no cadastro).

```http
DELETE /api/me/ranking
Authorization: Bearer <token>
```

Resposta `204 No Content`.

Todos exigem autenticação. Como `anyRequest().authenticated()` já cobre qualquer rota não listada em `SecurityConfig`, nenhuma mudança de segurança é necessária além de garantir que essas três rotas não acabem, por engano, em alguma lista de `permitAll`.

## Testes mínimos

- `UserService`: monta `MeResponse` completo, incluindo o caso sem `RiotAccount` vinculada (`riotAccount = null`);
- `UserService`: atualiza apenas o nome quando só o nome é enviado, preservando o curso;
- `UserService`: atualiza apenas o curso quando só o curso é enviado;
- `UserService`: rejeita `courseId` inexistente ou inativo;
- `UserService`: `PATCH` sem nenhum campo preenchido é rejeitado;
- `UserService`: sair do ranking seta `rankingEligible = false` e é idempotente;
- controller: `MeController` delega corretamente para o `UserService` usando o `userId` do `Authentication` (mesmo padrão de teste direto por método já usado em `RiotAccountControllerTest`).

## Critérios de aceite

- usuário autenticado consegue ver, editar (nome/curso) e sair do ranking pelos três endpoints;
- nenhum dado privado (`email`, `puuid`, flags internas) vaza para fora de `/api/me`;
- `RiotAccountController` e `MeController` resolvem o usuário autenticado da mesma forma;
- nenhuma etapa futura (perfil público, histórico, ranking) foi antecipada;
- os testes definidos para esta etapa passam.
