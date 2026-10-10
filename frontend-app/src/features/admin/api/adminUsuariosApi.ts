import { apiClient } from '@/shared/api/apiClient';
import type { Perfil } from '@/features/autenticacao/api/autenticacaoApi';
import type { Plano } from '@/features/planos/api/planosApi';

export interface ResumoUsuario {
  readonly id: string;
  readonly nome: string;
  readonly email: string;
  readonly perfil: Perfil;
  readonly planoAtual: Plano;
  readonly assinaturaAtivaFimEm: string | null;
}

export interface PaginaUsuarios {
  readonly total: number;
  readonly pagina: number;
  readonly tamanho: number;
  readonly itens: readonly ResumoUsuario[];
}

export interface AtribuirPlanoRequest {
  readonly plano: Plano;
  readonly duracaoDias?: number;
}

interface ParametrosListagem {
  readonly busca?: string;
  readonly pagina?: number;
  readonly tamanho?: number;
}

function construirQuery({ busca, pagina = 0, tamanho = 20 }: ParametrosListagem): string {
  const params = new URLSearchParams();
  params.set('pagina', String(pagina));
  params.set('tamanho', String(tamanho));
  if (busca && busca.trim().length > 0) params.set('q', busca.trim());
  return params.toString();
}

export const adminUsuariosApi = {
  listar: (parametros: ParametrosListagem) =>
    apiClient.get<PaginaUsuarios>(`/api/admin/usuarios?${construirQuery(parametros)}`),
  trocarPlano: (usuarioId: string, request: AtribuirPlanoRequest) =>
    apiClient.put<void>(`/api/admin/usuarios/${usuarioId}/plano`, request),
};
