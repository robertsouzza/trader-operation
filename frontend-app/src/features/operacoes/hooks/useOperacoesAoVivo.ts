import { useQuery } from '@tanstack/react-query';
import { operacoesApi } from '../api/operacoesApi';

export function useOperacoesAoVivo() {
  return useQuery({
    queryKey: ['operacoes', 'ao-vivo'] as const,
    queryFn: () => operacoesApi.listarAoVivo(),
    staleTime: 15_000,
  });
}
