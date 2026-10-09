package com.traderoperation.planos;

import static org.assertj.core.api.Assertions.assertThat;

import com.traderoperation.planos.application.service.MatrizDeDireitos;
import com.traderoperation.planos.domain.Direito;
import com.traderoperation.planos.domain.Plano;
import org.junit.jupiter.api.Test;

class MatrizDeDireitosTest {

    private final MatrizDeDireitos matriz = new MatrizDeDireitos();

    @Test
    void freeNaoTemCopiloto() {
        assertThat(matriz.doPlano(Plano.FREE)).doesNotContain(Direito.USAR_COPILOTO_MT5);
    }

    @Test
    void proTemCopilotoENaoTemBacktest() {
        assertThat(matriz.doPlano(Plano.PRO))
                .contains(Direito.USAR_COPILOTO_MT5, Direito.USAR_CHAT_IA)
                .doesNotContain(Direito.PEDIR_BACKTEST_IA);
    }

    @Test
    void premiumTemBacktestEPublicarNaVitrine() {
        assertThat(matriz.doPlano(Plano.PREMIUM))
                .contains(Direito.PEDIR_BACKTEST_IA, Direito.PUBLICAR_NA_VITRINE, Direito.USAR_COPILOTO_MT5);
    }

    @Test
    void masterTemPublicarOperacaoMesmoComFree() {
        assertThat(matriz.paraUsuario("MASTER", Plano.FREE))
                .contains(Direito.PUBLICAR_OPERACAO, Direito.APROVAR_ESTRATEGIA);
    }

    @Test
    void clientePremiumNaoAdministra() {
        assertThat(matriz.paraUsuario("CLIENTE", Plano.PREMIUM))
                .contains(Direito.PEDIR_BACKTEST_IA)
                .doesNotContain(Direito.ADMINISTRAR_USUARIOS, Direito.PUBLICAR_OPERACAO);
    }

    @Test
    void adminTemAdministrarUsuariosIndependenteDoPlano() {
        assertThat(matriz.paraUsuario("ADMIN", Plano.FREE))
                .contains(Direito.ADMINISTRAR_USUARIOS);
    }

    @Test
    void perfilDesconhecidoNaoGanhaDireitosDePerfil() {
        assertThat(matriz.doPerfil("VISITANTE")).isEmpty();
    }
}
