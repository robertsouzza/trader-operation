package com.traderoperation.chat;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.traderoperation.chat.domain.Mensagem;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class MensagemDominioTest {

    @Test
    void mensagemValidaConstruida() {
        Mensagem m = new Mensagem(UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(),
                "olá", Instant.now());
        assertThat(m.texto()).isEqualTo("olá");
    }

    @Test
    void textoVazioFalha() {
        assertThatThrownBy(() -> new Mensagem(UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(),
                "  ", Instant.now()))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void textoMaiorQueTamanhoMaximoFalha() {
        String texto = "a".repeat(Mensagem.TAMANHO_MAXIMO + 1);
        assertThatThrownBy(() -> new Mensagem(UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(),
                texto, Instant.now()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining(String.valueOf(Mensagem.TAMANHO_MAXIMO));
    }
}
