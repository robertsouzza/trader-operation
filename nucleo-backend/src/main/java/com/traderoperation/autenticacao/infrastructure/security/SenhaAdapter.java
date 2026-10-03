package com.traderoperation.autenticacao.infrastructure.security;

import com.traderoperation.autenticacao.application.port.out.SenhaPort;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Component;

@Component
class SenhaAdapter implements SenhaPort {

    private final BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();

    @Override
    public boolean confere(String senhaPura, String hash) {
        return senhaPura != null && encoder.matches(senhaPura, hash);
    }
}
