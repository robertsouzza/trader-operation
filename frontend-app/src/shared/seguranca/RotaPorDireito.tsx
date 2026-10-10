import type { ReactNode } from 'react';
import { Navigate } from 'react-router-dom';
import { useUsuarioAtual } from './contextoUsuarioAtual';
import { Carregando } from '@/shared/componentes/Carregando';

interface Props {
  readonly direito: string;
  readonly children: ReactNode;
}

export function RotaPorDireito({ direito, children }: Props) {
  const { carregando, naoAutenticado, direitos } = useUsuarioAtual();

  if (carregando) return <Carregando />;
  if (naoAutenticado) return <Navigate to="/login" replace />;
  if (!direitos.has(direito)) return <Navigate to="/sem-permissao" replace />;
  return <>{children}</>;
}
