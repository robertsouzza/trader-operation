import { useMemo, useState } from 'react';
import { Link, useParams } from 'react-router-dom';
import { useOperacao } from '../hooks/useOperacao';
import { useUsuarioAtual } from '@/shared/seguranca/contextoUsuarioAtual';
import { Botao } from '@/shared/componentes/Botao';
import { Carregando } from '@/shared/componentes/Carregando';
import { ChatOperacao } from '../componentes/ChatOperacao';
import { FormularioEncerrarOperacao } from '../componentes/FormularioEncerrarOperacao';
import { useContadorParticipantes } from '../hooks/useContadorParticipantes';
import type { OperacaoResumo } from '../api/operacoesApi';
import { useCandles } from '@/features/mercado/hooks/useCandles';
import type { Ativo, Timeframe } from '@/features/mercado/api/mercadoApi';
import {
  GraficoCandles,
  type LinhaOperacao,
} from '@/features/mercado/componentes/GraficoCandles';
import { EscolhaTimeframe } from '@/features/mercado/componentes/EscolhaTimeframe';

const FORMATO_DATA = new Intl.DateTimeFormat('pt-BR', {
  dateStyle: 'medium',
  timeStyle: 'short',
});

const ATIVOS_SUPORTADOS: readonly string[] = ['NAS100', 'XAUUSD'];

function formatarData(iso: string | null): string {
  if (!iso) return '—';
  try {
    return FORMATO_DATA.format(new Date(iso));
  } catch {
    return iso;
  }
}

function linhasDaOperacao(op: OperacaoResumo): readonly LinhaOperacao[] {
  const linhas: LinhaOperacao[] = [
    { preco: op.entrada, cor: '#38bdf8', rotulo: `E @ ${op.entrada}` },
    { preco: op.stop, cor: '#ef4444', rotulo: `S @ ${op.stop}` },
  ];
  op.alvos.forEach((alvo, i) => {
    linhas.push({ preco: alvo, cor: '#22c55e', rotulo: `A${i + 1} @ ${alvo}` });
  });
  return linhas;
}

