import { useQuery } from '@tanstack/react-query';
import { operacoesApi } from '../api/operacoesApi';

export function useOperacao(id: string | null) {
  return useQuery({
    queryKey: ['operacoes', 'detalhe', id] as const,
    queryFn: () => operacoesApi.obter(id as string),
    enabled: id != null,
    staleTime: 30_000,
  });
}
