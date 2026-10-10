import { describe, expect, it } from 'vitest';
import { render, screen } from '@testing-library/react';
import { Route, Routes } from 'react-router-dom';
import { RotaPorDireito } from './RotaPorDireito';
import { Envoltorio, estadoAutenticado } from '@/test/helpers';

function montar(direitos: readonly string[], rotas: readonly string[] = ['/admin']) {
  const estado = estadoAutenticado({ direitos: new Set(direitos) });
  return render(
    <Envoltorio usuario={estado} rotas={rotas}>
      <Routes>
        <Route path="/sem-permissao" element={<p>sem permissao</p>} />
        <Route
          path="/admin"
          element={
            <RotaPorDireito direito="ADMINISTRAR_USUARIOS">
              <p>area admin</p>
            </RotaPorDireito>
          }
        />
      </Routes>
    </Envoltorio>,
  );
}

describe('RotaPorDireito', () => {
  it('renderiza filhos quando o direito está presente', () => {
    montar(['ADMINISTRAR_USUARIOS']);
    expect(screen.getByText('area admin')).toBeInTheDocument();
  });

  it('redireciona para /sem-permissao quando o direito falta', () => {
    montar(['USAR_COPILOTO_MT5']);
    expect(screen.getByText('sem permissao')).toBeInTheDocument();
  });
});
