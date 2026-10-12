import { useState, type FormEvent } from 'react';
import { Dialogo } from '@/shared/componentes/Dialogo';
import { Botao } from '@/shared/componentes/Botao';
import { CampoTexto } from '@/shared/componentes/CampoTexto';
import { usePublicarOperacao } from '../hooks/usePublicarOperacao';
import type { Direcao } from '../api/operacoesApi';

interface Props {
  readonly aoFechar: () => void;
}

const DIRECOES: readonly Direcao[] = ['COMPRA', 'VENDA'];

export function FormularioPublicarOperacao({ aoFechar }: Props) {
  const [ativo, setAtivo] = useState('NAS100');
  const [direcao, setDirecao] = useState<Direcao>('COMPRA');
  const [entrada, setEntrada] = useState('');
  const [stop, setStop] = useState('');
  const [alvos, setAlvos] = useState('');
  const [estrategia, setEstrategia] = useState('');
  const [erro, setErro] = useState<string | null>(null);
  const publicar = usePublicarOperacao();

  async function aoSubmeter(evento: FormEvent) {
    evento.preventDefault();
    setErro(null);
    const alvosParsed = alvos
      .split(',')
      .map((v) => Number(v.trim()))
      .filter((v) => Number.isFinite(v) && v > 0);
    if (alvosParsed.length === 0) {
      setErro('Informe pelo menos um alvo numérico, separados por vírgula.');
      return;
    }
    try {
      await publicar.mutateAsync({
        ativo: ativo.trim(),
        direcao,
        entrada: Number(entrada),
        stop: Number(stop),
        alvos: alvosParsed,
        ...(estrategia.trim() ? { estrategia: estrategia.trim() } : {}),
      });
      aoFechar();
    } catch (e) {
      setErro(e instanceof Error ? e.message : 'Não foi possível publicar a operação.');
    }
  }

  return (
    <Dialogo aberto titulo="Publicar nova operação" aoFechar={aoFechar}>
      <form onSubmit={aoSubmeter} className="flex flex-col gap-3">
        <div className="grid grid-cols-2 gap-3">
          <CampoTexto
            rotulo="Ativo"
            value={ativo}
            onChange={(e) => setAtivo(e.target.value.toUpperCase())}
            required
          />
          <div className="flex flex-col gap-1">
            <label htmlFor="direcao-select" className="text-sm font-medium text-texto">
              Direção
            </label>
            <select
              id="direcao-select"
              value={direcao}
              onChange={(e) => setDirecao(e.target.value as Direcao)}
              className="rounded-md border border-borda bg-superficie px-3 py-2 text-sm text-texto focus:border-acento focus:outline-none"
            >
              {DIRECOES.map((d) => (
                <option key={d} value={d}>
                  {d}
                </option>
              ))}
            </select>
          </div>
        </div>
        <div className="grid grid-cols-2 gap-3">
          <CampoTexto
            rotulo="Entrada"
            type="number"
            step="0.00000001"
            value={entrada}
            onChange={(e) => setEntrada(e.target.value)}
            required
          />
          <CampoTexto
            rotulo="Stop"
            type="number"
            step="0.00000001"
            value={stop}
            onChange={(e) => setStop(e.target.value)}
            required
          />
        </div>
        <CampoTexto
          rotulo="Alvos"
          value={alvos}
          onChange={(e) => setAlvos(e.target.value)}
          placeholder="20100, 20200"
          dica="Separe por vírgula. Pelo menos um alvo é obrigatório."
          required
        />
        <CampoTexto
          rotulo="Estratégia (opcional)"
          value={estrategia}
          onChange={(e) => setEstrategia(e.target.value)}
          placeholder="triangulo-rompimento"
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
          <Botao type="submit" carregando={publicar.isPending}>
            Publicar
          </Botao>
        </div>
      </form>
    </Dialogo>
  );
}
