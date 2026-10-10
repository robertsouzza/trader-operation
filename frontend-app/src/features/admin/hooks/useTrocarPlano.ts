import { useMutation, useQueryClient } from '@tanstack/react-query';
import { adminUsuariosApi, type AtribuirPlanoRequest } from '../api/adminUsuariosApi';

interface Variaveis {
  readonly usuarioId: string;
  readonly request: AtribuirPlanoRequest;
}

export function useTrocarPlano() {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: ({ usuarioId, request }: Variaveis) =>
      adminUsuariosApi.trocarPlano(usuarioId, request),
    onSuccess: async () => {
      await queryClient.invalidateQueries({ queryKey: ['adminUsuarios'] });
    },
  });
}
