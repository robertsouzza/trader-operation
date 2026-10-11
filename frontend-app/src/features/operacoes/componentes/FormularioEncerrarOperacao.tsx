import { useState, type FormEvent } from 'react';
import { Dialogo } from '@/shared/componentes/Dialogo';
import { Botao } from '@/shared/componentes/Botao';
import { CampoTexto } from '@/shared/componentes/CampoTexto';
import { useEncerrarOperacao } from '../hooks/useEncerrarOperacao';
import type { OperacaoResumo, ResultadoOperacao } from '../api/operacoesApi';

interface Props {
  readonly operacao: OperacaoResumo;
  readonly aoFechar: () => void;
}

const RESULTADOS: readonly ResultadoOperacao[] = ['GAIN', 'LOSS', 'NEUTRO', 'INDEFINIDO'];

export function FormularioEncerrarOperacao({ operacao, aoFechar }: Props) {
  const [resultado, setResultado] = useState<ResultadoOperacao>('GAIN');
  const [observacao, setObservacao] = useState('');
  const [erro, setErro] = useState<string | null>(null);
  const encerrar = useEncerrarOperacao();

  async function aoSubmeter(evento: FormEvent) {
    evento.preventDefault();
    setErro(null);
    try {
      await encerrar.mutateAsync({
        id: operacao.id,
        request: {
          resultado,
          ...(observacao.trim() ? { observacao: observacao.trim() } : {}),
        },
      });
      aoFechar();
    } catch (e) {
      setErro(e instanceof Error ? e.message : 'Não foi possível encerrar a operação.');
    }
  }

  return (
    <Dialogo aberto titulo={`Encerrar ${operacao.ativo}`} aoFechar={aoFechar}>
      <form onSubmit={aoSubmeter} className="flex flex-col gap-3">
        <div className="flex flex-col gap-1">
          <label htmlFor="resultado-select" className="text-sm font-medium text-texto">
            Resultado
          </label>
          <select
            id="resultado-select"
            value={resultado}
            onChange={(e) => setResultado(e.target.value as ResultadoOperacao)}
            className="rounded-md border border-borda bg-superficie px-3 py-2 text-sm text-texto focus:border-acento focus:outline-none"
          >
            {RESULTADOS.map((r) => (
              <option key={r} value={r}>
                {r}
              </option>
            ))}
          </select>
        </div>
        <CampoTexto
          rotulo="Observação (opcional)"
          value={observacao}
          onChange={(e) => setObservacao(e.target.value)}
          placeholder="alvo 1 batido"
        />
        {erro ? (
          <div role="alert" className="rounded-md bg-erro/10 px-3 py-2 text-sm text-erro">
            {erro}
          </div>
        ) : null}
        <div className="flex justify-end gap-2">
          <Botao variante="ghost" onClick={aoFechar} type="button">
            Cancelar
          </Botao>
          <Botao type="submit" carregando={encerrar.isPending}>
            Confirmar encerramento
          </Botao>
        </div>
      </form>
    </Dialogo>
  );
}
