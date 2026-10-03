package com.traderoperation.autenticacao.application.port.out;

public interface SenhaPort {

    boolean confere(String senhaPura, String hash);
}
