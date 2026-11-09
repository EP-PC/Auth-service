# Auth-service

Microserviço **Auth** da store: cadastro, login/logout e emissão/validação de JWT.
Não tem banco. Fala com o **Account** via Feign (contrato `store:account`) para criar a conta
e validar as credenciais. O **Gateway** chama `POST /auth/solve` para validar o token das rotas protegidas.

## Endpoints

| Endpoint | Request | Sucesso | Erros |
|---|---|---|---|
| `POST /auth/register` | `{name, email, password}` | `201` + `{id, name, email, role}` | `400`, `409` (repassados do Account) |
| `POST /auth/login` | `{email, password}` | `200` + cookie `__store_jwt_token` (sem body) | `401` |
| `GET /auth/logout` | — | `200` + cookie com `Max-Age=0` | — |
| `GET /auth/me` | header `id-account` (posto pelo Gateway) | `200` + `{id, name, email, role}` | `401` sem header, `404` |
| `POST /auth/solve` | `{"token": "..."}` | `200` + `{"idAccount": "...", "role": "USER\|ADMIN"}` | `401` token inválido/expirado |
| `GET /auth/health-check` | — | `200` | — |

Se o Account estiver fora do ar, as rotas que dependem dele respondem `503`.
Os erros seguem o formato `{timestamp, status, error, message, path}`.

## JWT

Assinado com HMAC (HS256). Claims: `jti` (id da conta), `sub` (nome), `email`, `role`, `iss` (`store`), `nbf`, `exp`.
O token nunca carrega senha nem hash.

O cookie é `HttpOnly`, `Secure`, `SameSite=None` e `Path=/`, com `Max-Age` igual à duração do token.

## Variáveis de ambiente

| Variável | Obrigatória | Default | Descrição |
|---|---|---|---|
| `JWT_SECRET_KEY` | sim | — | Base64 de **32 bytes aleatórios** (mínimo do HS256) |
| `JWT_DURATION` | não | `3600000` | duração do token/cookie, em ms |
| `JWT_HTTP_ONLY` | não | `true` | flag `HttpOnly` do cookie |

Gerando a chave:

```bash
openssl rand -base64 32
```

## Rodando localmente

O `compose.yaml` sobe `db` + `account` (build do repo irmão `../Account-service`) + `auth` (porta 8080).

```bash
# 1. jars (o Dockerfile copia o target/)
(cd ../Account-service && mvn package)
mvn package

# 2. variáveis
cp .env.example .env   # e preencha (JWT_SECRET_KEY e ACCOUNT_ADMIN_*)

# 3. subir
docker compose up --build
```

Os compose do Gateway, Product-service e Account-service também usam `name: store` e a porta 8080.
Rode `docker compose down` neles antes (**sem `-v`**: o volume `store_db-data` é compartilhado).
Se o volume já existir, o admin do seed é o que foi criado na primeira vez. Use os mesmos `ACCOUNT_ADMIN_*`.

## Teste rápido

```bash
curl -i -X POST localhost:8080/auth/register -H 'Content-Type: application/json' \
  -d '{"name":"Antonio","email":"acme@insper.edu.br","password":"123@321"}'
curl -i -c jar.txt -X POST localhost:8080/auth/login -H 'Content-Type: application/json' \
  -d '{"email":"acme@insper.edu.br","password":"123@321"}'
curl -i -X POST localhost:8080/auth/solve -H 'Content-Type: application/json' \
  -d "{\"token\":\"$(awk '/__store_jwt_token/{print $7}' jar.txt)\"}"
```
