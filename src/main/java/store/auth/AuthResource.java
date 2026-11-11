package store.auth;

import java.time.Duration;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import store.account.AccountOut;

// paths e campos JSON sao contrato com o Gateway e o front: nao mudar
@RestController
@RequestMapping("/auth")
public class AuthResource {

    public static final String AUTH_COOKIE_TOKEN = "__store_jwt_token";

    @Autowired
    private AuthService authService;

    @Autowired
    private JwtService jwtService;

    // rota aberta: cria a conta (sempre USER) e devolve 201 + AccountOut; nao loga
    @PostMapping("/register")
    public ResponseEntity<AccountOut> register(@RequestBody RegisterIn in) {
        return ResponseEntity.status(HttpStatus.CREATED).body(authService.register(in));
    }

    // rota aberta: token so no cookie HttpOnly (o JS do front nao le)
    @PostMapping("/login")
    public ResponseEntity<Void> login(@RequestBody LoginIn in) {
        String token = authService.login(in);
        return ResponseEntity.ok()
            .header(HttpHeaders.SET_COOKIE, buildCookie(token, Duration.ofMillis(jwtService.duration())).toString())
            .build();
    }

    // rota aberta: sobrescreve o cookie com valor vazio e Max-Age=0 (o navegador apaga)
    @GetMapping("/logout")
    public ResponseEntity<Void> logout() {
        return ResponseEntity.ok()
            .header(HttpHeaders.SET_COOKIE, buildCookie("", Duration.ZERO).toString())
            .build();
    }

    // rota protegida: o Gateway resolve o token e injeta o header id-account.
    // required = false: sem header, o Service responde 401 (e nao 400 do Spring)
    @GetMapping("/me")
    public ResponseEntity<AccountOut> me(
        @RequestHeader(value = "id-account", required = false) String idAccount
    ) {
        return ResponseEntity.ok(authService.me(idAccount));
    }

    // chamado pelo Gateway (WebClient) em toda rota protegida
    @PostMapping("/solve")
    public ResponseEntity<SolveOut> solve(@RequestBody TokenOut in) {
        if (in == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid token");
        }
        return ResponseEntity.ok(authService.solve(in.token()));
    }

    @GetMapping("/health-check")
    public ResponseEntity<Void> healthCheck() {
        return ResponseEntity.ok().build();
    }

    // unico lugar que define o cookie. Secure vem de JWT_COOKIE_SECURE (default true).
    // SameSite=None (front em outro dominio) exige Secure: o navegador rejeita
    // None sem Secure, entao sem Secure cai para Lax (mesmo site)
    private ResponseCookie buildCookie(String value, Duration maxAge) {
        boolean secure = jwtService.cookieSecure();
        return ResponseCookie.from(AUTH_COOKIE_TOKEN, value)
            .httpOnly(jwtService.httpOnly())
            .secure(secure)
            .sameSite(secure ? "None" : "Lax")
            .path("/")
            .maxAge(maxAge)
            .build();
    }

}
