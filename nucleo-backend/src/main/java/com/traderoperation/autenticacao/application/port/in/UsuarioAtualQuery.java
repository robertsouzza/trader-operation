package com.traderoperation.autenticacao.application.port.in;

import java.util.UUID;

public interface UsuarioAtualQuery {

    DadosUsuario buscar(UUID id);
}
