import { AcessoNegadoError, ErroApi, NaoAutorizadoError } from './erros';
import { garantirCsrf } from './csrf';

type Metodo = 'GET' | 'POST' | 'PUT' | 'PATCH' | 'DELETE';

interface Opcoes {
  readonly metodo?: Metodo;
  readonly corpo?: unknown;
  readonly sinal?: AbortSignal;
}

const PRECISA_CSRF: ReadonlySet<Metodo> = new Set(['POST', 'PUT', 'PATCH', 'DELETE']);

function baseUrl(): string {
  return import.meta.env.VITE_API_BASE_URL ?? 'http://localhost:8090';
}

async function parsearErro(res: Response): Promise<string> {
  try {
    const corpo = (await res.json()) as { mensagem?: unknown };
    if (corpo && typeof corpo.mensagem === 'string') return corpo.mensagem;
  } catch {
    // resposta sem corpo JSON — segue com a mensagem padrão
  }
  return res.statusText || `Erro ${res.status}`;
}

export async function requisicao<T>(caminho: string, opcoes: Opcoes = {}): Promise<T> {
  const metodo: Metodo = opcoes.metodo ?? 'GET';
  const url = `${baseUrl()}${caminho}`;
  const cabecalhos: Record<string, string> = {};
  let corpo: string | undefined;

  if (opcoes.corpo !== undefined) {
    cabecalhos['Content-Type'] = 'application/json';
    corpo = JSON.stringify(opcoes.corpo);
  }

  if (PRECISA_CSRF.has(metodo)) {
    const csrf = await garantirCsrf(baseUrl());
    if (csrf) cabecalhos['X-XSRF-TOKEN'] = csrf;
  }

  const resposta = await fetch(url, {
    method: metodo,
    headers: cabecalhos,
    credentials: 'include',
    ...(corpo === undefined ? {} : { body: corpo }),
    ...(opcoes.sinal ? { signal: opcoes.sinal } : {}),
  });

  if (resposta.status === 204) {
    return undefined as T;
  }

  if (!resposta.ok) {
    const mensagem = await parsearErro(resposta);
    if (resposta.status === 401) throw new NaoAutorizadoError(mensagem);
    if (resposta.status === 403) throw new AcessoNegadoError(mensagem);
    throw new ErroApi(mensagem, resposta.status);
  }

  const texto = await resposta.text();
  return texto ? (JSON.parse(texto) as T) : (undefined as T);
}

export const apiClient = {
  get: <T>(caminho: string, sinal?: AbortSignal) =>
    requisicao<T>(caminho, sinal === undefined ? {} : { sinal }),
  post: <T>(caminho: string, corpo?: unknown, sinal?: AbortSignal) =>
    requisicao<T>(caminho, {
      metodo: 'POST',
      ...(corpo === undefined ? {} : { corpo }),
      ...(sinal === undefined ? {} : { sinal }),
    }),
  put: <T>(caminho: string, corpo?: unknown, sinal?: AbortSignal) =>
    requisicao<T>(caminho, {
      metodo: 'PUT',
      ...(corpo === undefined ? {} : { corpo }),
      ...(sinal === undefined ? {} : { sinal }),
    }),
  del: <T>(caminho: string, sinal?: AbortSignal) =>
    requisicao<T>(caminho, {
      metodo: 'DELETE',
      ...(sinal === undefined ? {} : { sinal }),
    }),
};
