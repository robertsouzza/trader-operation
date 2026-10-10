import type { ButtonHTMLAttributes } from 'react';

type Variante = 'primario' | 'secundario' | 'perigo' | 'ghost';

interface Props extends ButtonHTMLAttributes<HTMLButtonElement> {
  readonly variante?: Variante;
  readonly carregando?: boolean;
}

const CLASSES: Record<Variante, string> = {
  primario:
    'bg-acento text-fundo hover:bg-acento-forte disabled:bg-superficie-elevada disabled:text-texto-sutil',
  secundario:
    'bg-superficie-elevada text-texto hover:bg-borda disabled:text-texto-sutil',
  perigo: 'bg-erro text-white hover:bg-red-700 disabled:bg-superficie-elevada',
  ghost: 'bg-transparent text-texto hover:bg-superficie-elevada',
};

export function Botao({
  variante = 'primario',
  carregando = false,
  disabled,
  className = '',
  children,
  ...resto
}: Props) {
  const bloqueado = disabled || carregando;
  return (
    <button
      type="button"
      disabled={bloqueado}
      className={`inline-flex items-center justify-center gap-2 rounded-md px-4 py-2 text-sm font-medium transition-colors disabled:cursor-not-allowed ${CLASSES[variante]} ${className}`}
      {...resto}
    >
      {carregando ? (
        <span className="h-4 w-4 animate-spin rounded-full border-2 border-current border-t-transparent" />
      ) : null}
      {children}
    </button>
  );
}
