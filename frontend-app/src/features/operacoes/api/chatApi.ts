import { apiClient } from '@/shared/api/apiClient';

export interface MensagemResumo {
  readonly id: string;
  readonly operacaoId: string;
  readonly autorId: string;
  readonly autorNome: string;
  readonly texto: string;
  readonly enviadaEm: string;
}

export const chatApi = {
  listar: (operacaoId: string, limite = 100) =>
    apiClient.get<readonly MensagemResumo[]>(
      `/api/operacoes/${operacaoId}/mensagens?limite=${limite}`,
    ),
  enviar: (operacaoId: string, texto: string) =>
    apiClient.post<MensagemResumo>(`/api/operacoes/${operacaoId}/mensagens`, { texto }),
};
