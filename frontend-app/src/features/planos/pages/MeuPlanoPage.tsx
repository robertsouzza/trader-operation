import { useMeuPlano } from '../hooks/useMeuPlano';
import { Carregando } from '@/shared/componentes/Carregando';

const FORMATO_DATA = new Intl.DateTimeFormat('pt-BR', {
  dateStyle: 'medium',
});

function formatarData(iso: string | null): string {
  if (!iso) return 'não expira';
  try {
    return FORMATO_DATA.format(new Date(iso));
  } catch {
    return iso;
  }
}

function formatarDireito(direito: string): string {
  return direito.toLowerCase().replace(/_/g, ' ');
}

export function MeuPlanoPage() {
  const consulta = useMeuPlano();

  if (consulta.isLoading) return <Carregando mensagem="Carregando seu plano..." />;
  if (consulta.isError || !consulta.data) {
    return <p className="text-erro">Não foi possível carregar o plano.</p>;
  }

  const { plano, inicioEm, fimEm, origem, direitos } = consulta.data;

  return (
    <section className="flex flex-col gap-6">
      <header>
        <h1 className="text-2xl font-semibold text-texto">Meu plano</h1>
        <p className="text-sm text-texto-sutil">
          Seus direitos liberados vêm da união do perfil com o plano ativo.
        </p>
      </header>
      <div className="grid gap-4 rounded-lg border border-borda bg-superficie p-5 sm:grid-cols-2">
        <Campo rotulo="Plano" valor={plano} destaque />
        <Campo rotulo="Origem" valor={origem ?? 'padrão'} />
        <Campo rotulo="Início" valor={formatarData(inicioEm)} />
        <Campo rotulo="Fim" valor={formatarData(fimEm)} />
      </div>
      <div className="rounded-lg border border-borda bg-superficie p-5">
        <h2 className="mb-3 text-sm font-semibold uppercase tracking-wide text-texto-sutil">
          Direitos liberados ({direitos.length})
        </h2>
        {direitos.length === 0 ? (
          <p className="text-sm text-texto-sutil">Nenhum direito liberado por enquanto.</p>
        ) : (
          <ul className="flex flex-wrap gap-2">
            {direitos.map((d) => (
              <li
                key={d}
                className="rounded-full border border-borda px-3 py-1 text-xs text-texto"
              >
                {formatarDireito(d)}
              </li>
            ))}
          </ul>
        )}
      </div>
    </section>
  );
}

function Campo({
  rotulo,
  valor,
  destaque = false,
}: {
  readonly rotulo: string;
  readonly valor: string;
  readonly destaque?: boolean;
}) {
  return (
    <div className="flex flex-col gap-1">
      <span className="text-xs uppercase tracking-wide text-texto-sutil">{rotulo}</span>
      <span className={destaque ? 'text-lg font-semibold text-acento' : 'text-sm text-texto'}>
        {valor}
      </span>
    </div>
  );
}
