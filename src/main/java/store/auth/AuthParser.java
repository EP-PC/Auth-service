package store.auth;

import store.account.AccountIn;

// converte os DTOs do Auth para o DTO do contrato do Account.
// Sem validacao aqui: o Account valida e o Auth repassa o 400.
public class AuthParser {

    public static AccountIn to(RegisterIn in) {
        return in == null ? null :
            AccountIn.builder()
                .name(in.name())
                .email(in.email())
                .password(in.password())
                .build();
    }

    // o /accounts/login so usa email e senha; name fica nulo
    public static AccountIn to(LoginIn in) {
        return in == null ? null :
            AccountIn.builder()
                .email(in.email())
                .password(in.password())
                .build();
    }

}
