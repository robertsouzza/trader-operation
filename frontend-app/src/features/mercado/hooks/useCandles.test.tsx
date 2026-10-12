import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest';
import { renderHook, waitFor } from '@testing-library/react';
import { QueryClientProvider } from '@tanstack/react-query';
import type { ReactNode } from 'react';
import { useCandles } from './useCandles';
import { novoQueryClient } from '@/test/helpers';

function respostaJson(corpo: unknown): Response {
  return new Response(JSON.stringify(corpo), {
    status: 200,
    headers: { 'Content-Type': 'application/json' },
  });
}

describe('useCandles', () => {
  beforeEach(() => {
    document.cookie = 'XSRF-TOKEN=teste; path=/';
  });

  afterEach(() => {
    vi.restoreAllMocks();
  });

  it('busca candles quando ativo e timeframe estão definidos', async () => {
    vi.spyOn(window, 'fetch').mockResolvedValue(
      respostaJson([
        { ts: '2026-10-12T12:00:00Z', abertura: 100, maxima: 101, minima: 99, fechamento: 100.5, volume: 1 },
      ]),
    );

    const cliente = novoQueryClient();
    const wrapper = ({ children }: { readonly children: ReactNode }) => (
      <QueryClientProvider client={cliente}>{children}</QueryClientProvider>
    );
    const { result } = renderHook(() => useCandles('NAS100', 'M1', 1), { wrapper });

    await waitFor(() => expect(result.current.isSuccess).toBe(true));
    expect(result.current.data).toHaveLength(1);
  });

  it('não dispara fetch quando ativo é null', async () => {
    const fetchMock = vi.spyOn(window, 'fetch');

    const cliente = novoQueryClient();
    const wrapper = ({ children }: { readonly children: ReactNode }) => (
      <QueryClientProvider client={cliente}>{children}</QueryClientProvider>
    );
    renderHook(() => useCandles(null, 'M1'), { wrapper });

    // aguarda um tick para garantir que a query não foi acionada
    await new Promise((r) => setTimeout(r, 20));
    expect(fetchMock).not.toHaveBeenCalled();
  });
});
