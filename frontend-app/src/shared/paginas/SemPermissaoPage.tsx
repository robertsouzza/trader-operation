import { Link } from 'react-router-dom';

export function SemPermissaoPage() {
  return (
    <section className="flex flex-col items-center justify-center gap-4 py-16 text-center">
      <h1 className="text-3xl font-semibold text-texto">Sem permissão</h1>
      <p className="max-w-md text-sm text-texto-sutil">
        Seu perfil ou plano atual não libera esta área. Fale com um administrador se precisar de
        acesso.
      </p>
      <Link to="/" className="text-sm text-acento hover:underline">
        Voltar para o início
      </Link>
    </section>
  );
}
