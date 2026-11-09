package store.auth;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import feign.FeignException;
import lombok.extern.slf4j.Slf4j;
import store.account.AccountController;
import store.account.AccountOut;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

@Slf4j
@Service
public class AuthService {

    @Autowired
    private AccountController accountController;   // cliente Feign do contrato store:account

    @Autowired
    private JwtService jwtService;

    @Autowired
    private ObjectMapper objectMapper;

    // repassa 400/409 do Account com a mensagem dele (ex.: "Email already registered")
    public AccountOut register(RegisterIn in) {
        try {
            return accountController.create(AuthParser.to(in)).getBody();
        } catch (FeignException.BadRequest | FeignException.Conflict e) {
            throw new ResponseStatusException(HttpStatus.valueOf(e.status()), accountMessage(e));
        } catch (FeignException e) {
            throw translate(e);
        }
    }

    // credencial errada (401) ou incompleta (400) no Account -> 401 aqui, sem dizer qual campo falhou
    public String login(LoginIn in) {
        AccountOut account;
        try {
            account = accountController.login(AuthParser.to(in)).getBody();
        } catch (FeignException.Unauthorized | FeignException.BadRequest e) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid credentials");
        } catch (FeignException e) {
            throw translate(e);
        }
        return jwtService.generate(account);
    }

    // idAccount vem do header id-account, que so o Gateway coloca (depois do solve)
    public AccountOut me(String idAccount) {
        if (idAccount == null || idAccount.isBlank()) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Missing id-account");
        }
        try {
            return accountController.findById(idAccount).getBody();
        } catch (FeignException.NotFound e) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Account not found");
        } catch (FeignException e) {
            throw translate(e);
        }
    }

    public SolveOut solve(String token) {
        return jwtService.solve(token);
    }

    // Account fora do ar (status -1: conexao recusada/timeout) ou 5xx -> 503.
    // Qualquer outro status inesperado vira 500 no ExceptionHandler.
    private RuntimeException translate(FeignException e) {
        if (e.status() < 0 || e.status() >= 500) {
            // so a mensagem do Feign (metodo/url/status), sem corpo da requisicao
            log.error("Account service unavailable: {}", e.getMessage());
            return new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "Account service unavailable");
        }
        return e;
    }

    // le o campo "message" do JSON de erro do Account {timestamp, status, error, message, path}
    private String accountMessage(FeignException e) {
        try {
            JsonNode node = objectMapper.readTree(e.contentUTF8());
            if (node.hasNonNull("message")) {
                return node.get("message").asString();
            }
        } catch (RuntimeException ignored) {
            // corpo vazio ou nao-JSON: usa a mensagem padrao abaixo
        }
        return HttpStatus.valueOf(e.status()).getReasonPhrase();
    }

}
