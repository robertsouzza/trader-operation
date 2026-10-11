package com.traderoperation.autenticacao.application.port.in;

import com.traderoperation.shared.security.UsuarioAutenticado;
import java.util.Optional;

/**
 * Autentica uma requisição a partir do valor bruto do cookie de access.
 * Usado pelo módulo tempo_real no handshake WebSocket, que não passa pelo filtro HTTP.
 */
public interface AutenticarPorCookieUseCase {

    Optional<UsuarioAutenticado> autenticar(String tokenAccess);

    String nomeCookieAccess();
}
