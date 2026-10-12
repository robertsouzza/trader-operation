import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest';
import { mercadoApi } from './mercadoApi';

function respostaJson(status: number, corpo: unknown): Response {
  return new Response(JSON.stringify(corpo), {
    status,
    headers: { 'Content-Type': 'application/json' },
  });
}

describe('mercadoApi', () => {
  beforeEach(() => {
    document.cookie = 'XSRF-TOKEN=teste; path=/';
  });

  afterEach(() => {
    vi.restoreAllMocks();
    document.cookie = 'XSRF-TOKEN=; expires=Thu, 01 Jan 1970 00:00:00 GMT; path=/';
  });

  it('listarCandles chama a URL com os parâmetros corretos', async () => {
    const fetchMock = vi
      .spyOn(window, 'fetch')
      .mockResolvedValue(respostaJson(200, []));

    await mercadoApi.listarCandles('NAS100', 'M15', 50);

    const url = fetchMock.mock.calls[0]![0] as string;
    expect(url).toContain('/api/mercado/candles?');
    expect(url).toContain('ativo=NAS100');
    expect(url).toContain('timeframe=M15');
    expect(url).toContain('limite=50');
  });

  it('usa 200 como limite padrão quando não passado', async () => {
    const fetchMock = vi.spyOn(window, 'fetch').mockResolvedValue(respostaJson(200, []));

    await mercadoApi.listarCandles('XAUUSD', 'M1');

    const url = fetchMock.mock.calls[0]![0] as string;
    expect(url).toContain('limite=200');
  });

  it('devolve o array de candles do backend', async () => {
    vi.spyOn(window, 'fetch').mockResolvedValue(
      respostaJson(200, [
        {
          ts: '2026-10-12T12:00:00Z',
          abertura: 100,
          maxima: 101,
          minima: 99,
          fechamento: 100.5,
          volume: 1,
        },
      ]),
    );

    const candles = await mercadoApi.listarCandles('NAS100', 'M1', 1);
    expect(candles).toHaveLength(1);
    expect(candles[0]?.fechamento).toBe(100.5);
  });
});
