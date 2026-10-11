import { useQuery, useQueryClient } from '@tanstack/react-query';
import { useCallback } from 'react';
import { operacoesApi } from '../api/operacoesApi';
import { useAssinarTopico } from '@/shared/tempo-real/useAssinarTopico';

export function useOperacoesAoVivo() {
  const queryClient = useQueryClient();

  const invalidar = useCallback(() => {
    queryClient.invalidateQueries({ queryKey: ['operacoes', 'ao-vivo'] });
  }, [queryClient]);

  // Tempo real: nova operação ou encerramento → re-busca a lista.
  useAssinarTopico<unknown>('/topic/operacoes', invalidar);

  return useQuery({
    queryKey: ['operacoes', 'ao-vivo'] as const,
    queryFn: () => operacoesApi.listarAoVivo(),
    staleTime: 15_000,
  });
}
