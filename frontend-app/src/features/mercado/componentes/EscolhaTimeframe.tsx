import type { Timeframe } from '../api/mercadoApi';

const OPCOES: readonly { readonly valor: Timeframe; readonly rotulo: string }[] = [
  { valor: 'M1', rotulo: '1 minuto' },
  { valor: 'M5', rotulo: '5 minutos' },
  { valor: 'M15', rotulo: '15 minutos' },
  { valor: 'H1', rotulo: '1 hora' },
  { valor: 'D1', rotulo: '1 dia' },
];

interface Props {
  readonly valor: Timeframe;
  readonly aoMudar: (tf: Timeframe) => void;
}

export function EscolhaTimeframe({ valor, aoMudar }: Props) {
  return (
    <div className="flex items-center gap-2">
      <label htmlFor="timeframe-select" className="text-sm text-texto-sutil">
        Timeframe
      </label>
      <select
        id="timeframe-select"
        value={valor}
        onChange={(e) => aoMudar(e.target.value as Timeframe)}
        className="rounded-md border border-borda bg-superficie px-3 py-1 text-sm text-texto focus:border-acento focus:outline-none"
      >
        {OPCOES.map((o) => (
          <option key={o.valor} value={o.valor}>
            {o.rotulo}
          </option>
        ))}
      </select>
    </div>
  );
}
