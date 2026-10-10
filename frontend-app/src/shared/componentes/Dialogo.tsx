import { useEffect, type ReactNode } from 'react';

interface Props {
  readonly aberto: boolean;
  readonly titulo: string;
  readonly aoFechar: () => void;
  readonly children: ReactNode;
}

export function Dialogo({ aberto, titulo, aoFechar, children }: Props) {
  useEffect(() => {
    if (!aberto) return;
    function aoApertarEsc(evento: KeyboardEvent) {
      if (evento.key === 'Escape') aoFechar();
    }
    document.addEventListener('keydown', aoApertarEsc);
    return () => document.removeEventListener('keydown', aoApertarEsc);
  }, [aberto, aoFechar]);

  if (!aberto) return null;

  return (
    <div
      role="dialog"
      aria-modal="true"
      aria-label={titulo}
      className="fixed inset-0 z-50 flex items-center justify-center bg-black/60 p-4"
      onClick={aoFechar}
    >
      <div
        className="w-full max-w-md rounded-lg border border-borda bg-superficie p-6 shadow-xl"
        onClick={(e) => e.stopPropagation()}
      >
        <div className="mb-4 flex items-start justify-between gap-4">
          <h2 className="text-lg font-semibold text-texto">{titulo}</h2>
          <button
            type="button"
            onClick={aoFechar}
            aria-label="Fechar"
            className="text-texto-sutil hover:text-texto"
          >
            ✕
          </button>
        </div>
        {children}
      </div>
    </div>
  );
}
