import { apiClient } from '@/shared/api/apiClient';

export type Direcao = 'COMPRA' | 'VENDA';
export type StatusOperacao = 'PUBLICADA' | 'ENCERRADA';
export type ResultadoOperacao = 'GAIN' | 'LOSS' | 'NEUTRO' | 'INDEFINIDO';

export interface OperacaoResumo {
  readonly id: string;
  readonly ativo: string;
  readonly direcao: Direcao;
  readonly entrada: number;
  readonly stop: number;
  readonly alvos: readonly number[];
  readonly estrategia: string | null;
  readonly status: StatusOperacao;
  readonly resultado: ResultadoOperacao | null;
  readonly observacao: string | null;
  readonly masterId: string;
  readonly masterNome: string;
  readonly publicadaEm: string;
  readonly encerradaEm: string | null;
}

export interface PublicarOperacaoRequest {
  readonly ativo: string;
  readonly direcao: Direcao;
  readonly entrada: number;
  readonly stop: number;
  readonly alvos: readonly number[];
  readonly estrategia?: string;
}

export interface EncerrarOperacaoRequest {
  readonly resultado: ResultadoOperacao;
  readonly observacao?: string;
}

export const operacoesApi = {
  listarAoVivo: () => apiClient.get<readonly OperacaoResumo[]>('/api/operacoes/ao-vivo'),
  obter: (id: string) => apiClient.get<OperacaoResumo>(`/api/operacoes/${id}`),
  publicar: (req: PublicarOperacaoRequest) =>
    apiClient.post<OperacaoResumo>('/api/operacoes', req),
  encerrar: (id: string, req: EncerrarOperacaoRequest) =>
    apiClient.post<OperacaoResumo>(`/api/operacoes/${id}/encerrar`, req),
};
