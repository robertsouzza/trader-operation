package com.traderoperation.planos.infrastructure.adapter.out.persistence;

import com.traderoperation.planos.application.port.out.UsuarioLookupPort;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

/**
 * Leitura direta da tabela {@code usuarios} para não acoplar o módulo planos ao módulo autenticação.
 * Nunca lê {@code senha_hash}.
 */
@Component
class UsuarioLookupAdapter implements UsuarioLookupPort {

    private static final String SELECT_BASE = "SELECT id, nome, email, perfil FROM usuarios WHERE ativo = TRUE";

    private final JdbcTemplate jdbc;

    UsuarioLookupAdapter(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    public Optional<UsuarioResumido> buscarPorId(UUID id) {
        List<UsuarioResumido> achados = jdbc.query(
                SELECT_BASE + " AND id = ?",
                (rs, i) -> new UsuarioResumido(
                        (UUID) rs.getObject("id"),
                        rs.getString("nome"),
                        rs.getString("email"),
                        rs.getString("perfil")),
                id);
        return achados.stream().findFirst();
    }

    @Override
    public PaginaUsuarioResumido listar(String busca, int pagina, int tamanho) {
        int paginaSegura = Math.max(pagina, 0);
        int tamanhoSeguro = Math.min(Math.max(tamanho, 1), 100);
        String filtro = (busca == null || busca.isBlank()) ? null : "%" + busca.toLowerCase() + "%";

        long total;
        List<UsuarioResumido> itens;
        if (filtro == null) {
            total = Optional.ofNullable(jdbc.queryForObject(
                    "SELECT count(*) FROM usuarios WHERE ativo = TRUE", Long.class)).orElse(0L);
            itens = jdbc.query(
                    SELECT_BASE + " ORDER BY nome LIMIT ? OFFSET ?",
                    (rs, i) -> new UsuarioResumido(
                            (UUID) rs.getObject("id"),
                            rs.getString("nome"),
                            rs.getString("email"),
                            rs.getString("perfil")),
                    tamanhoSeguro, (long) paginaSegura * tamanhoSeguro);
        } else {
            total = Optional.ofNullable(jdbc.queryForObject(
                    "SELECT count(*) FROM usuarios WHERE ativo = TRUE AND (lower(nome) LIKE ? OR email LIKE ?)",
                    Long.class, filtro, filtro)).orElse(0L);
            itens = jdbc.query(
                    SELECT_BASE + " AND (lower(nome) LIKE ? OR email LIKE ?) ORDER BY nome LIMIT ? OFFSET ?",
                    (rs, i) -> new UsuarioResumido(
                            (UUID) rs.getObject("id"),
                            rs.getString("nome"),
                            rs.getString("email"),
                            rs.getString("perfil")),
                    filtro, filtro, tamanhoSeguro, (long) paginaSegura * tamanhoSeguro);
        }
        return new PaginaUsuarioResumido(total, List.copyOf(itens));
    }
}
