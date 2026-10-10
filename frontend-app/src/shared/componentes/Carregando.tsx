export function Carregando({ mensagem = 'Carregando...' }: { readonly mensagem?: string }) {
  return (
    <div className="flex items-center justify-center gap-3 p-10 text-texto-sutil" role="status">
      <span className="h-5 w-5 animate-spin rounded-full border-2 border-acento border-t-transparent" />
      <span>{mensagem}</span>
    </div>
  );
}
