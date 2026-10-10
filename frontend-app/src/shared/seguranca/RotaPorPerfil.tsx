import type { ReactNode } from 'react';
import { Navigate } from 'react-router-dom';
import { useUsuarioAtual } from './contextoUsuarioAtual';
import { Carregando } from '@/shared/componentes/Carregando';
import type { Perfil } from '@/features/autenticacao/api/autenticacaoApi';

interface Props {
  readonly perfil: Perfil;
  readonly children: ReactNode;
}

export function RotaPorPerfil({ perfil, children }: Props) {
  const { carregando, naoAutenticado, usuario } = useUsuarioAtual();

  if (carregando) return <Carregando />;
  if (naoAutenticado || !usuario) return <Navigate to="/login" replace />;
  if (usuario.perfil !== perfil) return <Navigate to="/sem-permissao" replace />;
  return <>{children}</>;
}
