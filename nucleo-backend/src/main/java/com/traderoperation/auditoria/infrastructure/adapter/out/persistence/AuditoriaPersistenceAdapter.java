package com.traderoperation.auditoria.infrastructure.adapter.out.persistence;

import com.traderoperation.auditoria.application.port.out.AuditoriaRepositoryPort;
import com.traderoperation.auditoria.domain.RegistroAuditoria;
import org.springframework.stereotype.Component;

@Component
class AuditoriaPersistenceAdapter implements AuditoriaRepositoryPort {

    private final RegistroAuditoriaJpaRepository repositorio;

    AuditoriaPersistenceAdapter(RegistroAuditoriaJpaRepository repositorio) {
        this.repositorio = repositorio;
    }

    @Override
    public void salvar(RegistroAuditoria r) {
        repositorio.save(new RegistroAuditoriaJpaEntity(
                r.id(), r.usuarioId(), r.acao(), r.entidade(), r.entidadeId(), r.dados(), r.ocorridoEm()));
    }
}
