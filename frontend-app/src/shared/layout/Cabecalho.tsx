import { useNavigate } from 'react-router-dom';
import { useUsuarioAtual } from '@/shared/seguranca/contextoUsuarioAtual';
import { useLogout } from '@/features/autenticacao/hooks/useLogout';
import { Botao } from '@/shared/componentes/Botao';

export function Cabecalho() {
  const { usuario, plano } = useUsuarioAtual();
  const logout = useLogout();
  const navegar = useNavigate();

  async function aoSair() {
    try {
      await logout.mutateAsync();
    } finally {
      navegar('/login', { replace: true });
    }
  }

  return (
    <header className="flex items-center justify-between border-b border-borda bg-superficie px-6 py-3">
      <div className="flex items-center gap-3">
        <span className="text-lg font-semibold text-acento">Trader Operation</span>
      </div>
      <div className="flex items-center gap-4">
        {usuario ? (
          <div className="flex items-center gap-2 text-sm">
            <span className="text-texto">{usuario.nome}</span>
            <span className="rounded-full bg-superficie-elevada px-2 py-0.5 text-xs font-medium text-acento">
              {usuario.perfil}
            </span>
            {plano ? (
              <span className="rounded-full bg-superficie-elevada px-2 py-0.5 text-xs font-medium text-texto-sutil">
                {plano.plano}
              </span>
            ) : null}
          </div>
        ) : null}
        <Botao variante="ghost" onClick={aoSair} carregando={logout.isPending}>
          Sair
        </Botao>
      </div>
    </header>
  );
}
