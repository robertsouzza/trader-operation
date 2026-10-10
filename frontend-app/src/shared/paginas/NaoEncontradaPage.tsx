import { Link } from 'react-router-dom';

export function NaoEncontradaPage() {
  return (
    <section className="flex flex-col items-center justify-center gap-4 py-16 text-center">
      <h1 className="text-3xl font-semibold text-texto">Página não encontrada</h1>
      <p className="text-sm text-texto-sutil">O endereço informado não existe.</p>
      <Link to="/" className="text-sm text-acento hover:underline">
        Voltar para o início
      </Link>
    </section>
  );
}
