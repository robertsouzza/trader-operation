import { useCallback, useState } from 'react';
import { useAssinarTopico } from '@/shared/tempo-real/useAssinarTopico';

interface Mensagem {
  readonly operacaoId: string;
  readonly participantes: number;
}

/**
 * Assina `/topic/operacoes/{id}` para marcar presença (o PresencaWebSocketListener
 * incrementa o contador no Redis) e `/topic/operacoes/{id}/participantes` para
 * receber o total ao vivo.
 */
export function useContadorParticipantes(operacaoId: string | null): number {
  const [total, setTotal] = useState(0);

  // Assinar o tópico da operação apenas marca presença — não processamos payload aqui.
  useAssinarTopico(
    operacaoId ? `/topic/operacoes/${operacaoId}` : null,
    useCallback(() => undefined, []),
  );

  useAssinarTopico<Mensagem>(
    operacaoId ? `/topic/operacoes/${operacaoId}/participantes` : null,
    useCallback((mensagem: Mensagem) => {
      setTotal(mensagem.participantes);
    }, []),
  );

  return total;
}
