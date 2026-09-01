# AAAC Ranking — API

Backend do ranking de League of Legends da PUCRS, mantido pelo Diretório de E-sports da AAAC Raposas. Java 21 + Spring Boot + PostgreSQL + Flyway.

## Pré-requisitos

- JDK 21 (o Gradle Wrapper cuida do resto — não precisa instalar Gradle)
- Docker Desktop (para o PostgreSQL local)
- Uma [Riot API key](https://developer.riotgames.com/) (necessária a partir das funcionalidades que integram com a Riot; a aplicação sobe sem ela, mas essas chamadas falham)

## 1. Configurar variáveis de ambiente

Copie o exemplo e ajuste os valores:

```bash
cp .env.example .env
```

Edite `.env` e defina pelo menos:

- `POSTGRES_PASSWORD` — senha do banco local;
- `RIOT_API_KEY` — sua chave da Riot Games API.

O `.env` é lido pelo `docker-compose.yml` automaticamente. Ele **não** é lido pelo Spring Boot quando você roda a aplicação fora do Docker (`bootRun`/`test` direto no host) — nesses casos exporte as variáveis no shell antes (veja o passo 3).

## 2. Subir o banco de dados

Você só precisa do container do Postgres para rodar a aplicação/testes localmente (o container `api` do compose é para rodar a aplicação inteirinha em Docker, veja a seção correspondente abaixo):

```bash
docker compose --env-file .env up -d db
```

Aguarde o healthcheck ficar `healthy`:

```bash
docker compose ps
```

As migrations do Flyway rodam automaticamente quando a aplicação sobe — não é preciso aplicar nada manualmente.

## 3. Rodar a aplicação (fora do Docker)

Exporte as variáveis do `.env` no shell atual e suba a aplicação:

```bash
set -a && source .env && set +a
./gradlew bootRun
```

No Windows (PowerShell), exporte cada variável do `.env` manualmente ou use um utilitário como `dotenv`/`Set-Item Env:` antes de rodar:

```powershell
Get-Content .env | ForEach-Object {
    if ($_ -match '^\s*([^#=]+)=(.*)$') {
        Set-Item -Path "Env:$($matches[1].Trim())" -Value $matches[2].Trim()
    }
}
./gradlew.bat bootRun
```

A API sobe em `http://localhost:8080` (ou na porta definida em `API_PORT`).

## 4. Rodar os testes

Com o container `db` no ar (etapa 2), exporte as variáveis e rode:

```bash
set -a && source .env && set +a
./gradlew test
```

`PucrsApplicationTests#contextLoads` precisa de um Postgres real acessível — sem o container rodando (ou sem as variáveis exportadas), esse teste falha enquanto os demais (unitários, com Mockito) continuam passando normalmente.

## Rodando tudo via Docker Compose

Para subir API + banco inteiramente em containers:

```bash
docker compose --env-file .env up -d
```

Isso builda a imagem a partir do `Dockerfile` e injeta as variáveis de conexão automaticamente no container `api` — não precisa exportar nada manualmente nesse caminho.

Para parar:

```bash
docker compose down
```

Para derrubar e também apagar os dados do Postgres (recriar o banco do zero):

```bash
docker compose down -v
```

## Variáveis de ambiente

| Variável | Uso | Obrigatória |
| --- | --- | --- |
| `POSTGRES_DB` / `POSTGRES_USER` / `POSTGRES_PASSWORD` / `POSTGRES_PORT` | Conexão com o Postgres local | Sim |
| `API_PORT` | Porta exposta pela API no `docker compose up` completo | Não (default `8080`) |
| `SPRING_PROFILES_ACTIVE` | Perfil Spring (`dev` por padrão) | Não |
| `RIOT_API_KEY` / `RIOT_PLATFORM` / `RIOT_CONTINENT` | Integração com a Riot Games API | `RIOT_API_KEY` sim para os endpoints de conta Riot |
| `JWT_SECRET` / `JWT_EXPIRATION_MINUTES` | Assinatura e expiração do token JWT | Não (tem default de desenvolvimento) |
| `MAIL_HOST` / `MAIL_PORT` / `MAIL_USERNAME` / `MAIL_PASSWORD` / `MAIL_SMTP_AUTH` / `MAIL_SMTP_STARTTLS` / `MAIL_FROM` | Envio do e-mail de verificação de cadastro | Sim para o fluxo de verificação de e-mail funcionar de fato |

> Sem um servidor SMTP configurado, o cadastro (`POST /api/auth/register`) continua funcionando, mas o envio do código de verificação falha silenciosamente na integração de e-mail. Para testar esse fluxo localmente sem uma conta SMTP real, suba um servidor de teste como o [Mailpit](https://github.com/axllent/mailpit) e aponte `MAIL_HOST`/`MAIL_PORT` para ele.

## Endpoints disponíveis hoje

```text
POST   /api/auth/register              (público)
POST   /api/auth/login                 (público)
POST   /api/auth/verify-email          (público)
POST   /api/auth/resend-verification   (público)

GET    /api/courses                    (público)

POST   /api/me/riot-account/link       (autenticado)
DELETE /api/me/riot-account/unlink     (autenticado)
```

O restante do endpoint map (perfil, ranking, partidas, etc.) ainda está em planejamento — veja `docs/guidelines/`.

## Estrutura do projeto

- `docs/guidelines/` — planejamento etapa a etapa do backend (escopo, contrato HTTP, critérios de aceite de cada funcionalidade);
- `src/main/resources/db/migration/` — migrations do Flyway, numeradas e imutáveis depois de aplicadas;
- `src/main/java/portfolio/pucrs/` — código-fonte, organizado por domínio (`auth`, `user`, `course`, `riot`, `common`).

## Troubleshooting

- **`contextLoads` falhando com erro de `DataSource`**: o container `db` não está rodando, ou as variáveis do `.env` não foram exportadas no shell antes do `gradlew`.
- **`FATAL: password authentication failed`**: o volume `postgres-data` já existe com uma senha diferente da atual no `.env` (o Postgres só aplica `POSTGRES_PASSWORD` na primeira inicialização do volume). Recrie o volume local (isso apaga os dados do Postgres local):
  ```bash
  docker compose down -v
  docker compose --env-file .env up -d db
  ```
- **Docker Desktop não está rodando**: abra o Docker Desktop e aguarde o ícone indicar que o engine está pronto antes de rodar `docker compose`.
