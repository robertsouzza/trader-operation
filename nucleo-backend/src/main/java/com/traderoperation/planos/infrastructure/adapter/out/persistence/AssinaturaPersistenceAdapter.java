package com.traderoperation.planos.infrastructure.adapter.out.persistence;

import com.traderoperation.planos.application.port.out.AssinaturaRepositoryPort;
import com.traderoperation.planos.domain.Assinatura;
import com.traderoperation.planos.domain.StatusAssinatura;
import java.time.Clock;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.springframework.stereotype.Component;

@Component
class AssinaturaPersistenceAdapter implements AssinaturaRepositoryPort {

    private final AssinaturaJpaRepository repositorio;
    private final Clock clock;

    AssinaturaPersistenceAdapter(AssinaturaJpaRepository repositorio, Clock clock) {
        this.repositorio = repositorio;
        this.clock = clock;
    }

    @Override
    public Optional<Assinatura> buscarAtivaDoUsuario(UUID usuarioId) {
        return repositorio.findByUsuarioIdAndStatus(usuarioId, StatusAssinatura.ATIVA)
                .map(AssinaturaPersistenceAdapter::paraDominio);
    }

    @Override
    public Map<UUID, Assinatura> buscarAtivasDeVarios(Set<UUID> usuarioIds) {
        if (usuarioIds == null || usuarioIds.isEmpty()) {
            return Map.of();
        }
        Map<UUID, Assinatura> por = new HashMap<>();
        repositorio.findByUsuarioIdInAndStatus(usuarioIds, StatusAssinatura.ATIVA)
                .forEach(e -> por.put(e.getUsuarioId(), paraDominio(e)));
        return Map.copyOf(por);
    }

    @Override
    public void salvar(Assinatura assinatura) {
        AssinaturaJpaEntity e = new AssinaturaJpaEntity();
        e.setId(assinatura.id());
        e.setUsuarioId(assinatura.usuarioId());
        e.setPlano(assinatura.plano());
        e.setStatus(assinatura.status());
        e.setOrigem(assinatura.origem());
        e.setInicioEm(assinatura.inicioEm());
        e.setFimEm(assinatura.fimEm());
        e.setCriadoEm(clock.instant());
        repositorio.save(e);
    }

    @Override
    public void atualizar(Assinatura assinatura) {
        AssinaturaJpaEntity e = repositorio.findById(assinatura.id()).orElseThrow(() ->
                new IllegalStateException("Assinatura não encontrada para atualização: " + assinatura.id()));
        e.setStatus(assinatura.status());
        e.setFimEm(assinatura.fimEm());
        // saveAndFlush: o Hibernate reordena INSERTs antes de UPDATEs no flush padrão. Sem flush
        // explícito aqui, o novo INSERT com status ATIVA bateria na constraint parcial
        // uk_assinaturas_ativa_por_usuario antes do UPDATE que cancela a anterior.
        repositorio.saveAndFlush(e);
    }

    private static Assinatura paraDominio(AssinaturaJpaEntity e) {
        return new Assinatura(
                e.getId(),
                e.getUsuarioId(),
                e.getPlano(),
                e.getStatus(),
                e.getOrigem(),
                e.getInicioEm(),
                e.getFimEm());
    }
}
