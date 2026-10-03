package com.traderoperation.autenticacao.infrastructure.adapter.out.persistence;

import com.traderoperation.autenticacao.application.port.out.UsuarioRepositoryPort;
import com.traderoperation.autenticacao.domain.Usuario;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Component;

@Component
class UsuarioPersistenceAdapter implements UsuarioRepositoryPort {

    private final UsuarioJpaRepository repositorio;

    UsuarioPersistenceAdapter(UsuarioJpaRepository repositorio) {
        this.repositorio = repositorio;
    }

    @Override
    public Optional<Usuario> buscarPorEmail(String email) {
        return repositorio.findByEmail(email).map(UsuarioPersistenceAdapter::paraDominio);
    }

    @Override
    public Optional<Usuario> buscarPorId(UUID id) {
        return repositorio.findById(id).map(UsuarioPersistenceAdapter::paraDominio);
    }

    private static Usuario paraDominio(UsuarioJpaEntity e) {
        return new Usuario(e.getId(), e.getNome(), e.getEmail(), e.getSenhaHash(), e.getPerfil(), e.isAtivo());
    }
}
