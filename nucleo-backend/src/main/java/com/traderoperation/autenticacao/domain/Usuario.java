package com.traderoperation.autenticacao.domain;

import java.util.Locale;
import java.util.Objects;
import java.util.UUID;

/** Usuário da plataforma. O hash da senha nunca sai do módulo de autenticação. */
public record Usuario(UUID id, String nome, String email, String senhaHash, Perfil perfil, boolean ativo) {

    public Usuario {
        Objects.requireNonNull(id, "id");
        Objects.requireNonNull(perfil, "perfil");
        if (nome == null || nome.isBlank()) {
            throw new IllegalArgumentException("O nome do usuário é obrigatório.");
        }
        email = normalizarEmail(email);
        if (senhaHash == null || senhaHash.isBlank()) {
            throw new IllegalArgumentException("O hash da senha é obrigatório.");
        }
    }

    /** E-mail sempre em minúsculas e sem espaços, para login e unicidade não dependerem de caixa. */
    public static String normalizarEmail(String email) {
        if (email == null || !email.contains("@")) {
            throw new IllegalArgumentException("E-mail inválido.");
        }
        return email.trim().toLowerCase(Locale.ROOT);
    }
}
