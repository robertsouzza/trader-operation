import { useState, type FormEvent } from 'react';
import { useLocation, useNavigate } from 'react-router-dom';
import { useLogin } from '../hooks/useLogin';
import { Botao } from '@/shared/componentes/Botao';
import { CampoTexto } from '@/shared/componentes/CampoTexto';
import { NaoAutorizadoError } from '@/shared/api/erros';

interface Estado {
  readonly de?: string;
}

export function LoginPage() {
  const navegar = useNavigate();
  const localizacao = useLocation();
  const estado = (localizacao.state ?? {}) as Estado;
  const [email, setEmail] = useState('');
  const [senha, setSenha] = useState('');
  const [erro, setErro] = useState<string | null>(null);
  const login = useLogin();

  async function aoSubmeter(evento: FormEvent) {
    evento.preventDefault();
    setErro(null);
    try {
      await login.mutateAsync({ email: email.trim(), senha });
      navegar(estado.de ?? '/', { replace: true });
    } catch (e) {
      if (e instanceof NaoAutorizadoError) {
        setErro(e.message);
      } else if (e instanceof Error) {
        setErro(e.message);
      } else {
        setErro('Não foi possível entrar. Tente novamente.');
      }
    }
  }

  return (
    <main className="flex min-h-screen items-center justify-center bg-fundo p-6">
      <form
        onSubmit={aoSubmeter}
        className="flex w-full max-w-sm flex-col gap-4 rounded-lg border border-borda bg-superficie p-6"
        aria-label="Entrar na plataforma"
      >
        <div>
          <h1 className="text-xl font-semibold text-texto">Trader Operation</h1>
          <p className="text-sm text-texto-sutil">Entre com seu e-mail e senha.</p>
        </div>
        <CampoTexto
          rotulo="E-mail"
          type="email"
          autoComplete="email"
          required
          value={email}
          onChange={(e) => setEmail(e.target.value)}
        />
        <CampoTexto
          rotulo="Senha"
          type="password"
          autoComplete="current-password"
          required
          value={senha}
          onChange={(e) => setSenha(e.target.value)}
        />
        {erro ? (
          <div role="alert" className="rounded-md bg-erro/10 px-3 py-2 text-sm text-erro">
            {erro}
          </div>
        ) : null}
        <Botao type="submit" carregando={login.isPending}>
          Entrar
        </Botao>
      </form>
    </main>
  );
}
