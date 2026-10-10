import { useQuery } from '@tanstack/react-query';
import { planosApi } from '../api/planosApi';

export function useMeuPlano() {
  return useQuery({
    queryKey: ['meuPlano'] as const,
    queryFn: () => planosApi.meuPlano(),
    staleTime: 60_000,
  });
}
