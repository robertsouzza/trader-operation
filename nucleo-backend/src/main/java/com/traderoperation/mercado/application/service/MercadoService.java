package com.traderoperation.mercado.application.service;

import com.traderoperation.mercado.application.port.in.ConsultarCandlesQuery;
import com.traderoperation.mercado.application.port.out.MotorQuantPort;
import com.traderoperation.mercado.domain.Ativo;
import com.traderoperation.mercado.domain.Candle;
import com.traderoperation.mercado.domain.Timeframe;
import java.util.List;
import org.springframework.stereotype.Service;

/** Repassa a consulta de candles para o motor-quant. Sem persistência, sem cache nesta skill. */
@Service
public class MercadoService implements ConsultarCandlesQuery {

    private final MotorQuantPort motor;

    public MercadoService(MotorQuantPort motor) {
        this.motor = motor;
    }

    @Override
    public List<Candle> listar(Ativo ativo, Timeframe timeframe, int limite) {
        return motor.candles(ativo, timeframe, limite);
    }
}
