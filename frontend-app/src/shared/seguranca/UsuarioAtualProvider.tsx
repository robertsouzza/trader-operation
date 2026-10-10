import { useQueries } from '@tanstack/react-query';
import { useMemo, type ReactNode } from 'react';
import { autenticacaoApi } from '@/features/autenticacao/api/autenticacaoApi';
import { planosApi } from '@/features/planos/api/planosApi';
import { NaoAutorizadoError } from '@/shared/api/erros';
import { ContextoUsuarioAtual, type EstadoUsuarioAtual } from './contextoUsuarioAtual';

const CONJUNTO_VAZIO: ReadonlySet<string> = new Set();

export function UsuarioAtualProvider({ children }: { readonly children: ReactNode }) {
  const resultados = useQueries({
    queries: [
      {
        queryKey: ['usuarioAtual'] as const,
        queryFn: () => autenticacaoApi.me(),
        retry: (contador: number, erro: unknown) =>
          erro instanceof NaoAutorizadoError ? false : contador < 1,
        staleTime: 60_000,
      },
      {
        queryKey: ['meuPlano'] as const,
        queryFn: () => planosApi.meuPlano(),
        retry: (contador: number, erro: unknown) =>
          erro instanceof NaoAutorizadoError ? false : contador < 1,
        staleTime: 60_000,
      },
    ],
  });

  const [consultaUsuario, consultaPlano] = resultados;

  const estado = useMemo<EstadoUsuarioAtual>(() => {
    const naoAutenticado =
      consultaUsuario?.error instanceof NaoAutorizadoError ||
      consultaPlano?.error instanceof NaoAutorizadoError;

    const carregando =
      !naoAutenticado && (consultaUsuario?.isLoading === true || consultaPlano?.isLoading === true);

    const usuario = consultaUsuario?.data ?? null;
    const plano = consultaPlano?.data ?? null;
    const direitos: ReadonlySet<string> = plano ? new Set(plano.direitos) : CONJUNTO_VAZIO;

    return { usuario, plano, direitos, carregando, naoAutenticado };
  }, [consultaUsuario, consultaPlano]);

  return (
    <ContextoUsuarioAtual.Provider value={estado}>
      {children}
    </ContextoUsuarioAtual.Provider>
  );
}
