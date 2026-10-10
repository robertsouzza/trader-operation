const NOME_COOKIE = 'XSRF-TOKEN';

export function lerTokenCsrf(): string | null {
  if (typeof document === 'undefined') return null;
  const partes = document.cookie ? document.cookie.split(';') : [];
  for (const parte of partes) {
    const [bruto, ...resto] = parte.split('=');
    if (!bruto) continue;
    if (bruto.trim() === NOME_COOKIE) {
      return decodeURIComponent(resto.join('=').trim());
    }
  }
  return null;
}

/**
 * Garante que o cookie XSRF-TOKEN existe antes de uma mutação. Se não existir,
 * dispara um GET no endpoint de CSRF do backend (que grava o cookie).
 */
export async function garantirCsrf(baseUrl: string): Promise<string | null> {
  const atual = lerTokenCsrf();
  if (atual) return atual;
  await fetch(`${baseUrl}/api/auth/csrf`, {
    method: 'GET',
    credentials: 'include',
  });
  return lerTokenCsrf();
}
