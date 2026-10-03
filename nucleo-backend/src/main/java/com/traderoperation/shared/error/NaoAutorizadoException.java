package com.traderoperation.shared.error;

/** Credenciais ou sessão inválidas. Vira HTTP 401. */
public class NaoAutorizadoException extends RuntimeException {

    public NaoAutorizadoException(String mensagem) {
        super(mensagem);
    }
}
