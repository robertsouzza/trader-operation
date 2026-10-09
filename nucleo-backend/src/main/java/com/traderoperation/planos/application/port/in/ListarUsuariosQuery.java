package com.traderoperation.planos.application.port.in;

public interface ListarUsuariosQuery {

    /** Listagem administrativa paginada. {@code busca} nulo ou vazio ignora o filtro. */
    PaginaUsuarios listar(String busca, int pagina, int tamanho);
}
