package com.traderoperation.planos;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.traderoperation.planos.application.port.in.PlanoAtual;
import com.traderoperation.planos.application.port.out.AssinaturaRepositoryPort;
import com.traderoperation.planos.application.port.out.UsuarioLookupPort;
import com.traderoperation.planos.application.service.MatrizDeDireitos;
import com.traderoperation.planos.application.service.PlanosService;
import com.traderoperation.planos.domain.Assinatura;
import com.traderoperation.planos.domain.Direito;
import com.traderoperation.planos.domain.OrigemAssinatura;
import com.traderoperation.planos.domain.Plano;
import com.traderoperation.planos.domain.StatusAssinatura;
import com.traderoperation.shared.error.RecursoNaoEncontradoException;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class PlanosServiceTest {

    private static final Instant AGORA = Instant.parse("2026-10-09T12:00:00Z");
    private static final UUID CLIENTE_ID = UUID.fromString("00000000-0000-0000-0000-000000000003");

    private FakeAssinaturas assinaturas;
    private FakeUsuarios usuarios;
    private PlanosService servico;

    @BeforeEach
    void setUp() {
        assinaturas = new FakeAssinaturas();
        usuarios = new FakeUsuarios();
        usuarios.registrar(CLIENTE_ID, "Cliente Dev", "cliente@trader.local", "CLIENTE");
        servico = new PlanosService(assinaturas, usuarios, new MatrizDeDireitos(),
                Clock.fixed(AGORA, ZoneOffset.UTC));
    }

    @Test
    void semAssinaturaAtivaUsuarioEhFree() {
        PlanoAtual atual = servico.consultar(CLIENTE_ID);
        assertThat(atual.plano()).isEqualTo(Plano.FREE);
        assertThat(atual.direitos()).contains(Direito.VER_OPERACAO_COM_ATRASO);
        assertThat(atual.direitos()).doesNotContain(Direito.USAR_COPILOTO_MT5);
    }

    @Test
    void atribuirPlanoCriaAtivaECancelaAnterior() {
        servico.atribuir(CLIENTE_ID, Plano.PRO, OrigemAssinatura.MANUAL, Duration.ofDays(30));
        PlanoAtual aposPro = servico.consultar(CLIENTE_ID);
        assertThat(aposPro.plano()).isEqualTo(Plano.PRO);
        assertThat(aposPro.direitos()).contains(Direito.USAR_COPILOTO_MT5);
        assertThat(aposPro.fimEm()).isEqualTo(AGORA.plus(Duration.ofDays(30)));

        servico.atribuir(CLIENTE_ID, Plano.PREMIUM, OrigemAssinatura.MANUAL, Duration.ofDays(30));
        assertThat(assinaturas.contarAtivasDoUsuario(CLIENTE_ID)).isEqualTo(1);
        assertThat(servico.consultar(CLIENTE_ID).plano()).isEqualTo(Plano.PREMIUM);
    }

    @Test
    void atribuirFreeSemDuracaoNaoDefineFim() {
        servico.atribuir(CLIENTE_ID, Plano.FREE, OrigemAssinatura.MANUAL, null);
        assertThat(servico.consultar(CLIENTE_ID).fimEm()).isNull();
    }

    @Test
    void atribuirUsuarioInexistenteFalha() {
        UUID desconhecido = UUID.randomUUID();
        assertThatThrownBy(() -> servico.atribuir(desconhecido, Plano.PRO, OrigemAssinatura.MANUAL, null))
                .isInstanceOf(RecursoNaoEncontradoException.class);
    }

    @Test
    void verificarDireitosRetornaUniaoPerfilEPlanoComoNomes() {
        servico.atribuir(CLIENTE_ID, Plano.PREMIUM, OrigemAssinatura.MANUAL, Duration.ofDays(30));
        Set<String> direitos = servico.direitosDoUsuario(CLIENTE_ID);
        assertThat(direitos).contains(Direito.PEDIR_BACKTEST_IA.name(), Direito.VER_OPERACAO_TEMPO_REAL.name());
        assertThat(direitos).doesNotContain(Direito.ADMINISTRAR_USUARIOS.name(), Direito.PUBLICAR_OPERACAO.name());
    }

    private static final class FakeAssinaturas implements AssinaturaRepositoryPort {
        private final Map<UUID, Assinatura> porId = new HashMap<>();

        @Override
        public Optional<Assinatura> buscarAtivaDoUsuario(UUID usuarioId) {
            return porId.values().stream()
                    .filter(a -> a.usuarioId().equals(usuarioId) && a.status() == StatusAssinatura.ATIVA)
                    .findFirst();
        }

        @Override
        public Map<UUID, Assinatura> buscarAtivasDeVarios(Set<UUID> usuarioIds) {
            Map<UUID, Assinatura> por = new HashMap<>();
            porId.values().stream()
                    .filter(a -> a.status() == StatusAssinatura.ATIVA && usuarioIds.contains(a.usuarioId()))
                    .forEach(a -> por.put(a.usuarioId(), a));
            return por;
        }

        @Override
        public void salvar(Assinatura a) {
            porId.put(a.id(), a);
        }

        @Override
        public void atualizar(Assinatura a) {
            porId.put(a.id(), a);
        }

        long contarAtivasDoUsuario(UUID usuarioId) {
            return porId.values().stream()
                    .filter(a -> a.usuarioId().equals(usuarioId) && a.status() == StatusAssinatura.ATIVA)
                    .count();
        }
    }

    private static final class FakeUsuarios implements UsuarioLookupPort {
        private final Map<UUID, UsuarioResumido> porId = new HashMap<>();

        void registrar(UUID id, String nome, String email, String perfil) {
            porId.put(id, new UsuarioResumido(id, nome, email, perfil));
        }

        @Override
        public Optional<UsuarioResumido> buscarPorId(UUID id) {
            return Optional.ofNullable(porId.get(id));
        }

        @Override
        public PaginaUsuarioResumido listar(String busca, int pagina, int tamanho) {
            return new PaginaUsuarioResumido(porId.size(), porId.values().stream().toList());
        }
    }
}
