import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { chatApi, type MensagemResumo } from '../api/chatApi';

export function useMensagens(operacaoId: string | null) {
  return useQuery({
    queryKey: ['operacoes', 'chat', operacaoId] as const,
    queryFn: () => chatApi.listar(operacaoId as string),
    enabled: operacaoId != null,
    staleTime: 10_000,
  });
}

export function useEnviarMensagem(operacaoId: string) {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: (texto: string) => chatApi.enviar(operacaoId, texto),
    onSuccess: (nova) => {
      queryClient.setQueryData<readonly MensagemResumo[]>(
        ['operacoes', 'chat', operacaoId],
        (atual) => (atual ? [...atual, nova] : [nova]),
      );
    },
  });
}
