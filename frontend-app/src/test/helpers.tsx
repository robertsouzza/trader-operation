import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import { MemoryRouter } from 'react-router-dom';
import type { ReactNode } from 'react';
import {
  ContextoUsuarioAtual,
  type EstadoUsuarioAtual,
} from '@/shared/seguranca/contextoUsuarioAtual';

export function novoQueryClient(): QueryClient {
  return new QueryClient({
    defaultOptions: {
      queries: { retry: false, staleTime: 0, gcTime: 0 },
      mutations: { retry: false },
    },
  });
}

interface Opcoes {
  readonly rotas?: readonly string[];
  readonly usuario?: EstadoUsuarioAtual;
  readonly queryClient?: QueryClient;
}

export function Envoltorio({
  children,
  rotas = ['/'],
  usuario,
  queryClient,
}: {
  readonly children: ReactNode;
} & Opcoes) {
  const cliente = queryClient ?? novoQueryClient();
  const conteudo = usuario ? (
    <ContextoUsuarioAtual.Provider value={usuario}>{children}</ContextoUsuarioAtual.Provider>
  ) : (
    children
  );
  return (
    <QueryClientProvider client={cliente}>
      <MemoryRouter initialEntries={[...rotas]}>{conteudo}</MemoryRouter>
    </QueryClientProvider>
  );
}

export function estadoAutenticado(parcial: Partial<EstadoUsuarioAtual> = {}): EstadoUsuarioAtual {
  return {
    usuario: {
      id: '00000000-0000-0000-0000-000000000003',
      nome: 'Cliente Dev',
      email: 'cliente@trader.local',
      perfil: 'CLIENTE',
    },
    plano: {
      plano: 'PRO',
      inicioEm: '2026-10-01T00:00:00Z',
      fimEm: '2026-11-01T00:00:00Z',
      origem: 'MANUAL',
      direitos: ['USAR_COPILOTO_MT5', 'VER_OPERACAO_TEMPO_REAL'],
    },
    direitos: new Set(['USAR_COPILOTO_MT5', 'VER_OPERACAO_TEMPO_REAL']),
    carregando: false,
    naoAutenticado: false,
    ...parcial,
  };
}

export function estadoNaoAutenticado(): EstadoUsuarioAtual {
  return {
    usuario: null,
    plano: null,
    direitos: new Set(),
    carregando: false,
    naoAutenticado: true,
  };
}
