package com.traderoperation.shared.security;

import java.util.UUID;

/**
 * Usuário da requisição atual, colocado no SecurityContext pelo filtro de autenticação.
 * Fica em shared para que qualquer módulo saiba quem está agindo sem depender de autenticacao.
 */
public record UsuarioAutenticado(UUID id, String email, String perfil) {
}
