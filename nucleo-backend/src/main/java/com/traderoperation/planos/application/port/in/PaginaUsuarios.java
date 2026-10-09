package com.traderoperation.planos.application.port.in;

import java.util.List;

/** Página de resultados da listagem administrativa de usuários. */
public record PaginaUsuarios(long total, int pagina, int tamanho, List<ResumoUsuario> itens) {
}
