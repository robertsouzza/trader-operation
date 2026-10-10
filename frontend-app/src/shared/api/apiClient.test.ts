import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest';
import { apiClient } from './apiClient';
import { AcessoNegadoError, NaoAutorizadoError } from './erros';

interface RespostaFalsa {
  readonly status: number;
  readonly corpo?: unknown;
}

function montarResposta({ status, corpo }: RespostaFalsa): Response {
  const texto = corpo === undefined ? '' : JSON.stringify(corpo);
  return new Response(status === 204 ? null : texto, {
    status,
    headers: corpo === undefined ? {} : { 'Content-Type': 'application/json' },
  });
}

describe('apiClient', () => {
  beforeEach(() => {
    document.cookie = 'XSRF-TOKEN=; expires=Thu, 01 Jan 1970 00:00:00 GMT; path=/';
  });

  afterEach(() => {
    vi.restoreAllMocks();
  });

  it('GET inclui credentials:include e não envia X-XSRF-TOKEN', async () => {
    const fetchMock = vi
      .spyOn(window, 'fetch')
      .mockResolvedValue(montarResposta({ status: 200, corpo: { ok: true } }));

    await apiClient.get<{ ok: boolean }>('/api/planos');

    expect(fetchMock).toHaveBeenCalledTimes(1);
    const [, init] = fetchMock.mock.calls[0]!;
    expect(init?.credentials).toBe('include');
    expect(init?.method).toBe('GET');
    const cabecalhos = init?.headers as Record<string, string> | undefined;
    expect(cabecalhos?.['X-XSRF-TOKEN']).toBeUndefined();
  });

  it('POST busca o cookie CSRF antes e envia o header X-XSRF-TOKEN', async () => {
    const fetchMock = vi.spyOn(window, 'fetch').mockImplementation(async (entrada) => {
      const url = typeof entrada === 'string' ? entrada : entrada.toString();
      if (url.endsWith('/api/auth/csrf')) {
        document.cookie = 'XSRF-TOKEN=valor-de-teste; path=/';
        return montarResposta({ status: 204 });
      }
      return montarResposta({ status: 200, corpo: { ok: true } });
    });

    await apiClient.post<{ ok: boolean }>('/api/auth/login', { email: 'a@b', senha: 'x' });

    expect(fetchMock).toHaveBeenCalledTimes(2);
    const [, initCsrf] = fetchMock.mock.calls[0]!;
    expect(initCsrf?.method).toBe('GET');
    const [, initPost] = fetchMock.mock.calls[1]!;
    const cabecalhos = initPost?.headers as Record<string, string> | undefined;
    expect(cabecalhos?.['X-XSRF-TOKEN']).toBe('valor-de-teste');
    expect(cabecalhos?.['Content-Type']).toBe('application/json');
  });

  it('traduz 401 em NaoAutorizadoError com a mensagem do backend', async () => {
    vi.spyOn(window, 'fetch').mockResolvedValue(
      montarResposta({ status: 401, corpo: { mensagem: 'E-mail ou senha inválidos.' } }),
    );
    await expect(apiClient.get('/api/auth/me')).rejects.toMatchObject({
      name: 'NaoAutorizadoError',
      status: 401,
      message: 'E-mail ou senha inválidos.',
    });
    await expect(apiClient.get('/api/auth/me')).rejects.toBeInstanceOf(NaoAutorizadoError);
  });

  it('traduz 403 em AcessoNegadoError', async () => {
    vi.spyOn(window, 'fetch').mockResolvedValue(
      montarResposta({ status: 403, corpo: { mensagem: 'Acesso negado.' } }),
    );
    await expect(apiClient.get('/api/admin/usuarios')).rejects.toBeInstanceOf(AcessoNegadoError);
  });

  it('traduz outros erros em ErroApi mantendo o status', async () => {
    vi.spyOn(window, 'fetch').mockResolvedValue(
      montarResposta({ status: 500, corpo: { mensagem: 'Erro interno.' } }),
    );
    await expect(apiClient.get('/api/planos')).rejects.toMatchObject({
      name: 'ErroApi',
      status: 500,
    });
  });

  it('204 devolve undefined sem tentar fazer parse de JSON', async () => {
    vi.spyOn(window, 'fetch').mockResolvedValue(montarResposta({ status: 204 }));
    const resultado = await apiClient.del('/api/auth/logout');
    expect(resultado).toBeUndefined();
  });
});
