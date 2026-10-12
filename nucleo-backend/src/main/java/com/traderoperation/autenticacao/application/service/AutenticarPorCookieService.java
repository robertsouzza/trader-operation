package com.traderoperation.autenticacao.application.service;

import com.traderoperation.autenticacao.application.port.in.AutenticarPorCookieUseCase;
import com.traderoperation.autenticacao.application.port.out.TokenPort;
import com.traderoperation.shared.security.UsuarioAutenticado;
import java.util.Optional;
import org.springframework.stereotype.Service;

/**
 * Autentica uma requisição a partir do valor bruto do cookie de access.
 * Serve ao módulo tempo_real, cujo handshake WebSocket não passa pelo filtro HTTP.
 * Devolve o nome do cookie como constante aqui (único ponto fora de infra que o conhece).
 */
@Service
public class AutenticarPorCookieService implements AutenticarPorCookieUseCase {

    private static final String NOME_COOKIE_ACCESS = "trader_access";

    private final TokenPort tokens;

    public AutenticarPorCookieService(TokenPort tokens) {
        this.tokens = tokens;
    }

    @Override
    public Optional<UsuarioAutenticado> autenticar(String tokenAccess) {
        if (tokenAccess == null || tokenAccess.isBlank()) {
            return Optional.empty();
        }
        return tokens.validarAccess(tokenAccess);
    }

    @Override
    public String nomeCookieAccess() {
        return NOME_COOKIE_ACCESS;
    }
}
