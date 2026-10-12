package com.traderoperation.mercado.infrastructure.adapter.out.motor;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import com.traderoperation.mercado.application.port.out.MotorQuantPort;
import com.traderoperation.mercado.domain.Ativo;
import com.traderoperation.mercado.domain.Timeframe;
import com.traderoperation.shared.config.TraderProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

class MotorQuantHttpAdapterTest {

    private static final String TOKEN = "shared-token-de-teste";
    private static final String BASE = "http://motor-quant-fake:8091";

    private MockRestServiceServer servidor;
    private MotorQuantPort adapter;

    @BeforeEach
    void setUp() {
        RestClient.Builder builder = RestClient.builder();
        servidor = MockRestServiceServer.bindTo(builder).build();
        TraderProperties props = new TraderProperties(
                null, null, null, null,
                new TraderProperties.Motor(BASE, TOKEN, 2));
        adapter = new MotorQuantHttpAdapter(props, builder);
    }

    @Test
    void chamaMotorQuantComHeaderInternalToken() {
        servidor.expect(requestTo(BASE + "/api/motor/candles?ativo=NAS100&timeframe=M1&limite=3"))
                .andExpect(method(HttpMethod.GET))
                .andExpect(header("X-INTERNAL-TOKEN", TOKEN))
                .andRespond(withSuccess(
                        """
                        [{"ts":"2026-10-12T12:00:00Z","abertura":100,"maxima":101,"minima":99,"fechamento":100.5,"volume":1},
                         {"ts":"2026-10-12T12:01:00Z","abertura":100.5,"maxima":101.5,"minima":100,"fechamento":101,"volume":2},
                         {"ts":"2026-10-12T12:02:00Z","abertura":101,"maxima":102,"minima":100.5,"fechamento":101.5,"volume":3}]
                        """,
                        MediaType.APPLICATION_JSON));

        var candles = adapter.candles(Ativo.NAS100, Timeframe.M1, 3);

        assertThat(candles).hasSize(3);
        assertThat(candles.get(0).fechamento().doubleValue()).isEqualTo(100.5);
        servidor.verify();
    }

    @Test
    void traduz403DoMotorEmIllegalStateException() {
        servidor.expect(requestTo(BASE + "/api/motor/candles?ativo=NAS100&timeframe=M1&limite=5"))
                .andRespond(withStatus(HttpStatus.FORBIDDEN));

        assertThatThrownBy(() -> adapter.candles(Ativo.NAS100, Timeframe.M1, 5))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("MOTOR_SHARED_TOKEN");
    }

    @Test
    void traduz500DoMotorEmIllegalStateException() {
        servidor.expect(requestTo(BASE + "/api/motor/candles?ativo=XAUUSD&timeframe=M5&limite=10"))
                .andRespond(withStatus(HttpStatus.INTERNAL_SERVER_ERROR));

        assertThatThrownBy(() -> adapter.candles(Ativo.XAUUSD, Timeframe.M5, 10))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("500");
    }

    @Test
    void listaVaziaQuandoCorpoNulo() {
        servidor.expect(requestTo(BASE + "/api/motor/candles?ativo=NAS100&timeframe=M1&limite=1"))
                .andRespond(withSuccess("[]", MediaType.APPLICATION_JSON));

        assertThat(adapter.candles(Ativo.NAS100, Timeframe.M1, 1)).isEmpty();
    }
}
