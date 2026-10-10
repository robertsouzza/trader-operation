import { apiClient } from '@/shared/api/apiClient';

export type Perfil = 'ADMIN' | 'MASTER' | 'CLIENTE';

export interface DadosUsuario {
  readonly id: string;
  readonly nome: string;
  readonly email: string;
  readonly perfil: Perfil;
}

export interface Credenciais {
  readonly email: string;
  readonly senha: string;
}

export const autenticacaoApi = {
  login: (credenciais: Credenciais) =>
    apiClient.post<DadosUsuario>('/api/auth/login', credenciais),
  logout: () => apiClient.post<void>('/api/auth/logout'),
  me: () => apiClient.get<DadosUsuario>('/api/auth/me'),
};
