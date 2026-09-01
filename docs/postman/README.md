# Testando o fluxo completo no Postman

Este guia cobre o fluxo de ponta a ponta implementado até agora: cadastro → verificação de e-mail → login → perfil → vínculo de conta Riot (com sincronização automática de elo, partidas e estatísticas). Os arquivos [AAAC-Ranking.postman_collection.json](AAAC-Ranking.postman_collection.json) e [AAAC-Ranking.postman_environment.json](AAAC-Ranking.postman_environment.json) desta pasta já trazem as requisições nessa ordem, prontas para importar.

## Pré-requisitos

1. **API rodando localmente** — o jeito mais simples é subir tudo via Docker Compose (API + Postgres + Mailpit juntos, já na mesma rede):
   ```bash
   docker compose --env-file .env up -d --build
   ```
   Veja o [README.md](../../README.md) principal para as outras formas de rodar (só o banco + `./gradlew bootRun`, etc.).
2. **`RIOT_API_KEY` válida no `.env`** — obrigatória mesmo para subir o container `api` (o `docker-compose.yml` falha rápido com uma mensagem clara se ela não estiver definida). Só é *usada de fato* no passo de vincular a conta Riot (pasta 4 da coleção); o resto do fluxo funciona com qualquer valor.
3. **Mailpit** — já vem como serviço do `docker-compose.yml` (`mailpit`), então sobe junto com o `docker compose up -d`. A UI web fica em `http://localhost:8025`. Se você estiver rodando a API fora do Docker (`./gradlew bootRun`), suba só o serviço do Mailpit:
   ```bash
   docker compose --env-file .env up -d mailpit
   ```
   Nesse caso `MAIL_HOST=localhost` (default do `.env.example`) já funciona, porque a API roda no host, fora do Docker.
4. **Um curso ativo cadastrado no banco** — a listagem de cursos (pasta 1) precisa retornar pelo menos um resultado. Se a tabela `courses` estiver vazia, insira um curso manualmente antes de começar:
   ```bash
   docker exec -i pucrs-db-1 psql -U aaac_ranking -d aaac_ranking -c "INSERT INTO courses (name, active) VALUES ('Ciência da Computação', true);"
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

1. **Registrar usuário** — `POST /api/auth/register`. Usa `{{email}}`/`{{password}}`/`{{courseId}}` do ambiente — o e-mail **precisa** terminar em `@edu.pucrs.br` (é o domínio institucional validado no backend; qualquer outro domínio dá `400`). Deve responder `201` com `{ id, fullName }`. Isso já dispara o envio do código de verificação por e-mail.
2. Abra o Mailpit (`http://localhost:8025`) e localize o e-mail "AAAC Ranking - Codigo de verificacao" enviado para `{{email}}`. Copie o código de 6 dígitos.
3. Na variável de ambiente **`verificationCode`**, cole o código copiado.
4. **Verificar e-mail** — `POST /api/auth/verify-email`. Deve responder `200`. Rodar de novo com o mesmo código deve dar `400` (código já consumido) — útil para confirmar que o uso único está funcionando.
5. **Reenviar código de verificação (opcional)** — `POST /api/auth/resend-verification`. Sempre responde `202`, mesmo que o e-mail já esteja verificado (não deve chegar um novo e-mail nesse caso — confirme no Mailpit).
6. **Login** — `POST /api/auth/login`. Deve responder `200` com o token JWT. Um script de teste salva esse token na variável `token`, usada em todas as requisições autenticadas daqui pra frente.

### 3. Perfil

1. **Ver meu perfil** — `GET /api/me`. Deve mostrar `emailVerified: true`, `rankingEligible: false` e `riotAccount: null` (antes de vincular uma conta).
2. **Editar nome/curso** — `PATCH /api/me` com `{ "fullName": "..." }`. Confirme que o nome mudou na resposta e que o curso permaneceu o mesmo (só um campo foi enviado — os dois são opcionais, mas pelo menos um é obrigatório, senão dá `400`).
3. **Alternar participação no ranking** — `PATCH /api/me/ranking`. Cada chamada inverte `rankingEligible` (é um toggle, não uma remoção definitiva) — rode duas vezes seguidas para ver o valor indo `true` → `false` → `true`.

### 4. Conta Riot

1. **Vincular conta Riot** — `POST /api/me/riot-account/link`. Ajuste `gameName`/`tagLine`/`region` no ambiente para um Riot ID **real** da região BR antes de rodar (o valor default `Faker#BR1` é só um placeholder e provavelmente não existe na região BR — `TrynBR#BR1` é um exemplo que funcionou nos nossos próprios testes). Respostas possíveis:
   - `201` — vinculado com sucesso. Nesse mesmo momento o backend também tenta, em sequência: sincronizar o elo Solo/Duo, importar as partidas da temporada e recalcular as estatísticas agregadas — tudo isso sem bloquear nem falhar a resposta, mesmo que a Riot API esteja lenta ou fora do ar;
   - `404` — Riot ID não existe;
   - `409` — conta já vinculada a outro usuário, ou este usuário já tem uma conta Riot vinculada (rode "Desvincular" primeiro);
   - `401` — sintoma quase sempre de `RIOT_API_KEY` ausente/inválida/expirada (as dev keys da Riot expiram a cada 24h).
2. **Ver perfil com conta Riot vinculada** — `GET /api/me` de novo. Agora o bloco `riotAccount` deve vir preenchido (`gameName`, `tagLine`, `region`) — repare que `puuid` nunca aparece, mesmo autenticado.
3. **Desvincular conta Riot** — `DELETE /api/me/riot-account/unlink`. Deve responder `204` (isso também apaga as partidas e estatísticas associadas — veja a nota abaixo). Rodar de novo (sem re-executar o link) deve dar `404` (não há mais conta vinculada).

## O que ainda não dá pra testar por endpoint

Ranking (`GET /api/ranking`), perfil público (`GET /api/players/{id}`) e histórico de partidas (`GET /api/players/{id}/matches`) ainda não existem como endpoint. O elo, as partidas e as estatísticas agregadas *são* sincronizados/calculados automaticamente ao vincular a conta Riot, mas a única forma de conferir isso hoje é consultando o banco direto:

```bash
docker exec -i pucrs-db-1 psql -U aaac_ranking -d aaac_ranking -c "
SELECT tier, division, league_points, wins, losses FROM ranked_stats;
"
docker exec -i pucrs-db-1 psql -U aaac_ranking -d aaac_ranking -c "
SELECT riot_match_id, win, champion_id, role, kills, deaths, assists FROM matches ORDER BY game_start DESC;
"
docker exec -i pucrs-db-1 psql -U aaac_ranking -d aaac_ranking -c "
SELECT total_games, wins, losses, win_rate, primary_role, most_played_champion_id FROM player_stats;
"
```

Vale notar: **desvincular a conta Riot apaga em cascata as linhas de `matches` e `player_stats` associadas** (é uma exclusão definitiva, não um arquivamento) — se quiser inspecionar esses dados, faça isso antes de rodar "Desvincular conta Riot".
