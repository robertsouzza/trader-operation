package com.traderoperation.tempo_real.infrastructure.websocket;

import com.traderoperation.autenticacao.application.port.in.AutenticarPorCookieUseCase;
import com.traderoperation.planos.application.port.in.VerificarDireitoUseCase;
import com.traderoperation.shared.security.UsuarioAutenticado;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.http.server.ServletServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.server.HandshakeInterceptor;

/**
 * Lê o cookie de access durante o handshake STOMP; se o token for válido, injeta
 * o {@link UsuarioAutenticado} e os direitos no mapa de atributos da sessão. Rejeita
 * o handshake quando o cookie falta ou é inválido.
 */
@Component
public class AutenticacaoHandshakeInterceptor implements HandshakeInterceptor {

    static final String ATTR_USUARIO = "usuario";
    static final String ATTR_DIREITOS = "direitos";

    private final AutenticarPorCookieUseCase autenticador;
    private final VerificarDireitoUseCase direitos;

    public AutenticacaoHandshakeInterceptor(
            AutenticarPorCookieUseCase autenticador,
            VerificarDireitoUseCase direitos) {
        this.autenticador = autenticador;
        this.direitos = direitos;
    }

    @Override
    public boolean beforeHandshake(
            ServerHttpRequest request,
            ServerHttpResponse response,
            WebSocketHandler wsHandler,
            Map<String, Object> attributes) {
        if (!(request instanceof ServletServerHttpRequest servlet)) {
            return false;
        }
        Optional<UsuarioAutenticado> usuario = lerCookie(
                servlet.getServletRequest(), autenticador.nomeCookieAccess())
                .flatMap(autenticador::autenticar);
        if (usuario.isEmpty()) {
            response.setStatusCode(HttpStatus.UNAUTHORIZED);
            return false;
        }
        UsuarioAutenticado u = usuario.get();
        Set<String> direitosDoUsuario = direitos.direitosDoUsuario(u.id());
        attributes.put(ATTR_USUARIO, u);
        attributes.put(ATTR_DIREITOS, direitosDoUsuario);
        return true;
    }

    @Override
    public void afterHandshake(
            ServerHttpRequest request, ServerHttpResponse response,
            WebSocketHandler wsHandler, Exception exception) {
        // nada
    }

    private static Optional<String> lerCookie(HttpServletRequest request, String nome) {
        Cookie[] cookies = request.getCookies();
        if (cookies == null) return Optional.empty();
        for (Cookie c : cookies) {
            if (nome.equals(c.getName())) {
                return Optional.of(c.getValue());
            }
        }
        return Optional.empty();
    }
}
