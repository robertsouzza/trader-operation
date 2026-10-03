package com.traderoperation.autenticacao.application.port.in;

import java.util.UUID;

/** Dados públicos do usuário: o que pode sair na API. */
public record DadosUsuario(UUID id, String nome, String email, String perfil) {
}
