package store.auth;

import lombok.Builder;

// corpo do POST /auth/login (contrato com o front: nao mudar os nomes)
@Builder
public record LoginIn(
    String email,
    String password
) {}
