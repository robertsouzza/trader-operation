import { useCatalogo } from '../hooks/useCatalogo';
import { useUsuarioAtual } from '@/shared/seguranca/contextoUsuarioAtual';
import { Carregando } from '@/shared/componentes/Carregando';
import type { Plano } from '../api/planosApi';

const ORDEM: readonly Plano[] = ['FREE', 'PRO', 'PREMIUM'];

const DESCRICAO: Record<Plano, string> = {
  FREE: 'Visão com atraso para começar.',
  PRO: 'Operações em tempo real e Copiloto no MT5.',
  PREMIUM: 'Tudo do PRO + testar e publicar estratégias próprias.',
};

function formatarNome(direito: string): string {
  return direito.toLowerCase().replace(/_/g, ' ');
}

export function CatalogoPlanosPage() {
  const consulta = useCatalogo();
  const { plano: planoAtual } = useUsuarioAtual();

  if (consulta.isLoading) return <Carregando mensagem="Carregando catálogo..." />;
  if (consulta.isError || !consulta.data) {
    return <p className="text-erro">Não foi possível carregar o catálogo.</p>;
  }

  const porPlano = new Map(consulta.data.map((c) => [c.plano, c.direitos]));

  return (
    <section className="flex flex-col gap-6">
      <header>
        <h1 className="text-2xl font-semibold text-texto">Planos</h1>
        <p className="text-sm text-texto-sutil">
          Compare os direitos de cada plano. Fale com um administrador para trocar o seu.
        </p>
      </header>
      <div className="grid gap-4 lg:grid-cols-3">
        {ORDEM.map((plano) => {
          const direitos = porPlano.get(plano) ?? [];
          const atual = planoAtual?.plano === plano;
          return (
            <article
              key={plano}
              className={`flex flex-col gap-3 rounded-lg border bg-superficie p-5 ${
                atual ? 'border-acento' : 'border-borda'
              }`}
            >
              <div className="flex items-center justify-between">
                <h2 className="text-lg font-semibold text-texto">{plano}</h2>
                {atual ? (
                  <span className="rounded-full bg-acento px-2 py-0.5 text-xs font-medium text-fundo">
                    plano atual
                  </span>
                ) : null}
              </div>
              <p className="text-sm text-texto-sutil">{DESCRICAO[plano]}</p>
              <ul className="flex flex-col gap-1 text-sm text-texto">
                {direitos.length === 0 ? (
                  <li className="text-texto-sutil">Sem direitos listados.</li>
                ) : (
                  direitos.map((d) => (
                    <li key={d} className="flex gap-2">
                      <span className="text-sucesso">✓</span>
                      <span>{formatarNome(d)}</span>
                    </li>
                  ))
                )}
              </ul>
            </article>
          );
        })}
      </div>
    </section>
  );
}
