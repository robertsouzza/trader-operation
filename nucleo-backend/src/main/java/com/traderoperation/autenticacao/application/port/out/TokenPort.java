package com.traderoperation.autenticacao.application.port.out;

import com.traderoperation.autenticacao.domain.Usuario;
import com.traderoperation.shared.security.UsuarioAutenticado;
import java.util.Optional;
import java.util.UUID;

public interface TokenPort {

    String gerarAccess(Usuario usuario);

    String gerarRefresh(Usuario usuario);

    /** Id do usuário se o refresh token for válido, assinado por nós e do tipo refresh. */
    Optional<UUID> validarRefresh(String token);

    /** Dados do usuário se o access token for válido, assinado por nós e do tipo access. */
    Optional<UsuarioAutenticado> validarAccess(String token);
}
