import { useState, type FormEvent } from 'react';
import { Botao } from '@/shared/componentes/Botao';
import { Carregando } from '@/shared/componentes/Carregando';
import { useUsuarioAtual } from '@/shared/seguranca/contextoUsuarioAtual';
import { useEnviarMensagem, useMensagens } from '../hooks/useChatOperacao';

const FORMATO_HORA = new Intl.DateTimeFormat('pt-BR', { timeStyle: 'short' });

function hora(iso: string): string {
  try {
    return FORMATO_HORA.format(new Date(iso));
  } catch {
    return iso;
  }
}

export function ChatOperacao({ operacaoId }: { readonly operacaoId: string }) {
  const consulta = useMensagens(operacaoId);
  const enviar = useEnviarMensagem(operacaoId);
  const { direitos, usuario } = useUsuarioAtual();
  const podeLer = direitos.has('LER_CHAT_OPERACAO');
  const podeEnviar = direitos.has('PARTICIPAR_CHAT_OPERACAO');
  const [texto, setTexto] = useState('');

  async function aoSubmeter(evento: FormEvent) {
    evento.preventDefault();
    const valor = texto.trim();
    if (!valor) return;
    try {
      await enviar.mutateAsync(valor);
      setTexto('');
    } catch {
      // o erro já é exibido pelo hook
    }
  }

  if (!podeLer) {
    return (
      <div className="rounded-lg border border-borda bg-superficie p-4 text-sm text-texto-sutil">
        Seu plano atual não libera o chat da operação.
      </div>
    );
  }

  if (consulta.isLoading) return <Carregando mensagem="Carregando chat..." />;
  if (consulta.isError || !consulta.data) {
    return <p className="text-erro">Não foi possível carregar o chat.</p>;
  }

  return (
    <section className="flex h-96 flex-col rounded-lg border border-borda bg-superficie">
      <header className="border-b border-borda px-4 py-2 text-sm font-semibold text-texto">
        Chat da operação
      </header>
      <ul className="flex-1 overflow-y-auto p-4 flex flex-col gap-2">
        {consulta.data.length === 0 ? (
          <li className="text-sm text-texto-sutil">Nenhuma mensagem ainda.</li>
        ) : (
          consulta.data.map((m) => {
            const propria = usuario?.id === m.autorId;
            return (
              <li
                key={m.id}
                className={`flex flex-col gap-0.5 rounded-md px-3 py-2 ${
                  propria
                    ? 'self-end bg-acento/20 text-texto'
                    : 'self-start bg-superficie-elevada text-texto'
                }`}
              >
                <span className="text-xs text-texto-sutil">
                  {m.autorNome} · {hora(m.enviadaEm)}
                </span>
                <span className="text-sm">{m.texto}</span>
              </li>
            );
          })
        )}
      </ul>
      {podeEnviar ? (
        <form onSubmit={aoSubmeter} className="flex gap-2 border-t border-borda p-3">
          <input
            value={texto}
            onChange={(e) => setTexto(e.target.value)}
            maxLength={500}
            placeholder="Digite sua mensagem..."
            className="flex-1 rounded-md border border-borda bg-fundo px-3 py-2 text-sm text-texto placeholder:text-texto-sutil focus:border-acento focus:outline-none"
          />
          <Botao type="submit" carregando={enviar.isPending}>
            Enviar
          </Botao>
        </form>
      ) : (
        <div className="border-t border-borda px-3 py-3 text-xs text-texto-sutil">
          Seu plano permite ler o chat, mas não enviar mensagens.
        </div>
      )}
    </section>
  );
}
