import { useMutation, useQueryClient } from '@tanstack/react-query';
import { operacoesApi, type EncerrarOperacaoRequest } from '../api/operacoesApi';

interface Variaveis {
  readonly id: string;
  readonly request: EncerrarOperacaoRequest;
}

export function useEncerrarOperacao() {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: ({ id, request }: Variaveis) => operacoesApi.encerrar(id, request),
    onSuccess: async (_, vars) => {
      await queryClient.invalidateQueries({ queryKey: ['operacoes', 'ao-vivo'] });
      await queryClient.invalidateQueries({ queryKey: ['operacoes', 'detalhe', vars.id] });
    },
  });
}
