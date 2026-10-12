package com.traderoperation.shared.eventos;

/** Nomes dos canais Redis compartilhados entre os módulos para pub/sub (D-26). */
public final class CanaisRedis {

    public static final String OPERACAO_PUBLICADA = "trader:operacoes:publicada";
    public static final String OPERACAO_ENCERRADA = "trader:operacoes:encerrada";
    public static final String CHAT_PREFIXO = "trader:chat:";

    public static String chatDaOperacao(String operacaoId) {
        return CHAT_PREFIXO + operacaoId;
    }

    private CanaisRedis() {}
}
