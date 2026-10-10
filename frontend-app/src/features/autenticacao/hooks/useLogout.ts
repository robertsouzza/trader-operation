import { useMutation, useQueryClient } from '@tanstack/react-query';
import { autenticacaoApi } from '../api/autenticacaoApi';

export function useLogout() {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: () => autenticacaoApi.logout(),
    onSettled: async () => {
      queryClient.removeQueries({ queryKey: ['usuarioAtual'] });
      queryClient.removeQueries({ queryKey: ['meuPlano'] });
      await queryClient.invalidateQueries();
    },
  });
}
