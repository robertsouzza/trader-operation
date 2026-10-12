import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest';
import { render, screen } from '@testing-library/react';
import { ListaOperacoesPage } from './ListaOperacoesPage';
import { Envoltorio, estadoAutenticado } from '@/test/helpers';

function respostaJson(corpo: unknown): Response {
  return new Response(JSON.stringify(corpo), {
    status: 200,
    headers: { 'Content-Type': 'application/json' },
  });
}

const operacoesExemplo = [
  {
    id: '00000000-0000-0000-0000-000000000001',
    ativo: 'NAS100',
    direcao: 'COMPRA',
    entrada: 20000,
    stop: 19900,
    alvos: [20100, 20200],
    estrategia: 'triangulo',
    status: 'PUBLICADA',
    resultado: null,
    observacao: null,
    masterId: 'master-id',
    masterNome: 'Master Dev',
    publicadaEm: '2026-10-11T15:00:00Z',
    encerradaEm: null,
  },
];

describe('ListaOperacoesPage', () => {
  beforeEach(() => {
    document.cookie = 'XSRF-TOKEN=teste; path=/';
  });

  afterEach(() => {
    vi.restoreAllMocks();
    document.cookie = 'XSRF-TOKEN=; expires=Thu, 01 Jan 1970 00:00:00 GMT; path=/';
  });

  it('renderiza cartões das operações recebidas', async () => {
    vi.spyOn(window, 'fetch').mockResolvedValue(respostaJson(operacoesExemplo));

    render(
      <Envoltorio
        usuario={estadoAutenticado({ direitos: new Set(['VER_OPERACAO_TEMPO_REAL']) })}
      >
        <ListaOperacoesPage />
      </Envoltorio>,
    );

    expect(await screen.findByText('NAS100')).toBeInTheDocument();
    expect(screen.getByText('COMPRA')).toBeInTheDocument();
    expect(screen.getByText(/Master Dev/)).toBeInTheDocument();
    // Sem direito PUBLICAR_OPERACAO, botão não aparece
    expect(screen.queryByRole('button', { name: /publicar operação/i })).toBeNull();
  });

  it('mostra botão de publicar para quem tem PUBLICAR_OPERACAO', async () => {
    vi.spyOn(window, 'fetch').mockResolvedValue(respostaJson([]));

    render(
      <Envoltorio
        usuario={estadoAutenticado({
          direitos: new Set(['VER_OPERACAO_TEMPO_REAL', 'PUBLICAR_OPERACAO']),
        })}
      >
        <ListaOperacoesPage />
      </Envoltorio>,
    );

    expect(await screen.findByRole('button', { name: /publicar operação/i })).toBeInTheDocument();
  });
});
