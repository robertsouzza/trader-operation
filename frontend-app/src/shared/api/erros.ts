/** Erros normalizados do cliente HTTP. Toda chamada ao backend produz um destes. */
export class ErroApi extends Error {
  readonly status: number;

  constructor(mensagem: string, status: number) {
    super(mensagem);
    this.status = status;
    this.name = 'ErroApi';
  }
}

export class NaoAutorizadoError extends ErroApi {
  constructor(mensagem = 'Faça login para continuar.') {
    super(mensagem, 401);
    this.name = 'NaoAutorizadoError';
  }
}

export class AcessoNegadoError extends ErroApi {
  constructor(mensagem = 'Você não tem permissão para esta ação.') {
    super(mensagem, 403);
    this.name = 'AcessoNegadoError';
  }
}
