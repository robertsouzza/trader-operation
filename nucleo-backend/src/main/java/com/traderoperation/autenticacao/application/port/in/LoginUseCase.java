package com.traderoperation.autenticacao.application.port.in;

public interface LoginUseCase {

    /** Valida as credenciais e emite os tokens. Lança NaoAutorizadoException se forem inválidas. */
    SessaoEmitida login(String email, String senha);
}
