package com.traderoperation.chat.infrastructure.adapter.out.persistence;

import com.traderoperation.chat.application.port.out.MensagemRepositoryPort;
import com.traderoperation.chat.domain.Mensagem;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Component;

@Component
class MensagemPersistenceAdapter implements MensagemRepositoryPort {

    private final MensagemJpaRepository repositorio;

    MensagemPersistenceAdapter(MensagemJpaRepository repositorio) {
        this.repositorio = repositorio;
    }

    @Override
    public void salvar(Mensagem mensagem) {
        MensagemJpaEntity e = new MensagemJpaEntity();
        e.setId(mensagem.id());
        e.setOperacaoId(mensagem.operacaoId());
        e.setAutorId(mensagem.autorId());
        e.setTexto(mensagem.texto());
        e.setEnviadaEm(mensagem.enviadaEm());
        repositorio.save(e);
    }

    @Override
    public List<Mensagem> listarMaisRecentes(UUID operacaoId, int limite) {
        // Vem em ordem decrescente do banco; devolvemos em ordem crescente para a UI apenas anexar no fim.
        return repositorio.listarMaisRecentes(operacaoId, PageRequest.of(0, limite))
                .stream()
                .map(MensagemPersistenceAdapter::aDominio)
                .sorted(Comparator.comparing(Mensagem::enviadaEm))
                .toList();
    }

    private static Mensagem aDominio(MensagemJpaEntity e) {
        return new Mensagem(e.getId(), e.getOperacaoId(), e.getAutorId(), e.getTexto(), e.getEnviadaEm());
    }
}
