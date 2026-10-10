import { useMutation, useQueryClient } from '@tanstack/react-query';
import { autenticacaoApi, type Credenciais } from '../api/autenticacaoApi';

export function useLogin() {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: (credenciais: Credenciais) => autenticacaoApi.login(credenciais),
    onSuccess: async () => {
      await queryClient.invalidateQueries({ queryKey: ['usuarioAtual'] });
      await queryClient.invalidateQueries({ queryKey: ['meuPlano'] });
    },
  });
}
