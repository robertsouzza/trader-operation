package com.traderoperation.planos.application.port.out;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Leitura mínima da tabela de usuários para o módulo de planos — só o que a listagem
 * administrativa e o filtro JWT precisam. Nunca retorna {@code senha_hash}.
 */
public interface UsuarioLookupPort {

    Optional<UsuarioResumido> buscarPorId(UUID id);

    PaginaUsuarioResumido listar(String busca, int pagina, int tamanho);

    record UsuarioResumido(UUID id, String nome, String email, String perfil) {
    }

    record PaginaUsuarioResumido(long total, List<UsuarioResumido> itens) {
    }
}
