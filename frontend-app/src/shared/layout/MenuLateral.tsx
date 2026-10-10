import { NavLink } from 'react-router-dom';
import { useUsuarioAtual } from '@/shared/seguranca/contextoUsuarioAtual';

interface Item {
  readonly caminho: string;
  readonly rotulo: string;
  readonly direitoRequerido?: string;
}

const ITENS: readonly Item[] = [
  { caminho: '/', rotulo: 'Início' },
  { caminho: '/meu-plano', rotulo: 'Meu plano' },
  { caminho: '/planos', rotulo: 'Catálogo de planos' },
  {
    caminho: '/admin/usuarios',
    rotulo: 'Administrar usuários',
    direitoRequerido: 'ADMINISTRAR_USUARIOS',
  },
];

export function MenuLateral() {
  const { direitos, naoAutenticado } = useUsuarioAtual();
  if (naoAutenticado) return null;

  const visiveis = ITENS.filter(
    (item) => !item.direitoRequerido || direitos.has(item.direitoRequerido),
  );

  return (
    <nav
      aria-label="Navegação principal"
      className="hidden w-56 shrink-0 border-r border-borda bg-superficie p-4 md:block"
    >
      <ul className="flex flex-col gap-1">
        {visiveis.map((item) => (
          <li key={item.caminho}>
            <NavLink
              to={item.caminho}
              end={item.caminho === '/'}
              className={({ isActive }) =>
                `block rounded-md px-3 py-2 text-sm transition-colors ${
                  isActive
                    ? 'bg-superficie-elevada text-acento'
                    : 'text-texto hover:bg-superficie-elevada'
                }`
              }
            >
              {item.rotulo}
            </NavLink>
          </li>
        ))}
      </ul>
    </nav>
  );
}
