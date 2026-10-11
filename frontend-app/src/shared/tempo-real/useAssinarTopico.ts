import { useEffect, useRef } from 'react';
import { stompClient } from './stompClient';

/**
 * Assina um destino STOMP enquanto o componente estiver montado.
 * Passe `destino = null` para desativar temporariamente a assinatura.
 */
export function useAssinarTopico<T>(
  destino: string | null,
  aoReceber: (payload: T) => void,
): void {
  // Mantém o callback atualizado sem derrubar a assinatura a cada render.
  const callbackRef = useRef(aoReceber);
  useEffect(() => {
    callbackRef.current = aoReceber;
  }, [aoReceber]);

  useEffect(() => {
    if (!destino) return;
    const cancelar = stompClient.subscribe<T>(destino, (payload) => {
      callbackRef.current(payload);
    });
    return () => {
      cancelar();
    };
  }, [destino]);
}
