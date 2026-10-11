package com.traderoperation.tempo_real.infrastructure.websocket;

import com.traderoperation.shared.security.UsuarioAutenticado;
import java.security.Principal;
import java.util.Set;

/**
 * Principal injetado na sessão WebSocket depois do handshake autenticado.
 * Carrega os direitos para o ChannelInterceptor decidir subscriptions.
 */
public final class UsuarioWsPrincipal implements Principal {

    private final UsuarioAutenticado usuario;
    private final Set<String> direitos;

    public UsuarioWsPrincipal(UsuarioAutenticado usuario, Set<String> direitos) {
        this.usuario = usuario;
        this.direitos = Set.copyOf(direitos);
    }

    public UsuarioAutenticado usuario() {
        return usuario;
    }

    public Set<String> direitos() {
        return direitos;
    }

    @Override
    public String getName() {
        return usuario.id().toString();
    }
}
