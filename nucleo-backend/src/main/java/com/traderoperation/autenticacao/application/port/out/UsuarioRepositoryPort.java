package com.traderoperation.autenticacao.application.port.out;

import com.traderoperation.autenticacao.domain.Usuario;
import java.util.Optional;
import java.util.UUID;

public interface UsuarioRepositoryPort {

    Optional<Usuario> buscarPorEmail(String email);

    Optional<Usuario> buscarPorId(UUID id);
}
