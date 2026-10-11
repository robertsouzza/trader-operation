import { useState } from 'react';
import { useOperacoesAoVivo } from '../hooks/useOperacoesAoVivo';
import { useUsuarioAtual } from '@/shared/seguranca/contextoUsuarioAtual';
import { Botao } from '@/shared/componentes/Botao';
import { Carregando } from '@/shared/componentes/Carregando';
import { CartaoOperacao } from '../componentes/CartaoOperacao';
import { FormularioPublicarOperacao } from '../componentes/FormularioPublicarOperacao';

export function ListaOperacoesPage() {
  const consulta = useOperacoesAoVivo();
  const { direitos } = useUsuarioAtual();
  const podePublicar = direitos.has('PUBLICAR_OPERACAO');
  const [publicando, setPublicando] = useState(false);

  return (
    <section className="flex flex-col gap-6">
      <header className="flex items-center justify-between">
        <div>
          <h1 className="text-2xl font-semibold text-texto">Operações ao vivo</h1>
          <p className="text-sm text-texto-sutil">
            Sinais publicados pelo operador master. Clique para entrar na operação.
          </p>
        </div>
        {podePublicar ? (
          <Botao onClick={() => setPublicando(true)}>Publicar operação</Botao>
        ) : null}
      </header>

      {consulta.isLoading ? (
        <Carregando mensagem="Carregando operações..." />
      ) : consulta.isError || !consulta.data ? (
        <p className="text-erro">Não foi possível carregar as operações.</p>
      ) : consulta.data.length === 0 ? (
        <p className="rounded-lg border border-borda bg-superficie p-6 text-sm text-texto-sutil">
          Nenhuma operação publicada no momento.
        </p>
      ) : (
        <ul className="grid gap-3 md:grid-cols-2 lg:grid-cols-3">
          {consulta.data.map((op) => (
            <li key={op.id}>
              <CartaoOperacao operacao={op} />
            </li>
          ))}
        </ul>
      )}

      {publicando ? <FormularioPublicarOperacao aoFechar={() => setPublicando(false)} /> : null}
    </section>
  );
}
