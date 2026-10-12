package com.traderoperation.operacoes.application.port.in;

import java.time.Duration;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ConsultarOperacoesQuery {

    /** Operações PUBLICADA; usa o atraso quando o usuário só tem VER_OPERACAO_COM_ATRASO. */
    List<OperacaoResumo> listarAoVivo(Duration atrasoMinimo);

    /** Carrega uma operação pelo id, respeitando o atraso quando aplicável. */
    Optional<OperacaoResumo> obter(UUID id, Duration atrasoMinimo);

    /** Checagem rápida usada por outros módulos (ex: chat) sem vazar o enum StatusOperacao. */
    boolean estaPublicada(UUID id);
}
