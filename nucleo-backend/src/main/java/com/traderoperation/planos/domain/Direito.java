package com.traderoperation.planos.domain;

/**
 * Lista fechada de direitos (D-15). A matriz plano→direitos e perfil→direitos vive em
 * {@link com.traderoperation.planos.application.service.MatrizDeDireitos}; trocar a matriz
 * exige deploy. Nenhum ponto da aplicação pode criar outra lista de direitos "na mão".
 */
public enum Direito {
    VER_OPERACAO_COM_ATRASO,
    VER_OPERACAO_TEMPO_REAL,
    LER_CHAT_OPERACAO,
    PARTICIPAR_CHAT_OPERACAO,
    USAR_COPILOTO_MT5,
    USAR_CHAT_IA,
    PEDIR_BACKTEST_IA,
    PUBLICAR_NA_VITRINE,
    PUBLICAR_OPERACAO,
    APROVAR_ESTRATEGIA,
    ADMINISTRAR_USUARIOS
}
