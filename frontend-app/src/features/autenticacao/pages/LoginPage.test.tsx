import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest';
import { render, screen, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { LoginPage } from './LoginPage';
import { Envoltorio } from '@/test/helpers';

function respostaJson(status: number, corpo: unknown): Response {
  return new Response(JSON.stringify(corpo), {
    status,
    headers: { 'Content-Type': 'application/json' },
  });
}

describe('LoginPage', () => {
  beforeEach(() => {
    document.cookie = 'XSRF-TOKEN=teste; path=/';
  });

  afterEach(() => {
    vi.restoreAllMocks();
    document.cookie = 'XSRF-TOKEN=; expires=Thu, 01 Jan 1970 00:00:00 GMT; path=/';
  });

  it('mostra mensagem genérica em 401', async () => {
    vi.spyOn(window, 'fetch').mockResolvedValue(
      respostaJson(401, { mensagem: 'E-mail ou senha inválidos.' }),
    );

    render(
      <Envoltorio>
        <LoginPage />
      </Envoltorio>,
    );

    await userEvent.type(screen.getByLabelText('E-mail'), 'master@trader.local');
    await userEvent.type(screen.getByLabelText('Senha'), 'errada');
    await userEvent.click(screen.getByRole('button', { name: /entrar/i }));

    const alerta = await screen.findByRole('alert');
    expect(alerta).toHaveTextContent('E-mail ou senha inválidos.');
  });

  it('envia e-mail e senha ao backend', async () => {
    const fetchMock = vi
      .spyOn(window, 'fetch')
      .mockResolvedValue(
        respostaJson(200, {
          id: 'id',
          nome: 'Master Dev',
          email: 'master@trader.local',
          perfil: 'MASTER',
        }),
      );

    render(
      <Envoltorio>
        <LoginPage />
      </Envoltorio>,
    );

    await userEvent.type(screen.getByLabelText('E-mail'), 'master@trader.local');
    await userEvent.type(screen.getByLabelText('Senha'), 'trader123');
    await userEvent.click(screen.getByRole('button', { name: /entrar/i }));

    await waitFor(() => expect(fetchMock).toHaveBeenCalled());
    const [, init] = fetchMock.mock.calls[0]!;
    expect(init?.method).toBe('POST');
    expect(JSON.parse(init?.body as string)).toEqual({
      email: 'master@trader.local',
      senha: 'trader123',
    });
  });
});
