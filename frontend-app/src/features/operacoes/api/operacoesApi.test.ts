import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest';
import { operacoesApi } from './operacoesApi';

function respostaJson(status: number, corpo: unknown): Response {
  return new Response(status === 204 ? null : JSON.stringify(corpo), {
    status,
    headers: corpo === null ? {} : { 'Content-Type': 'application/json' },
  });
}

describe('operacoesApi', () => {
  beforeEach(() => {
    document.cookie = 'XSRF-TOKEN=teste; path=/';
  });

  afterEach(() => {
    vi.restoreAllMocks();
    document.cookie = 'XSRF-TOKEN=; expires=Thu, 01 Jan 1970 00:00:00 GMT; path=/';
  });

  it('publicar envia POST com corpo serializado', async () => {
    const fetchMock = vi
      .spyOn(window, 'fetch')
      .mockResolvedValue(respostaJson(200, { id: 'abc', ativo: 'NAS100' }));

    await operacoesApi.publicar({
      ativo: 'NAS100',
      direcao: 'COMPRA',
      entrada: 20000,
      stop: 19900,
      alvos: [20100],
    });

    const [, init] = fetchMock.mock.calls[0]!;
    expect(init?.method).toBe('POST');
    expect(JSON.parse(init?.body as string)).toEqual({
      ativo: 'NAS100',
      direcao: 'COMPRA',
      entrada: 20000,
      stop: 19900,
      alvos: [20100],
    });
  });

  it('encerrar chama o endpoint certo', async () => {
    const fetchMock = vi
      .spyOn(window, 'fetch')
      .mockResolvedValue(respostaJson(200, { id: 'abc', status: 'ENCERRADA' }));

    await operacoesApi.encerrar('abc', { resultado: 'GAIN', observacao: 'alvo 1' });

    const url = fetchMock.mock.calls[0]![0] as string;
    expect(url).toContain('/api/operacoes/abc/encerrar');
  });
});
