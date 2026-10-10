import { useId, type InputHTMLAttributes } from 'react';

interface Props extends InputHTMLAttributes<HTMLInputElement> {
  readonly rotulo: string;
  readonly erro?: string;
  readonly dica?: string;
}

export function CampoTexto({ rotulo, erro, dica, id, className = '', ...resto }: Props) {
  const idGerado = useId();
  const idFinal = id ?? idGerado;
  return (
    <div className="flex flex-col gap-1">
      <label htmlFor={idFinal} className="text-sm font-medium text-texto">
        {rotulo}
      </label>
      <input
        id={idFinal}
        className={`rounded-md border border-borda bg-superficie px-3 py-2 text-sm text-texto placeholder:text-texto-sutil focus:border-acento focus:outline-none ${erro ? 'border-erro' : ''} ${className}`}
        aria-invalid={erro ? 'true' : 'false'}
        {...(erro ? { 'aria-describedby': `${idFinal}-erro` } : {})}
        {...resto}
      />
      {erro ? (
        <span id={`${idFinal}-erro`} className="text-xs text-erro">
          {erro}
        </span>
      ) : dica ? (
        <span className="text-xs text-texto-sutil">{dica}</span>
      ) : null}
    </div>
  );
}
