import { useQuery } from '@tanstack/react-query';
import { mercadoApi, type Ativo, type Timeframe } from '../api/mercadoApi';

export function useCandles(
  ativo: Ativo | null,
  timeframe: Timeframe | null,
  limite = 200,
) {
  return useQuery({
    queryKey: ['mercado', 'candles', ativo, timeframe, limite] as const,
    queryFn: () => mercadoApi.listarCandles(ativo as Ativo, timeframe as Timeframe, limite),
    enabled: ativo != null && timeframe != null,
    staleTime: 15_000,
  });
}
