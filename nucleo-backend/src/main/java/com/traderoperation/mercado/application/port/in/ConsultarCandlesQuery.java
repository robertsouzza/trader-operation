package com.traderoperation.mercado.application.port.in;

import com.traderoperation.mercado.domain.Ativo;
import com.traderoperation.mercado.domain.Candle;
import com.traderoperation.mercado.domain.Timeframe;
import java.util.List;

public interface ConsultarCandlesQuery {

    List<Candle> listar(Ativo ativo, Timeframe timeframe, int limite);
}
