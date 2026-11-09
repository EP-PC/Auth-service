package store.auth;

import lombok.Builder;

// corpo do POST /auth/solve: {"token": "..."} enviado pelo Gateway (como no hands-on 3)
@Builder
public record TokenOut(
    String token
) {}
