import { createContext, useContext } from 'react';
import type { DadosUsuario } from '@/features/autenticacao/api/autenticacaoApi';
import type { PlanoAtual } from '@/features/planos/api/planosApi';

export interface EstadoUsuarioAtual {
  readonly usuario: DadosUsuario | null;
  readonly plano: PlanoAtual | null;
  readonly direitos: ReadonlySet<string>;
  readonly carregando: boolean;
  readonly naoAutenticado: boolean;
}

export const ContextoUsuarioAtual = createContext<EstadoUsuarioAtual | null>(null);

export function useUsuarioAtual(): EstadoUsuarioAtual {
  const valor = useContext(ContextoUsuarioAtual);
  if (!valor) {
    throw new Error('useUsuarioAtual precisa estar dentro de <UsuarioAtualProvider>.');
  }
  return valor;
}
