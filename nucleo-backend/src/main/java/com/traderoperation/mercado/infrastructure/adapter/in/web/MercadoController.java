package com.traderoperation.mercado.infrastructure.adapter.in.web;

import com.traderoperation.mercado.application.port.in.ConsultarCandlesQuery;
import com.traderoperation.mercado.domain.Ativo;
import com.traderoperation.mercado.domain.Candle;
import com.traderoperation.mercado.domain.Timeframe;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import java.util.List;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Mercado")
@RestController
@RequestMapping("/api/mercado")
@Validated
class MercadoController {

    private final ConsultarCandlesQuery consultar;

    MercadoController(ConsultarCandlesQuery consultar) {
        this.consultar = consultar;
    }

    @Operation(summary = "Lista candles do ativo/timeframe (proxy para o motor-quant)")
    @GetMapping("/candles")
    @PreAuthorize("hasAuthority('VER_OPERACAO_TEMPO_REAL')")
    List<Candle> listarCandles(
            @RequestParam Ativo ativo,
            @RequestParam Timeframe timeframe,
            @RequestParam(defaultValue = "200") @Min(1) @Max(2000) int limite) {
        return consultar.listar(ativo, timeframe, limite);
    }
}
