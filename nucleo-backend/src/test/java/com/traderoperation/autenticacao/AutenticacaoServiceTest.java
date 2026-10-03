package com.traderoperation.autenticacao;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.traderoperation.auditoria.application.port.in.RegistrarAuditoriaUseCase;
import com.traderoperation.autenticacao.application.port.out.SenhaPort;
import com.traderoperation.autenticacao.application.port.out.TokenPort;
import com.traderoperation.autenticacao.application.port.out.UsuarioRepositoryPort;
import com.traderoperation.autenticacao.application.service.AutenticacaoService;
import com.traderoperation.autenticacao.domain.Perfil;
import com.traderoperation.autenticacao.domain.Usuario;
import com.traderoperation.shared.error.NaoAutorizadoException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class AutenticacaoServiceTest {

    private static final Usuario CLIENTE = new Usuario(UUID.randomUUID(), "Cliente", "cliente@trader.local",
            "hash-certo", Perfil.CLIENTE, true);

    private final List<String> acoesAuditadas = new ArrayList<>();

    private final UsuarioRepositoryPort usuarios = new UsuarioRepositoryPort() {
        @Override
        public Optional<Usuario> buscarPorEmail(String email) {
            return CLIENTE.email().equals(email) ? Optional.of(CLIENTE) : Optional.empty();
        }

        @Override
        public Optional<Usuario> buscarPorId(UUID id) {
            return CLIENTE.id().equals(id) ? Optional.of(CLIENTE) : Optional.empty();
        }
    };
    private final SenhaPort senhas = (senha, hash) -> "trader123".equals(senha) && "hash-certo".equals(hash);
    private final TokenPort tokens = new TokenPort() {
        @Override
        public String gerarAccess(Usuario u) {
            return "access-" + u.id();
        }

        @Override
        public String gerarRefresh(Usuario u) {
            return "refresh-" + u.id();
        }

        @Override
        public Optional<UUID> validarRefresh(String token) {
            return token.startsWith("refresh-") ? Optional.of(UUID.fromString(token.substring(8))) : Optional.empty();
        }
    };
    private final RegistrarAuditoriaUseCase auditoria =
            (usuarioId, acao, entidade, entidadeId, dados) -> acoesAuditadas.add(acao);

    private final AutenticacaoService servico = new AutenticacaoService(usuarios, senhas, tokens, auditoria);

    @Test
    void loginComCredenciaisCertasEmiteTokensEAudita() {
        var sessao = servico.login("  Cliente@Trader.local ", "trader123");
        assertThat(sessao.usuario().perfil()).isEqualTo("CLIENTE");
        assertThat(sessao.accessToken()).startsWith("access-");
        assertThat(sessao.refreshToken()).startsWith("refresh-");
        assertThat(acoesAuditadas).containsExactly("LOGIN");
    }

    @Test
    void senhaErradaEEmailInexistenteDaoAMesmaMensagem() {
        assertThatThrownBy(() -> servico.login("cliente@trader.local", "errada"))
                .isInstanceOf(NaoAutorizadoException.class).hasMessage("E-mail ou senha inválidos.");
        assertThatThrownBy(() -> servico.login("ninguem@trader.local", "trader123"))
                .isInstanceOf(NaoAutorizadoException.class).hasMessage("E-mail ou senha inválidos.");
        assertThat(acoesAuditadas).containsExactly("LOGIN_FALHA", "LOGIN_FALHA");
    }

    @Test
    void renovarComRefreshValidoEmiteNovoAccess() {
        var sessao = servico.renovar("refresh-" + CLIENTE.id());
        assertThat(sessao.accessToken()).isEqualTo("access-" + CLIENTE.id());
        assertThat(sessao.refreshToken()).isNull();
    }

    @Test
    void renovarSemTokenOuComTokenInvalidoFalha() {
        assertThatThrownBy(() -> servico.renovar(null)).isInstanceOf(NaoAutorizadoException.class);
        assertThatThrownBy(() -> servico.renovar("access-" + CLIENTE.id())).isInstanceOf(NaoAutorizadoException.class);
    }

    @Test
    void auditoriaNaoRecebeSenha() {
        List<Map<String, Object>> dadosGravados = new ArrayList<>();
        var servicoEspiao = new AutenticacaoService(usuarios, senhas, tokens,
                (usuarioId, acao, entidade, entidadeId, dados) -> dadosGravados.add(dados));
        assertThatThrownBy(() -> servicoEspiao.login("cliente@trader.local", "senha-secreta"))
                .isInstanceOf(NaoAutorizadoException.class);
        assertThat(dadosGravados.toString()).doesNotContain("senha-secreta");
    }
}
