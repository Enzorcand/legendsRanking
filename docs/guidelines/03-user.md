# Etapa 03 — User

## Objetivo

Consolidar o modelo de usuário da comunidade PUCRS e seu vínculo obrigatório com um curso, deixando-o pronto para ser usado pelo cadastro e pela autenticação na etapa seguinte.

## Escopo

Implementar apenas:

- entidade JPA `User` alinhada à tabela `users`;
- relacionamento obrigatório `User N — 1 Course`;
- relacionamento `User 1 — 1 RiotAccount` no modelo JPA;
- repositório de usuário;
- serviço de domínio para consultas e validações internas simples;
- DTOs internos que não exponham senha nem identificadores Riot;
- testes unitários e de persistência relevantes.

Não implementar nesta etapa:

- `POST /api/auth/register`, login, JWT, logout ou recuperação de senha;
- envio/verificação de e-mail;
- endpoints `GET /api/me`, `PATCH /api/me` ou remoção do ranking;
- vínculo ou sincronização de conta Riot;
- administração de usuários.

Essas funcionalidades dependem de uma identidade autenticada e pertencem às etapas de autenticação, perfil e Riot.

## Banco de dados e migrations

A tabela `users` já foi criada na migration V1 e contém os campos necessários para este estágio.

**Não editar a V1.** Como ela já foi aplicada, qualquer alteração real de schema deve entrar em uma migration `V2__<descricao>.sql` ou posterior.

Não criar migration se o trabalho se limitar ao mapeamento JPA dos campos existentes.

## Mapeamento esperado

| Coluna | Campo Java sugerido | Regra |
| --- | --- | --- |
| `id` | `Long id` | `@Id` e `@GeneratedValue` |
| `course_id` | `Course course` | `@ManyToOne(fetch = LAZY)`, obrigatório |
| `full_name` | `String fullName` | obrigatório |
| `email` | `String email` | obrigatório, único e normalizado em minúsculas |
| `password_hash` | `String passwordHash` | nunca é devolvido em DTO ou logs |
| `email_verified` | `boolean emailVerified` | inicia como `false` |
| `is_active` | `boolean active` | inicia como `true` |
| `ranking_eligible` | `boolean rankingEligible` | inicia como `false` |
| `created_at` | `LocalDateTime createdAt` | preenchido na persistência |
| `updated_at` | `LocalDateTime updatedAt` | atualizado em cada alteração |

Usar nomes Java em *camelCase* e `@Column(name = "...")` para mapear as colunas em *snake_case* explicitamente.

## Relacionamento Riot

O schema tem `riot_accounts.user_id` com `UNIQUE`, portanto o modelo deve refletir uma conta Riot por usuário:

```text
User 1 ─── 1 RiotAccount
```

Modelar `User.riotAccount` como `@OneToOne(mappedBy = "user")` e `RiotAccount.user` como `@OneToOne` com a coluna `user_id`. Não manter uma coleção de contas Riot no MVP.

## Estrutura sugerida

```text
portfolio.pucrs.user
├── dto
│   └── UserSummaryResponse
├── entity
│   └── User
├── repository
│   └── UserRepository
└── service
    └── UserService
```

O pacote `user` não deve depender de controller nesta etapa. Endpoints de perfil só serão adicionados quando existir autenticação.

## Repositório e serviço

O repositório deve oferecer somente consultas necessárias ao domínio, como:

```text
existsByEmailIgnoreCase(email)
findByEmailIgnoreCase(email)
```

O serviço deve:

- normalizar e-mail com `trim().toLowerCase(Locale.ROOT)` antes de consultar;
- concentrar a busca por e-mail;
- manter regras de estado fora de controllers;
- não receber nem retornar senha em texto puro.

Validação do domínio de e-mail institucional e hashing de senha serão aplicados no fluxo de cadastro da etapa de autenticação, por exigirem configuração e tokenização próprios.

## DTOs e privacidade

Não expor a entidade `User` em nenhuma resposta HTTP. Mesmo DTOs internos não devem incluir:

- `passwordHash`;
- e-mail, exceto quando o consumidor for o próprio usuário autenticado em uma etapa futura;
- PUUID ou qualquer campo de `RiotAccount`;
- flags internas que não façam parte do contrato.

## Testes mínimos

- repositório: busca/verificação de e-mail sem diferenciar maiúsculas e minúsculas;
- serviço: e-mail é normalizado antes da consulta;
- entidade: `createdAt` e `updatedAt` são preenchidos; `updatedAt` é alterado em atualização;
- persistência: um usuário sem curso não é aceito;
- persistência: duas contas Riot não podem ser vinculadas ao mesmo usuário;
- regressão: aplicação inicia com `spring.jpa.hibernate.ddl-auto=validate`.

Usar Testcontainers + PostgreSQL quando a infraestrutura de testes de integração for introduzida. Não usar H2 como substituto do PostgreSQL para constraints ou migrations.

## Critérios de aceite

- `User` e `RiotAccount` refletem as cardinalidades do schema;
- campos privados não estão em DTOs;
- nenhum endpoint de autenticação foi antecipado;
- nenhuma migration aplicada foi editada;
- a aplicação continua iniciando e validando o schema;
- os testes unitários desta etapa passam.
