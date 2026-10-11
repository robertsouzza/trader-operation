import { useMutation, useQueryClient } from '@tanstack/react-query';
import { operacoesApi, type PublicarOperacaoRequest } from '../api/operacoesApi';

export function usePublicarOperacao() {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: (req: PublicarOperacaoRequest) => operacoesApi.publicar(req),
    onSuccess: async () => {
      await queryClient.invalidateQueries({ queryKey: ['operacoes', 'ao-vivo'] });
    },
  });
}