export function OperacaoPage() {
  const { id } = useParams<{ id: string }>();
  const consulta = useOperacao(id ?? null);
  const { usuario, direitos } = useUsuarioAtual();
  const [encerrando, setEncerrando] = useState(false);
  const [timeframe, setTimeframe] = useState<Timeframe>('M1');

  // Hooks ficam antes dos early returns.
  const operacaoAtiva =
    consulta.data && consulta.data.status === 'PUBLICADA' ? consulta.data.id : null;
  const participantes = useContadorParticipantes(operacaoAtiva);

  const ativoDoGrafico: Ativo | null =
    consulta.data && ATIVOS_SUPORTADOS.includes(consulta.data.ativo)
      ? (consulta.data.ativo as Ativo)
      : null;
  const consultaCandles = useCandles(ativoDoGrafico, timeframe, 300);
  const linhas = useMemo(
    () => (consulta.data ? linhasDaOperacao(consulta.data) : []),
    [consulta.data],
  );

  if (!id) {
    return <p className="text-erro">ID de operação inválido.</p>;
  }
  if (consulta.isLoading) return <Carregando mensagem="Carregando operação..." />;
  if (consulta.isError || !consulta.data) {
    return (
      <section className="flex flex-col gap-3">
        <p className="text-erro">Não foi possível carregar a operação.</p>
        <Link to="/operacoes" className="text-sm text-acento hover:underline">
          Voltar para operações
        </Link>
      </section>
    );
  }

  const op = consulta.data;
  const podeEncerrar =
    direitos.has('PUBLICAR_OPERACAO') && usuario?.id === op.masterId && op.status === 'PUBLICADA';

  return (
    <section className="flex flex-col gap-6">
      <header className="flex flex-wrap items-start justify-between gap-4">
        <div>
          <Link to="/operacoes" className="text-xs text-acento hover:underline">
            ← Operações ao vivo
          </Link>
          <h1 className="mt-1 text-2xl font-semibold text-texto">
            {op.ativo}{' '}
            <span className={op.direcao === 'COMPRA' ? 'text-sucesso' : 'text-erro'}>
              {op.direcao}
            </span>
          </h1>
          <p className="text-sm text-texto-sutil">
            Publicada em {formatarData(op.publicadaEm)} por {op.masterNome}
          </p>
        </div>
        <div className="flex flex-col items-end gap-1">
          <StatusBadge op={op} />
          {op.status === 'PUBLICADA' ? (
            <span className="text-xs text-texto-sutil">
              {participantes} participante(s) ao vivo
            </span>
          ) : null}
        </div>
      </header>

      <div className="grid gap-6 lg:grid-cols-[1fr_320px]">
        {/* Coluna principal: gráfico + controles */}
        <div className="flex min-w-0 flex-col gap-3">
          <div className="flex items-center justify-between">
            <h2 className="text-sm font-semibold uppercase tracking-wide text-texto-sutil">
              Gráfico ao vivo
            </h2>
            <EscolhaTimeframe valor={timeframe} aoMudar={setTimeframe} />
          </div>
          <div className="overflow-hidden rounded-lg border border-borda bg-superficie">
            {ativoDoGrafico == null ? (
              <div className="flex items-center justify-center p-10 text-sm text-texto-sutil">
                Gráfico não disponível para o ativo {op.ativo}.
              </div>
            ) : consultaCandles.isLoading ? (
              <Carregando mensagem="Carregando candles..." />
            ) : consultaCandles.isError || !consultaCandles.data ? (
              <div className="flex items-center justify-center p-10 text-sm text-erro">
                Não foi possível carregar os candles.
              </div>
            ) : consultaCandles.data.length === 0 ? (
              <div className="flex items-center justify-center p-10 text-sm text-texto-sutil">
                Sem candles no período para {op.ativo} / {timeframe}.
              </div>
            ) : (
              <GraficoCandles candles={consultaCandles.data} linhas={linhas} />
            )}
          </div>

          {op.observacao ? (
            <p className="rounded-lg border border-borda bg-superficie p-4 text-sm text-texto-sutil">
              <span className="mr-2 font-semibold text-texto">Observação:</span>
              {op.observacao}
            </p>
          ) : null}
        </div>

        {/* Painel lateral: dados da operação + chat */}
        <aside className="flex flex-col gap-4">
          <div className="flex flex-col gap-3 rounded-lg border border-borda bg-superficie p-4">
            <h2 className="text-sm font-semibold uppercase tracking-wide text-texto-sutil">
              Dados da operação
            </h2>
            <Campo rotulo="Entrada" valor={String(op.entrada)} destaque />
            <Campo rotulo="Stop" valor={String(op.stop)} />
            <Campo rotulo="Alvos" valor={op.alvos.join(' / ')} />
            {op.estrategia ? <Campo rotulo="Estratégia" valor={op.estrategia} /> : null}
            {op.resultado ? <Campo rotulo="Resultado" valor={op.resultado} /> : null}
            {op.encerradaEm ? (
              <Campo rotulo="Encerrada em" valor={formatarData(op.encerradaEm)} />
            ) : null}
            {podeEncerrar ? (
              <Botao variante="perigo" onClick={() => setEncerrando(true)}>
                Encerrar operação
              </Botao>
            ) : null}
          </div>

          <ChatOperacao operacaoId={op.id} />
        </aside>
      </div>

      {encerrando ? (
        <FormularioEncerrarOperacao operacao={op} aoFechar={() => setEncerrando(false)} />
      ) : null}
    </section>
  );
}

function StatusBadge({ op }: { readonly op: OperacaoResumo }) {
  const classes =
    op.status === 'PUBLICADA'
      ? 'bg-sucesso/20 text-sucesso'
      : 'bg-superficie-elevada text-texto-sutil';
  return (
    <span className={`rounded-full px-3 py-1 text-xs font-medium ${classes}`}>{op.status}</span>
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
