package com.traderoperation.auditoria.application.port.in;

import java.util.Map;
import java.util.UUID;

/** Porta de entrada para outros módulos gravarem no diário com detalhes que o @Auditavel não tem. */
public interface RegistrarAuditoriaUseCase {

    void registrar(UUID usuarioId, String acao, String entidade, String entidadeId, Map<String, Object> dados);
}
