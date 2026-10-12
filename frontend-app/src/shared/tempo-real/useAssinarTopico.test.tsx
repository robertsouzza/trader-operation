import { afterEach, describe, expect, it, vi } from 'vitest';
import { render } from '@testing-library/react';
import { useAssinarTopico } from './useAssinarTopico';
import { stompClient } from './stompClient';

function Componente({
  destino,
  callback,
}: {
  readonly destino: string | null;
  readonly callback: (p: unknown) => void;
}) {
  useAssinarTopico(destino, callback);
  return <div />;
}

describe('useAssinarTopico', () => {
  afterEach(() => {
    vi.restoreAllMocks();
  });

  it('chama stompClient.subscribe quando montado', () => {
    const cancelar = vi.fn();
    const subscribeSpy = vi.spyOn(stompClient, 'subscribe').mockImplementation(() => cancelar);

    const { unmount } = render(<Componente destino="/topic/operacoes" callback={() => {}} />);

    expect(subscribeSpy).toHaveBeenCalledWith('/topic/operacoes', expect.any(Function));

    unmount();
    expect(cancelar).toHaveBeenCalled();
  });

  it('não assina quando destino é null', () => {
    const subscribeSpy = vi.spyOn(stompClient, 'subscribe').mockImplementation(() => () => {});

    render(<Componente destino={null} callback={() => {}} />);

    expect(subscribeSpy).not.toHaveBeenCalled();
  });

  it('reinscreve quando destino muda', () => {
    const cancelarA = vi.fn();
    const cancelarB = vi.fn();
    let numero = 0;
    const subscribeSpy = vi.spyOn(stompClient, 'subscribe').mockImplementation(() => {
      numero += 1;
      return numero === 1 ? cancelarA : cancelarB;
    });

    const { rerender } = render(<Componente destino="/topic/a" callback={() => {}} />);
    expect(subscribeSpy).toHaveBeenCalledTimes(1);

    rerender(<Componente destino="/topic/b" callback={() => {}} />);
    expect(cancelarA).toHaveBeenCalled();
    expect(subscribeSpy).toHaveBeenCalledTimes(2);
  });
});
