package com.traderoperation.autenticacao.application.port.in;

/** Resultado de login ou renovação. Os tokens vão para cookies httpOnly, nunca para o corpo da resposta. */
public record SessaoEmitida(DadosUsuario usuario, String accessToken, String refreshToken) {
}
