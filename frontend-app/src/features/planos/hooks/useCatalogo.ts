import { useQuery } from '@tanstack/react-query';
import { planosApi } from '../api/planosApi';

export function useCatalogo() {
  return useQuery({
    queryKey: ['catalogoPlanos'] as const,
    queryFn: () => planosApi.catalogo(),
    staleTime: 10 * 60_000,
  });
}
