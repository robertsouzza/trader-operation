package com.traderoperation.mercado.infrastructure.adapter.out.motor;

import com.traderoperation.mercado.application.port.out.MotorQuantPort;
import com.traderoperation.mercado.domain.Ativo;
import com.traderoperation.mercado.domain.Candle;
import com.traderoperation.mercado.domain.Timeframe;
import com.traderoperation.shared.config.TraderProperties;
import java.util.List;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

/**
 * Cliente HTTP do motor-quant: adiciona o header {@code X-INTERNAL-TOKEN} (D-23/D-28).
 * 4xx do motor significa config errada deste lado (401/403 do token, 404 de rota) — vira
 * {@link IllegalStateException}, não um erro para o usuário final. 5xx vira exceção genérica.
 * Timeout usa o default do RestClient; controle fino entra se virar demanda real.
 */
@Component
class MotorQuantHttpAdapter implements MotorQuantPort {

    static final String HEADER_TOKEN = "X-INTERNAL-TOKEN";

    private final RestClient http;

    MotorQuantHttpAdapter(TraderProperties props, RestClient.Builder builder) {
        TraderProperties.Motor cfg = props.motor();
        this.http = builder
                .baseUrl(cfg.baseUrl())
                .defaultHeader(HEADER_TOKEN, cfg.sharedToken())
                .build();
    }

    @Override
    public List<Candle> candles(Ativo ativo, Timeframe timeframe, int limite) {
        List<Candle> resposta = http.get()
                .uri(uri -> uri.path("/api/motor/candles")
                        .queryParam("ativo", ativo.name())
                        .queryParam("timeframe", timeframe.name())
                        .queryParam("limite", limite)
                        .build())
                .retrieve()
                .onStatus(HttpStatusCode::is4xxClientError, (req, res) -> {
                    throw new IllegalStateException(
                            "Config do motor-quant inválida (status " + res.getStatusCode() + "): "
                                    + "verifique MOTOR_SHARED_TOKEN e TRADER_MOTOR_BASE_URL.");
                })
                .onStatus(HttpStatusCode::is5xxServerError, (req, res) -> {
                    throw new IllegalStateException(
                            "motor-quant respondeu erro " + res.getStatusCode());
                })
                .body(new ParameterizedTypeReference<List<Candle>>() {});
        return resposta == null ? List.of() : resposta;
    }
}
