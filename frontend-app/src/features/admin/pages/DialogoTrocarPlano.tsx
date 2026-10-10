import { useState, type FormEvent } from 'react';
import { Dialogo } from '@/shared/componentes/Dialogo';
import { Botao } from '@/shared/componentes/Botao';
import { CampoTexto } from '@/shared/componentes/CampoTexto';
import { useTrocarPlano } from '../hooks/useTrocarPlano';
import type { ResumoUsuario } from '../api/adminUsuariosApi';
import type { Plano } from '@/features/planos/api/planosApi';

interface Props {
  readonly usuario: ResumoUsuario;
  readonly aoFechar: () => void;
}

const PLANOS: readonly Plano[] = ['FREE', 'PRO', 'PREMIUM'];

export function DialogoTrocarPlano({ usuario, aoFechar }: Props) {
  const [plano, setPlano] = useState<Plano>(usuario.planoAtual);
  const [duracaoDias, setDuracaoDias] = useState('30');
  const [erro, setErro] = useState<string | null>(null);
  const trocar = useTrocarPlano();

  async function aoSubmeter(evento: FormEvent) {
    evento.preventDefault();
    setErro(null);
    const dias = Number(duracaoDias);
    try {
      await trocar.mutateAsync({
        usuarioId: usuario.id,
        request: {
          plano,
          ...(plano === 'FREE' || !Number.isFinite(dias) || dias <= 0
            ? {}
            : { duracaoDias: dias }),
        },
      });
      aoFechar();
    } catch (e) {
      setErro(e instanceof Error ? e.message : 'Não foi possível trocar o plano.');
    }
  }

  return (
    <Dialogo aberto titulo={`Trocar plano de ${usuario.nome}`} aoFechar={aoFechar}>
      <form onSubmit={aoSubmeter} className="flex flex-col gap-4">
        <div className="flex flex-col gap-1">
          <label htmlFor="plano-select" className="text-sm font-medium text-texto">
            Plano
          </label>
          <select
            id="plano-select"
            value={plano}
            onChange={(e) => setPlano(e.target.value as Plano)}
            className="rounded-md border border-borda bg-superficie px-3 py-2 text-sm text-texto focus:border-acento focus:outline-none"
          >
            {PLANOS.map((p) => (
              <option key={p} value={p}>
                {p}
              </option>
            ))}
          </select>
        </div>
        <CampoTexto
          rotulo="Duração (dias)"
          type="number"
          min="1"
          value={duracaoDias}
          onChange={(e) => setDuracaoDias(e.target.value)}
          disabled={plano === 'FREE'}
          dica={plano === 'FREE' ? 'FREE não expira.' : 'Padrão: 30 dias.'}
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
          <Botao type="submit" carregando={trocar.isPending}>
            Confirmar
          </Botao>
        </div>
      </form>
    </Dialogo>
  );
}
