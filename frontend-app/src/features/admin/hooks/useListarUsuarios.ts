import { useQuery } from '@tanstack/react-query';
import { adminUsuariosApi } from '../api/adminUsuariosApi';

interface Opcoes {
  readonly busca: string;
  readonly pagina: number;
  readonly tamanho: number;
}

export function useListarUsuarios({ busca, pagina, tamanho }: Opcoes) {
  return useQuery({
    queryKey: ['adminUsuarios', { busca, pagina, tamanho }] as const,
    queryFn: () => adminUsuariosApi.listar({ busca, pagina, tamanho }),
    staleTime: 30_000,
  });
}
