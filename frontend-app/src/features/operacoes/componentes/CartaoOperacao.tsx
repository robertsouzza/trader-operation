import { Link } from 'react-router-dom';
import type { OperacaoResumo } from '../api/operacoesApi';

const FORMATO_DATA = new Intl.DateTimeFormat('pt-BR', { timeStyle: 'short', dateStyle: 'short' });

function formatarData(iso: string | null): string {
  if (!iso) return '—';
  try {
    return FORMATO_DATA.format(new Date(iso));
  } catch {
    return iso;
  }
}

export function CartaoOperacao({ operacao }: { readonly operacao: OperacaoResumo }) {
  const cor = operacao.direcao === 'COMPRA' ? 'text-sucesso' : 'text-erro';
  return (
    <Link
      to={`/operacoes/${operacao.id}`}
      className="flex flex-col gap-2 rounded-lg border border-borda bg-superficie p-4 transition-colors hover:border-acento"
    >
      <div className="flex items-center justify-between">
        <div className="flex items-center gap-2">
          <span className="text-base font-semibold text-texto">{operacao.ativo}</span>
          <span className={`text-xs font-semibold uppercase ${cor}`}>{operacao.direcao}</span>
        </div>
        <span className="text-xs text-texto-sutil">{formatarData(operacao.publicadaEm)}</span>
      </div>
      <div className="grid grid-cols-3 gap-2 text-sm">
        <Celula rotulo="Entrada" valor={String(operacao.entrada)} />
        <Celula rotulo="Stop" valor={String(operacao.stop)} />
        <Celula rotulo="Alvos" valor={operacao.alvos.join(' / ')} />
      </div>
      {operacao.estrategia ? (
        <span className="text-xs text-texto-sutil">Estratégia: {operacao.estrategia}</span>
      ) : null}
      <span className="text-xs text-texto-sutil">por {operacao.masterNome}</span>
    </Link>
  );
}

function Celula({ rotulo, valor }: { readonly rotulo: string; readonly valor: string }) {
  return (
    <div className="flex flex-col">
      <span className="text-xs uppercase tracking-wide text-texto-sutil">{rotulo}</span>
      <span className="text-sm text-texto">{valor}</span>
    </div>
  );
}
