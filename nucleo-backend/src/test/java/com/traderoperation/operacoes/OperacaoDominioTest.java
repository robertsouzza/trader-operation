package com.traderoperation.operacoes;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.traderoperation.operacoes.domain.Direcao;
import com.traderoperation.operacoes.domain.Operacao;
import com.traderoperation.operacoes.domain.ResultadoOperacao;
import com.traderoperation.operacoes.domain.StatusOperacao;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class OperacaoDominioTest {

    private static final UUID MASTER = UUID.randomUUID();
    private static final Instant AGORA = Instant.parse("2026-10-10T15:00:00Z");

    @Test
    void publicarCompraValidaCriaOperacaoAtiva() {
        Operacao op = Operacao.publicar(
                UUID.randomUUID(), "NAS100", Direcao.COMPRA,
                BigDecimal.valueOf(20000), BigDecimal.valueOf(19900),
                List.of(BigDecimal.valueOf(20100), BigDecimal.valueOf(20200)),
                "triangulo", MASTER, AGORA);

        assertThat(op.status()).isEqualTo(StatusOperacao.PUBLICADA);
        assertThat(op.resultado()).isNull();
        assertThat(op.encerradaEm()).isNull();
        assertThat(op.alvos()).hasSize(2);
    }

    @Test
    void compraComStopAcimaDaEntradaFalha() {
        assertThatThrownBy(() -> Operacao.publicar(
                UUID.randomUUID(), "NAS100", Direcao.COMPRA,
                BigDecimal.valueOf(20000), BigDecimal.valueOf(20100),
                List.of(BigDecimal.valueOf(20200)),
                null, MASTER, AGORA))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("stop");
    }

    @Test
    void vendaComAlvoAcimaDaEntradaFalha() {
        assertThatThrownBy(() -> Operacao.publicar(
                UUID.randomUUID(), "NAS100", Direcao.VENDA,
                BigDecimal.valueOf(20000), BigDecimal.valueOf(20100),
                List.of(BigDecimal.valueOf(20200)),
                null, MASTER, AGORA))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("alvo");
    }

    @Test
    void listaDeAlvosVaziaFalha() {
        assertThatThrownBy(() -> Operacao.publicar(
                UUID.randomUUID(), "NAS100", Direcao.COMPRA,
                BigDecimal.valueOf(20000), BigDecimal.valueOf(19900),
                List.of(), null, MASTER, AGORA))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void encerrarMudaStatusEResultado() {
        Operacao op = Operacao.publicar(
                UUID.randomUUID(), "NAS100", Direcao.COMPRA,
                BigDecimal.valueOf(20000), BigDecimal.valueOf(19900),
                List.of(BigDecimal.valueOf(20100)),
                null, MASTER, AGORA);
        Instant depois = AGORA.plusSeconds(60);

        Operacao enc = op.encerrar(ResultadoOperacao.GAIN, "alvo 1 batido", depois);

        assertThat(enc.status()).isEqualTo(StatusOperacao.ENCERRADA);
        assertThat(enc.resultado()).isEqualTo(ResultadoOperacao.GAIN);
        assertThat(enc.encerradaEm()).isEqualTo(depois);
        assertThat(enc.observacao()).isEqualTo("alvo 1 batido");
        // original continua imutável
        assertThat(op.status()).isEqualTo(StatusOperacao.PUBLICADA);
    }

    @Test
    void encerrarOperacaoJaEncerradaFalha() {
        Operacao op = Operacao.publicar(
                UUID.randomUUID(), "NAS100", Direcao.COMPRA,
                BigDecimal.valueOf(20000), BigDecimal.valueOf(19900),
                List.of(BigDecimal.valueOf(20100)),
                null, MASTER, AGORA);
        Operacao enc = op.encerrar(ResultadoOperacao.LOSS, null, AGORA.plusSeconds(10));

        assertThatThrownBy(() -> enc.encerrar(ResultadoOperacao.GAIN, null, AGORA.plusSeconds(20)))
                .isInstanceOf(IllegalStateException.class);
    }
}
