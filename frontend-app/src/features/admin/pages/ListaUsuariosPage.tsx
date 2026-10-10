import { useEffect, useState } from 'react';
import { useListarUsuarios } from '../hooks/useListarUsuarios';
import { Botao } from '@/shared/componentes/Botao';
import { CampoTexto } from '@/shared/componentes/CampoTexto';
import { Carregando } from '@/shared/componentes/Carregando';
import { DialogoTrocarPlano } from './DialogoTrocarPlano';
import type { ResumoUsuario } from '../api/adminUsuariosApi';

const TAMANHO = 20;
const FORMATO_DATA = new Intl.DateTimeFormat('pt-BR', { dateStyle: 'medium' });

function formatarFim(iso: string | null): string {
  if (!iso) return '—';
  try {
    return FORMATO_DATA.format(new Date(iso));
  } catch {
    return iso;
  }
}

export function ListaUsuariosPage() {
  const [busca, setBusca] = useState('');
  const [buscaAplicada, setBuscaAplicada] = useState('');
  const [pagina, setPagina] = useState(0);
  const [emEdicao, setEmEdicao] = useState<ResumoUsuario | null>(null);

  useEffect(() => {
    const timer = setTimeout(() => {
      setBuscaAplicada(busca);
      setPagina(0);
    }, 300);
    return () => clearTimeout(timer);
  }, [busca]);

  const consulta = useListarUsuarios({ busca: buscaAplicada, pagina, tamanho: TAMANHO });

  return (
    <section className="flex flex-col gap-6">
      <header>
        <h1 className="text-2xl font-semibold text-texto">Usuários</h1>
        <p className="text-sm text-texto-sutil">
          Listagem administrativa. Troque o plano manualmente quando necessário.
        </p>
      </header>

      <CampoTexto
        rotulo="Buscar"
        placeholder="nome ou e-mail"
        value={busca}
        onChange={(e) => setBusca(e.target.value)}
      />

      {consulta.isLoading ? (
        <Carregando mensagem="Carregando usuários..." />
      ) : consulta.isError || !consulta.data ? (
        <p className="text-erro">Não foi possível carregar a lista.</p>
      ) : (
        <>
          <div className="overflow-x-auto rounded-lg border border-borda">
            <table className="min-w-full text-sm">
              <thead className="bg-superficie-elevada text-left text-xs uppercase tracking-wide text-texto-sutil">
                <tr>
                  <th className="px-4 py-3">Nome</th>
                  <th className="px-4 py-3">E-mail</th>
                  <th className="px-4 py-3">Perfil</th>
                  <th className="px-4 py-3">Plano atual</th>
                  <th className="px-4 py-3">Expira em</th>
                  <th className="px-4 py-3"></th>
                </tr>
              </thead>
              <tbody className="divide-y divide-borda bg-superficie">
                {consulta.data.itens.length === 0 ? (
                  <tr>
                    <td colSpan={6} className="px-4 py-6 text-center text-texto-sutil">
                      Nenhum usuário encontrado.
                    </td>
                  </tr>
                ) : (
                  consulta.data.itens.map((u) => (
                    <tr key={u.id} className="text-texto">
                      <td className="px-4 py-3">{u.nome}</td>
                      <td className="px-4 py-3 text-texto-sutil">{u.email}</td>
                      <td className="px-4 py-3">
                        <span className="rounded-full bg-superficie-elevada px-2 py-0.5 text-xs">
                          {u.perfil}
                        </span>
                      </td>
                      <td className="px-4 py-3">
                        <span className="rounded-full bg-superficie-elevada px-2 py-0.5 text-xs text-acento">
                          {u.planoAtual}
                        </span>
                      </td>
                      <td className="px-4 py-3 text-texto-sutil">
                        {formatarFim(u.assinaturaAtivaFimEm)}
                      </td>
                      <td className="px-4 py-3 text-right">
                        <Botao variante="secundario" onClick={() => setEmEdicao(u)}>
                          Trocar plano
                        </Botao>
                      </td>
                    </tr>
                  ))
                )}
              </tbody>
            </table>
          </div>
          <div className="flex items-center justify-between text-sm text-texto-sutil">
            <span>
              Página {pagina + 1} — {consulta.data.total} usuário(s) no total
            </span>
            <div className="flex gap-2">
              <Botao
                variante="ghost"
                disabled={pagina === 0}
                onClick={() => setPagina((p) => Math.max(p - 1, 0))}
              >
                Anterior
              </Botao>
              <Botao
                variante="ghost"
                disabled={(pagina + 1) * TAMANHO >= consulta.data.total}
                onClick={() => setPagina((p) => p + 1)}
              >
                Próxima
              </Botao>
            </div>
          </div>
        </>
      )}

      {emEdicao ? (
        <DialogoTrocarPlano usuario={emEdicao} aoFechar={() => setEmEdicao(null)} />
      ) : null}
    </section>
  );
}
