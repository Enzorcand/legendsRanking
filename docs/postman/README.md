# Testando o fluxo completo no Postman

Este guia cobre o fluxo de ponta a ponta descrito no plano do projeto: cadastro → verificação de e-mail → login → perfil → vínculo de conta Riot. Os arquivos [AAAC-Ranking.postman_collection.json](AAAC-Ranking.postman_collection.json) e [AAAC-Ranking.postman_environment.json](AAAC-Ranking.postman_environment.json) desta pasta já trazem as requisições nessa ordem, prontas para importar.

## Pré-requisitos

1. **API rodando localmente** — veja o [README.md](../../README.md) principal (`docker compose up -d db` + `./gradlew bootRun`, ou tudo em Docker).
2. **Um servidor SMTP alcançável em `MAIL_HOST`/`MAIL_PORT`** — o cadastro (`POST /api/auth/register`) chama o envio do e-mail de verificação de forma síncrona; se não houver um SMTP respondendo, o cadastro falha com erro 500. Para testar sem uma conta de e-mail real, suba o [Mailpit](https://github.com/axllent/mailpit) localmente:
   ```bash
   docker run -d --name mailpit -p 1025:1025 -p 8025:8025 axllent/mailpit
   ```
   Isso expõe SMTP em `localhost:1025` (que já é o default de `MAIL_HOST`/`MAIL_PORT` no `.env.example`) e uma UI web em `http://localhost:8025` para ler os e-mails capturados.
3. **`RIOT_API_KEY` válida** — só é necessária no passo de vincular a conta Riot (etapa 4 da coleção). O resto do fluxo funciona sem ela.
4. **Um curso ativo cadastrado no banco** — a listagem de cursos (etapa 1) precisa retornar pelo menos um resultado. Se a tabela `courses` estiver vazia, insira um curso manualmente antes de começar:
   ```sql
   INSERT INTO courses (name, active) VALUES ('Ciência da Computação', true);
   ```

## Importar no Postman

1. Postman → **Import** → selecione os dois arquivos `.json` desta pasta.
2. No canto superior direito, selecione o ambiente **"AAAC Ranking - Local"**.
3. Ajuste `baseUrl` no ambiente se a API não estiver em `http://localhost:8080`.

## Passo a passo

Execute as requisições na ordem das pastas (1 → 2 → 3 → 4). Cada uma tem uma nota sobre o que observar.

### 1. Cursos → "Listar cursos ativos"
`GET /api/courses` — público, sem token. Um script de teste já captura o `id` do primeiro curso da resposta e salva na variável `courseId`, usada no cadastro. Confirme que a resposta não é uma lista vazia.

### 2. Autenticação

1. **Registrar usuário** — `POST /api/auth/register`. Usa `{{email}}`/`{{password}}`/`{{courseId}}` do ambiente. Deve responder `201` com `{ id, fullName }`. Isso já dispara o envio do código de verificação por e-mail.
2. Abra o Mailpit (`http://localhost:8025`) e localize o e-mail "AAAC Ranking - Codigo de verificacao" enviado para `{{email}}`. Copie o código de 6 dígitos.
3. Na variável de ambiente **`verificationCode`**, cole o código copiado.
4. **Verificar e-mail** — `POST /api/auth/verify-email`. Deve responder `200`. Rodar de novo com o mesmo código deve dar `400` (código já consumido) — útil para confirmar que o uso único está funcionando.
5. **Reenviar código de verificação (opcional)** — `POST /api/auth/resend-verification`. Sempre responde `202`, mesmo que o e-mail já esteja verificado (não deve chegar um novo e-mail nesse caso — confirme no Mailpit).
6. **Login** — `POST /api/auth/login`. Deve responder `200` com o token JWT. Um script de teste salva esse token na variável `token`, usada em todas as requisições autenticadas daqui pra frente.

### 3. Perfil

1. **Ver meu perfil** — `GET /api/me`. Deve mostrar `emailVerified: true`, `rankingEligible: false` e `riotAccount: null`.
2. **Editar nome/curso** — `PATCH /api/me` com `{ "fullName": "..." }`. Confirme que o nome mudou na resposta e que o curso permaneceu o mesmo (só um campo foi enviado).
3. **Alternar participação no ranking** — `PATCH /api/me/ranking`. Cada chamada inverte `rankingEligible` (é um toggle, não uma remoção definitiva) — rode duas vezes seguidas para ver o valor indo `true` → `false` → `true`.

### 4. Conta Riot

1. **Vincular conta Riot** — `POST /api/me/riot-account/link`. Ajuste `gameName`/`tagLine`/`region` no ambiente para um Riot ID **real** da região BR antes de rodar (o valor default `Faker#BR1` é só um placeholder e provavelmente não existe na região BR). Exige `RIOT_API_KEY` configurada no backend. Respostas possíveis:
   - `201` — vinculado com sucesso (o backend também tenta sincronizar elo/partidas em segundo plano nesse momento, sem bloquear a resposta mesmo se a Riot API estiver lenta/fora);
   - `404` — Riot ID não existe;
   - `409` — conta já vinculada a outro usuário, ou este usuário já tem uma conta Riot vinculada (rode "Desvincular" primeiro).
2. **Ver perfil com conta Riot vinculada** — `GET /api/me` de novo. Agora o bloco `riotAccount` deve vir preenchido (`gameName`, `tagLine`, `region`) — repare que `puuid` nunca aparece, mesmo autenticado.
3. **Desvincular conta Riot** — `DELETE /api/me/riot-account/unlink`. Deve responder `204`. Rodar de novo (sem re-executar o link) deve dar `404` (não há mais conta vinculada).

## O que ainda não dá pra testar

Ranking (`GET /api/ranking`), perfil público (`GET /api/players/{id}`) e histórico de partidas (`GET /api/players/{id}/matches`) ainda não têm endpoint — as partidas e o elo são sincronizados em segundo plano ao vincular a conta Riot, mas ainda não são expostos por nenhuma rota (isso é etapa 13 em diante). Para conferir que a sincronização de fato rodou, é preciso consultar o banco diretamente:

```sql
SELECT * FROM ranked_stats;
SELECT * FROM matches ORDER BY game_start DESC;
```
