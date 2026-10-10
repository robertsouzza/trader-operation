import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest';
import { renderHook, waitFor } from '@testing-library/react';
import { QueryClientProvider } from '@tanstack/react-query';
import type { ReactNode } from 'react';
import { useCatalogo } from './useCatalogo';
import { novoQueryClient } from '@/test/helpers';

function respostaJson(corpo: unknown): Response {
  return new Response(JSON.stringify(corpo), {
    status: 200,
    headers: { 'Content-Type': 'application/json' },
  });
}

describe('useCatalogo', () => {
  beforeEach(() => {
    document.cookie = 'XSRF-TOKEN=; expires=Thu, 01 Jan 1970 00:00:00 GMT; path=/';
  });

  afterEach(() => {
    vi.restoreAllMocks();
  });

  it('retorna os três planos quando a API responde', async () => {
    vi.spyOn(window, 'fetch').mockResolvedValue(
      respostaJson([
        { plano: 'FREE', direitos: ['VER_OPERACAO_COM_ATRASO'] },
        { plano: 'PRO', direitos: ['USAR_COPILOTO_MT5'] },
        { plano: 'PREMIUM', direitos: ['PEDIR_BACKTEST_IA'] },
      ]),
    );

    const cliente = novoQueryClient();
    const wrapper = ({ children }: { readonly children: ReactNode }) => (
      <QueryClientProvider client={cliente}>{children}</QueryClientProvider>
    );
    const { result } = renderHook(() => useCatalogo(), { wrapper });

    await waitFor(() => expect(result.current.isSuccess).toBe(true));
    expect(result.current.data?.map((p) => p.plano)).toEqual(['FREE', 'PRO', 'PREMIUM']);
  });
});
