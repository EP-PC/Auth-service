package store.auth;

import lombok.Builder;

// corpo do POST /auth/register (contrato com o front: nao mudar os nomes)
@Builder
public record RegisterIn(
    String name,
    String email,
    String password
) {}
