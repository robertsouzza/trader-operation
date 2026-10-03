package com.traderoperation.autenticacao.infrastructure.security;

import com.traderoperation.shared.config.TraderProperties;
import java.time.Duration;
import org.springframework.http.ResponseCookie;
import org.springframework.stereotype.Component;

/** Monta os cookies httpOnly da sessão (D-10). O frontend nunca lê os tokens. */
@Component
public class CookiesSessao {

    public static final String ACCESS = "trader_access";
    public static final String REFRESH = "trader_refresh";
    private static final String CAMINHO_ACCESS = "/";
    private static final String CAMINHO_REFRESH = "/api/auth";

    private final boolean secure;
    private final JwtService jwt;

    public CookiesSessao(TraderProperties props, JwtService jwt) {
        this.secure = props.cookie().secure();
        this.jwt = jwt;
    }

    public ResponseCookie access(String token) {
        return cookie(ACCESS, token, CAMINHO_ACCESS, jwt.duracaoAccess());
    }

    public ResponseCookie refresh(String token) {
        return cookie(REFRESH, token, CAMINHO_REFRESH, jwt.duracaoRefresh());
    }

    public ResponseCookie expirarAccess() {
        return cookie(ACCESS, "", CAMINHO_ACCESS, Duration.ZERO);
    }

    public ResponseCookie expirarRefresh() {
        return cookie(REFRESH, "", CAMINHO_REFRESH, Duration.ZERO);
    }

    private ResponseCookie cookie(String nome, String valor, String caminho, Duration duracao) {
        return ResponseCookie.from(nome, valor)
                .httpOnly(true)
                .secure(secure)
                .sameSite("Lax")
                .path(caminho)
                .maxAge(duracao)
                .build();
    }
}
