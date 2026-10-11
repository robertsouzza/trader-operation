import { useState } from 'react';
import { Link, useParams } from 'react-router-dom';
import { useOperacao } from '../hooks/useOperacao';
import { useUsuarioAtual } from '@/shared/seguranca/contextoUsuarioAtual';
import { Botao } from '@/shared/componentes/Botao';
import { Carregando } from '@/shared/componentes/Carregando';
import { ChatOperacao } from '../componentes/ChatOperacao';
import { FormularioEncerrarOperacao } from '../componentes/FormularioEncerrarOperacao';
import type { OperacaoResumo } from '../api/operacoesApi';

const FORMATO_DATA = new Intl.DateTimeFormat('pt-BR', {
  dateStyle: 'medium',
  timeStyle: 'short',
});

function formatarData(iso: string | null): string {
  if (!iso) return '—';
  try {
    return FORMATO_DATA.format(new Date(iso));
  } catch {
    return iso;
  }
}

export function OperacaoPage() {
  const { id } = useParams<{ id: string }>();
  const consulta = useOperacao(id ?? null);
  const { usuario, direitos } = useUsuarioAtual();
  const [encerrando, setEncerrando] = useState(false);

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
      <header className="flex items-start justify-between gap-4">
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
        <StatusBadge op={op} />
      </header>

      <div className="grid gap-4 rounded-lg border border-borda bg-superficie p-5 sm:grid-cols-3">
        <Campo rotulo="Entrada" valor={String(op.entrada)} destaque />
        <Campo rotulo="Stop" valor={String(op.stop)} />
        <Campo rotulo="Alvos" valor={op.alvos.join(' / ')} />
        {op.estrategia ? (
          <Campo rotulo="Estratégia" valor={op.estrategia} />
        ) : null}
        {op.resultado ? <Campo rotulo="Resultado" valor={op.resultado} /> : null}
        {op.encerradaEm ? (
          <Campo rotulo="Encerrada em" valor={formatarData(op.encerradaEm)} />
        ) : null}
      </div>

      {op.observacao ? (
        <p className="rounded-lg border border-borda bg-superficie p-4 text-sm text-texto-sutil">
          <span className="mr-2 font-semibold text-texto">Observação:</span>
          {op.observacao}
        </p>
      ) : null}

      {podeEncerrar ? (
        <div className="flex justify-end">
          <Botao variante="perigo" onClick={() => setEncerrando(true)}>
            Encerrar operação
          </Botao>
        </div>
      ) : null}

      <ChatOperacao operacaoId={op.id} />

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
