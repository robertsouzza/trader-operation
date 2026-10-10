import { Link } from 'react-router-dom';
import { useUsuarioAtual } from '@/shared/seguranca/contextoUsuarioAtual';

interface Atalho {
  readonly titulo: string;
  readonly descricao: string;
  readonly direito?: string;
  readonly caminho: string | null;
  readonly perfilAlvo?: string;
}

const ATALHOS: readonly Atalho[] = [
  {
    titulo: 'Meu plano',
    descricao: 'Veja o plano atual e os direitos liberados.',
    caminho: '/meu-plano',
  },
  {
    titulo: 'Catálogo de planos',
    descricao: 'Compare FREE, PRO e PREMIUM.',
    caminho: '/planos',
  },
  {
    titulo: 'Administrar usuários',
    descricao: 'Lista e atribui plano manualmente.',
    direito: 'ADMINISTRAR_USUARIOS',
    caminho: '/admin/usuarios',
  },
  {
    titulo: 'Operações em tempo real',
    descricao: 'Gráfico ao vivo com entrada, stop e alvos (em breve).',
    direito: 'VER_OPERACAO_TEMPO_REAL',
    caminho: null,
  },
  {
    titulo: 'Chat com a IA',
    descricao: 'Conversa com o analista de IA sobre a operação (em breve).',
    direito: 'USAR_CHAT_IA',
    caminho: null,
  },
  {
    titulo: 'Pedir backtest à IA',
    descricao: 'Teste de estratégia com relatório padronizado (em breve).',
    direito: 'PEDIR_BACKTEST_IA',
    caminho: null,
  },
];

export function HomePage() {
  const { usuario, direitos } = useUsuarioAtual();
  const visiveis = ATALHOS.filter((a) => !a.direito || direitos.has(a.direito));

  return (
    <section className="flex flex-col gap-6">
      <header>
        <h1 className="text-2xl font-semibold text-texto">
          Olá{usuario ? `, ${usuario.nome}` : ''}!
        </h1>
        <p className="text-sm text-texto-sutil">
          Escolha por onde começar. Novas áreas aparecem conforme seu plano libera direitos.
        </p>
      </header>
      <ul className="grid gap-4 sm:grid-cols-2 lg:grid-cols-3">
        {visiveis.map((atalho) => {
          const conteudo = (
            <div
              className={`flex h-full flex-col gap-2 rounded-lg border border-borda bg-superficie p-4 transition-colors ${
                atalho.caminho ? 'hover:border-acento' : 'opacity-70'
              }`}
            >
              <h2 className="text-base font-semibold text-texto">{atalho.titulo}</h2>
              <p className="text-sm text-texto-sutil">{atalho.descricao}</p>
            </div>
          );
          return (
            <li key={atalho.titulo}>
              {atalho.caminho ? (
                <Link to={atalho.caminho} className="block h-full">
                  {conteudo}
                </Link>
              ) : (
                conteudo
              )}
            </li>
          );
        })}
      </ul>
    </section>
  );
}
