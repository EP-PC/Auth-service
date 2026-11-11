package store.auth;

import java.util.Base64;
import java.util.Date;

import javax.crypto.SecretKey;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import store.account.AccountOut;

@Service
public class JwtService {

    private static final String ISSUER = "store";
    private static final String CLAIM_EMAIL = "email";
    private static final String CLAIM_ROLE = "role";

    private final SecretKey key;
    private final long duration;
    private final boolean httpOnly;
    private final boolean cookieSecure;

    // a chave e montada uma vez so: Base64 invalido ou chave < 256 bits
    // (WeakKeyException) derrubam a inicializacao, em vez de falhar no 1o login
    public JwtService(
        @Value("${store.jwt.secretKey}") String secretKey,
        @Value("${store.jwt.duration}") long duration,
        @Value("${store.jwt.httpOnly}") boolean httpOnly,
        @Value("${store.jwt.cookieSecure}") boolean cookieSecure
    ) {
        this.key = Keys.hmacShaKeyFor(Base64.getDecoder().decode(secretKey));
        this.duration = duration;
        this.httpOnly = httpOnly;
        this.cookieSecure = cookieSecure;
    }

    // nunca colocar senha nem hash no token: o payload e so Base64, qualquer um le
    public String generate(AccountOut account) {
        Date now = new Date();
        return Jwts.builder()
            .id(account.id())                       // jti = id da conta
            .subject(account.name())                // sub = nome
            .claim(CLAIM_EMAIL, account.email())
            .claim(CLAIM_ROLE, account.role())
            .issuer(ISSUER)
            .notBefore(now)
            .expiration(new Date(now.getTime() + duration))
            .signWith(key)                          // 32 bytes -> HS256
            .compact();
    }

    // verifica assinatura, nbf, exp e iss. Qualquer falha -> 401 (nunca 500)
    public SolveOut solve(String token) {
        try {
            Claims claims = Jwts.parser()
                .verifyWith(key)
                .requireIssuer(ISSUER)
                .build()
                .parseSignedClaims(token)
                .getPayload();
            String idAccount = claims.getId();
            String role = claims.get(CLAIM_ROLE, String.class);
            if (idAccount == null || role == null) {
                throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid token");
            }
            return SolveOut.builder()
                .idAccount(idAccount)
                .role(role)
                .build();
        } catch (JwtException | IllegalArgumentException e) {
            // IllegalArgumentException: token nulo ou vazio. Nao logar o token.
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid token");
        }
    }

    public long duration() {
        return duration;
    }

    public boolean httpOnly() {
        return httpOnly;
    }

    public boolean cookieSecure() {
        return cookieSecure;
    }

}
