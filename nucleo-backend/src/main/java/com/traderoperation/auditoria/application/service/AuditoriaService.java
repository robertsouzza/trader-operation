package com.traderoperation.auditoria.application.service;

import com.traderoperation.auditoria.application.port.in.RegistrarAuditoriaUseCase;
import com.traderoperation.auditoria.application.port.out.AuditoriaRepositoryPort;
import com.traderoperation.auditoria.domain.RegistroAuditoria;
import java.time.Clock;
import java.util.Map;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuditoriaService implements RegistrarAuditoriaUseCase {

    private final AuditoriaRepositoryPort repositorio;
    private final Clock clock;

    public AuditoriaService(AuditoriaRepositoryPort repositorio, Clock clock) {
        this.repositorio = repositorio;
        this.clock = clock;
    }

    /** Transação própria: o registro fica gravado mesmo quando a operação auditada faz rollback. */
    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void registrar(UUID usuarioId, String acao, String entidade, String entidadeId, Map<String, Object> dados) {
        repositorio.salvar(RegistroAuditoria.novo(usuarioId, acao, entidade, entidadeId, dados, clock.instant()));
    }
}
