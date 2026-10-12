import { apiClient } from '@/shared/api/apiClient';

export type Ativo = 'NAS100' | 'XAUUSD';
export type Timeframe = 'M1' | 'M5' | 'M15' | 'H1' | 'D1';

export interface Candle {
  readonly ts: string;
  readonly abertura: number;
  readonly maxima: number;
  readonly minima: number;
  readonly fechamento: number;
  readonly volume: number;
}

export const mercadoApi = {
  listarCandles: (ativo: Ativo, timeframe: Timeframe, limite = 200) => {
    const params = new URLSearchParams({
      ativo,
      timeframe,
      limite: String(limite),
    });
    return apiClient.get<readonly Candle[]>(`/api/mercado/candles?${params.toString()}`);
  },
};
