import { apiClient } from '@/shared/api/apiClient';

export type Plano = 'FREE' | 'PRO' | 'PREMIUM';
export type OrigemAssinatura = 'MANUAL' | 'PAGAMENTO';

export interface CatalogoPlano {
  readonly plano: Plano;
  readonly direitos: readonly string[];
}

export interface PlanoAtual {
  readonly plano: Plano;
  readonly inicioEm: string | null;
  readonly fimEm: string | null;
  readonly origem: OrigemAssinatura | null;
  readonly direitos: readonly string[];
}

export const planosApi = {
  catalogo: () => apiClient.get<readonly CatalogoPlano[]>('/api/planos'),
  meuPlano: () => apiClient.get<PlanoAtual>('/api/me/plano'),
};
