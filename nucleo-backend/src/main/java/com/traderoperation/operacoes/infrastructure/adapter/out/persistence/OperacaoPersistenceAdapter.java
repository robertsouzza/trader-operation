package com.traderoperation.operacoes.infrastructure.adapter.out.persistence;

import com.traderoperation.operacoes.application.port.out.OperacaoRepositoryPort;
import com.traderoperation.operacoes.domain.Operacao;
import com.traderoperation.operacoes.domain.StatusOperacao;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Component;

@Component
class OperacaoPersistenceAdapter implements OperacaoRepositoryPort {

    private final OperacaoJpaRepository repositorio;

    OperacaoPersistenceAdapter(OperacaoJpaRepository repositorio) {
        this.repositorio = repositorio;
    }

    @Override
    public void salvar(Operacao operacao) {
        repositorio.save(aEntidade(operacao));
    }

    @Override
    public void atualizar(Operacao operacao) {
        repositorio.save(aEntidade(operacao));
    }

    @Override
    public Optional<Operacao> buscarPorId(UUID id) {
        return repositorio.findById(id).map(OperacaoPersistenceAdapter::aDominio);
    }

    @Override
    public List<Operacao> buscarPublicadasAte(Instant limiteSuperior) {
        return repositorio
                .findByStatusAndPublicadaEmLessThanEqualOrderByPublicadaEmDesc(
                        StatusOperacao.PUBLICADA, limiteSuperior)
                .stream()
                .map(OperacaoPersistenceAdapter::aDominio)
                .toList();
    }

    private static OperacaoJpaEntity aEntidade(Operacao op) {
        OperacaoJpaEntity e = new OperacaoJpaEntity();
        e.setId(op.id());
        e.setAtivo(op.ativo());
        e.setDirecao(op.direcao());
        e.setEntrada(op.entrada());
        e.setStop(op.stop());
        e.setAlvos(op.alvos().toArray(new BigDecimal[0]));
        e.setEstrategia(op.estrategia());
        e.setStatus(op.status());
        e.setResultado(op.resultado());
        e.setObservacao(op.observacao());
        e.setMasterId(op.masterId());
        e.setPublicadaEm(op.publicadaEm());
        e.setEncerradaEm(op.encerradaEm());
        return e;
    }

    private static Operacao aDominio(OperacaoJpaEntity e) {
        return new Operacao(
                e.getId(),
                e.getAtivo(),
                e.getDirecao(),
                e.getEntrada(),
                e.getStop(),
                e.getAlvos() == null ? List.of() : Arrays.asList(e.getAlvos()),
                e.getEstrategia(),
                e.getStatus(),
                e.getResultado(),
                e.getObservacao(),
                e.getMasterId(),
                e.getPublicadaEm(),
                e.getEncerradaEm());
    }
}
