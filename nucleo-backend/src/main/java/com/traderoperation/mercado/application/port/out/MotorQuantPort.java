package com.traderoperation.mercado.application.port.out;

import com.traderoperation.mercado.domain.Ativo;
import com.traderoperation.mercado.domain.Candle;
import com.traderoperation.mercado.domain.Timeframe;
import java.util.List;

/** Porta de saída: chama o motor-quant adicionando o shared secret (D-23 + D-28). */
public interface MotorQuantPort {

    List<Candle> candles(Ativo ativo, Timeframe timeframe, int limite);
}
