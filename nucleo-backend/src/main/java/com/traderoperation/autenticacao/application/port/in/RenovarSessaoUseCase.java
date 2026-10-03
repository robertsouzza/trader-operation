package com.traderoperation.autenticacao.application.port.in;

public interface RenovarSessaoUseCase {

    /** Emite um novo access token a partir de um refresh token válido. */
    SessaoEmitida renovar(String refreshToken);
}
