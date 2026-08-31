# Etapa 02 — Course

## Objetivo

Disponibilizar a leitura pública dos cursos ativos que poderão ser escolhidos no cadastro de usuários.

## Escopo

Implementar apenas:

- entidade JPA `Course`;
- repositório para consultas de curso;
- serviço de listagem;
- endpoint público `GET /api/courses`;
- DTO público de resposta;
- testes da regra de listagem.

Não implementar nesta etapa:

- criação, edição ou exclusão de cursos por API;
- painel administrativo;
- vínculo de curso ao fluxo de cadastro;
- autenticação JWT;
- integração com qualquer sistema acadêmico.

## Banco de dados

A tabela `courses` já existe na migration `V1__create_core_schema.sql`, aplicada no banco de desenvolvimento.

**Não editar a V1.** Migrations aplicadas são imutáveis. Se houver necessidade de alterar o schema, criar uma nova migration `V2__<descricao>.sql`.

Mapeamento esperado:

| Coluna | Tipo Java sugerido | Regra |
| --- | --- | --- |
| `id` | `Long` | chave gerada pelo banco |
| `name` | `String` | obrigatório e único |
| `active` | `boolean` | somente cursos ativos são retornados |
| `created_at` | `LocalDateTime` | preenchido pelo banco |
| `updated_at` | `LocalDateTime` | preenchido pelo banco |

## Estrutura sugerida

```text
portfolio.pucrs.course
├── controller
│   └── CourseController
├── dto
│   └── CourseResponse
├── entity
│   └── Course
├── repository
│   └── CourseRepository
└── service
    └── CourseService
```

Não retornar a entidade JPA diretamente pelo controller.

## Contrato HTTP

```http
GET /api/courses
```

Resposta `200 OK`:

```json
[
  {
    "id": 1,
    "name": "Ciência da Computação"
  }
]
```

Regras:

- retornar somente registros com `active = true`;
- ordenar por `name` em ordem alfabética crescente;
- retornar `[]` quando não houver cursos ativos;
- não expor `active`, `createdAt` ou `updatedAt` neste endpoint.

## Persistência e serviço

O repositório deve declarar uma consulta expressiva, por exemplo `findAllByActiveTrueOrderByNameAsc()`.

O serviço deve ser `@Transactional(readOnly = true)`, converter as entidades em `CourseResponse` e concentrar a regra de listagem. O controller deve apenas delegar a requisição e responder HTTP.

## Segurança temporária

O endpoint é público por requisito. Enquanto o módulo de autenticação não existe, adicionar uma configuração mínima de segurança que permita sem login apenas:

```text
GET /api/courses
```

Essa configuração será expandida na etapa de autenticação para JWT, autorização e rate limiting. Não usar a senha temporária gerada pelo Spring como solução de autenticação.

## Testes mínimos

- `CourseService`: cursos inativos não são retornados;
- `CourseService`: resultado é ordenado pelo nome;
- `CourseService`: lista vazia é válida;
- controller: `GET /api/courses` responde `200` e contém somente `id` e `name`;
- integração com PostgreSQL/Testcontainers, quando a infraestrutura de testes for adicionada.

## Critérios de aceite

- a aplicação inicia com `spring.jpa.hibernate.ddl-auto=validate`;
- `GET /api/courses` é acessível sem autenticação;
- a resposta não expõe entidades ou campos internos;
- não há mudanças no schema fora de uma nova migration;
- os testes definidos para a regra de listagem passam.
