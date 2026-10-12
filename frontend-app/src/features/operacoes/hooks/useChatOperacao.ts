import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { useCallback } from 'react';
import { chatApi, type MensagemResumo } from '../api/chatApi';
import { useAssinarTopico } from '@/shared/tempo-real/useAssinarTopico';

export function useMensagens(operacaoId: string | null) {
  const queryClient = useQueryClient();

  const aoReceber = useCallback(
    (nova: MensagemResumo) => {
      if (!operacaoId) return;
      queryClient.setQueryData<readonly MensagemResumo[]>(
        ['operacoes', 'chat', operacaoId],
        (atual) => {
          if (!atual) return [nova];
          if (atual.some((m) => m.id === nova.id)) return atual;
          return [...atual, nova];
        },
      );
    },
    [operacaoId, queryClient],
  );

  useAssinarTopico<MensagemResumo>(operacaoId ? `/topic/chat/${operacaoId}` : null, aoReceber);

  return useQuery({
    queryKey: ['operacoes', 'chat', operacaoId] as const,
    queryFn: () => chatApi.listar(operacaoId as string),
    enabled: operacaoId != null,
    staleTime: 10_000,
  });
}

export function useEnviarMensagem(operacaoId: string) {
  return useMutation({
    mutationFn: (texto: string) => chatApi.enviar(operacaoId, texto),
    // sem onSuccess: a mensagem volta pelo STOMP e `useMensagens` já anexa (com dedupe por id).
  });
}
