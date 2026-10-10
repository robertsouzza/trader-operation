import type { ReactNode } from 'react';
import { Navigate, useLocation } from 'react-router-dom';
import { useUsuarioAtual } from './contextoUsuarioAtual';
import { Carregando } from '@/shared/componentes/Carregando';

export function RotaProtegida({ children }: { readonly children: ReactNode }) {
  const { carregando, naoAutenticado } = useUsuarioAtual();
  const localizacao = useLocation();

  if (carregando) return <Carregando />;
  if (naoAutenticado) {
    return <Navigate to="/login" replace state={{ de: localizacao.pathname }} />;
  }
  return <>{children}</>;
}
