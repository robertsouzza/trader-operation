import { Route, Routes } from 'react-router-dom';
import { Layout } from '@/shared/layout/Layout';
import { RotaProtegida } from '@/shared/seguranca/RotaProtegida';
import { RotaPorDireito } from '@/shared/seguranca/RotaPorDireito';
import { LoginPage } from '@/features/autenticacao/pages/LoginPage';
import { HomePage } from '@/features/home/pages/HomePage';
import { CatalogoPlanosPage } from '@/features/planos/pages/CatalogoPlanosPage';
import { MeuPlanoPage } from '@/features/planos/pages/MeuPlanoPage';
import { ListaUsuariosPage } from '@/features/admin/pages/ListaUsuariosPage';
import { ListaOperacoesPage } from '@/features/operacoes/pages/ListaOperacoesPage';
import { OperacaoPage } from '@/features/operacoes/pages/OperacaoPage';
import { SemPermissaoPage } from '@/shared/paginas/SemPermissaoPage';
import { NaoEncontradaPage } from '@/shared/paginas/NaoEncontradaPage';

export default function App() {
  return (
    <Routes>
      <Route path="/login" element={<LoginPage />} />
      <Route element={<Layout />}>
        <Route path="/sem-permissao" element={<SemPermissaoPage />} />
        <Route path="/planos" element={<CatalogoPlanosPage />} />
        <Route
          path="/"
          element={
            <RotaProtegida>
              <HomePage />
            </RotaProtegida>
          }
        />
        <Route
          path="/meu-plano"
          element={
            <RotaProtegida>
              <MeuPlanoPage />
            </RotaProtegida>
          }
        />
        <Route
          path="/admin/usuarios"
          element={
            <RotaPorDireito direito="ADMINISTRAR_USUARIOS">
              <ListaUsuariosPage />
            </RotaPorDireito>
          }
        />
        <Route
          path="/operacoes"
          element={
            <RotaPorDireito direito="VER_OPERACAO_TEMPO_REAL">
              <ListaOperacoesPage />
            </RotaPorDireito>
          }
        />
        <Route
          path="/operacoes/:id"
          element={
            <RotaPorDireito direito="VER_OPERACAO_TEMPO_REAL">
              <OperacaoPage />
            </RotaPorDireito>
          }
        />
        <Route path="*" element={<NaoEncontradaPage />} />
      </Route>
    </Routes>
  );
}
