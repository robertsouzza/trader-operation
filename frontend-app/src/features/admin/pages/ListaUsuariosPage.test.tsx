import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest';
import { render, screen, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { ListaUsuariosPage } from './ListaUsuariosPage';
import { Envoltorio, estadoAutenticado } from '@/test/helpers';

function respostaJson(status: number, corpo: unknown): Response {
  return new Response(status === 204 ? null : JSON.stringify(corpo), {
    status,
    headers: corpo === null ? {} : { 'Content-Type': 'application/json' },
  });
}

const paginaPadrao = {
  total: 1,
  pagina: 0,
  tamanho: 20,
  itens: [
    {
      id: '00000000-0000-0000-0000-000000000003',
      nome: 'Cliente Dev',
      email: 'cliente@trader.local',
      perfil: 'CLIENTE',
      planoAtual: 'PRO',
      assinaturaAtivaFimEm: '2026-11-01T00:00:00Z',
    },
  ],
};

describe('ListaUsuariosPage', () => {
  beforeEach(() => {
    document.cookie = 'XSRF-TOKEN=teste; path=/';
  });

  afterEach(() => {
    vi.restoreAllMocks();
    document.cookie = 'XSRF-TOKEN=; expires=Thu, 01 Jan 1970 00:00:00 GMT; path=/';
  });

  it('mostra os usuários da API e abre o diálogo de troca', async () => {
    const fetchMock = vi.spyOn(window, 'fetch').mockImplementation(async (entrada, init) => {
      const url = typeof entrada === 'string' ? entrada : entrada.toString();
      if (url.includes('/api/admin/usuarios') && (!init || init.method === 'GET' || !init.method)) {
        return respostaJson(200, paginaPadrao);
      }
      if (url.includes('/plano') && init?.method === 'PUT') {
        return respostaJson(204, null);
      }
      return respostaJson(404, { mensagem: 'Rota não mapeada no teste' });
    });

    render(
      <Envoltorio usuario={estadoAutenticado({ direitos: new Set(['ADMINISTRAR_USUARIOS']) })}>
        <ListaUsuariosPage />
      </Envoltorio>,
    );

    expect(await screen.findByText('Cliente Dev')).toBeInTheDocument();
    await userEvent.click(screen.getByRole('button', { name: /trocar plano/i }));
    expect(await screen.findByRole('dialog')).toBeInTheDocument();

    await userEvent.selectOptions(screen.getByLabelText('Plano'), 'FREE');
    await userEvent.click(screen.getByRole('button', { name: /confirmar/i }));

    await waitFor(() => {
      const put = fetchMock.mock.calls.find(
        ([, init]) => init?.method === 'PUT',
      );
      expect(put).toBeDefined();
    });
  });
});
