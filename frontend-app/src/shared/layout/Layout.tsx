import { Outlet } from 'react-router-dom';
import { Cabecalho } from './Cabecalho';
import { MenuLateral } from './MenuLateral';

export function Layout() {
  return (
    <div className="flex min-h-screen flex-col">
      <Cabecalho />
      <div className="flex flex-1">
        <MenuLateral />
        <main className="flex-1 overflow-x-hidden">
          <div className="mx-auto w-full max-w-6xl p-6">
            <Outlet />
          </div>
        </main>
      </div>
    </div>
  );
}
