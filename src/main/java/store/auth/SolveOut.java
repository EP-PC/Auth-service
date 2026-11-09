package store.auth;

import lombok.Builder;

// resposta do POST /auth/solve. Os nomes idAccount e role SAO as chaves JSON
// que o Gateway le por nome: nao renomear.
@Builder
public record SolveOut(
    String idAccount,
    String role
) {}
