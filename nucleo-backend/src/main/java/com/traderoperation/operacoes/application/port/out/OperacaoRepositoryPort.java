package com.traderoperation.operacoes.application.port.out;

import com.traderoperation.operacoes.domain.Operacao;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface OperacaoRepositoryPort {

    void salvar(Operacao operacao);

    void atualizar(Operacao operacao);

    Optional<Operacao> buscarPorId(UUID id);

    /** Operações PUBLICADA com publicada_em <= limiteSuperior (controle de atraso). */
    List<Operacao> buscarPublicadasAte(Instant limiteSuperior);
}
