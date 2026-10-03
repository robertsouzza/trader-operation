package com.traderoperation.auditoria.application.port.out;

import com.traderoperation.auditoria.domain.RegistroAuditoria;

public interface AuditoriaRepositoryPort {

    void salvar(RegistroAuditoria registro);
}
