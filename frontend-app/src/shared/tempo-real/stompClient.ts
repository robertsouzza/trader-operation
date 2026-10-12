import { Client, type IMessage, type StompSubscription } from '@stomp/stompjs';

type Ouvinte<T> = (payload: T) => void;

/**
 * Cliente STOMP singleton. Conecta no backend quando o primeiro `subscribe` chega,
 * reconecta sozinho em caso de queda e desconecta quando o último ouvinte sai.
 * O handshake WebSocket carrega o cookie `trader_access` automaticamente (D-25).
 */
class StompClienteTraderOperation {
  private cliente: Client | null = null;
  private readonly inscricoes = new Map<string, { stomp: StompSubscription; contagem: number }>();
  private readonly ouvintes = new Map<string, Set<Ouvinte<unknown>>>();
  private readonly pendentes: Array<() => void> = [];
  private conectado = false;

  private url(): string {
    const base = import.meta.env.VITE_API_BASE_URL ?? 'http://localhost:8090';
    return base.replace(/^http/, 'ws') + '/ws/stomp';
  }

  private garantirCliente(): Client {
    if (this.cliente) return this.cliente;
    const cliente = new Client({
      brokerURL: this.url(),
      reconnectDelay: 5_000,
      heartbeatIncoming: 10_000,
      heartbeatOutgoing: 10_000,
      debug: () => {}, // silencioso em produção
    });
    cliente.onConnect = () => {
      this.conectado = true;
      // reinscreve nos destinos ativos
      for (const [destino] of this.ouvintes) {
        this.assinarNoBroker(destino);
      }
      while (this.pendentes.length > 0) {
        const tarefa = this.pendentes.shift();
        if (tarefa) tarefa();
      }
    };
    cliente.onDisconnect = () => {
      this.conectado = false;
      this.inscricoes.clear();
    };
    cliente.onWebSocketClose = () => {
      this.conectado = false;
      this.inscricoes.clear();
    };
    cliente.activate();
    this.cliente = cliente;
    return cliente;
  }

  private assinarNoBroker(destino: string): void {
    if (!this.cliente || !this.conectado) return;
    const atual = this.inscricoes.get(destino);
    if (atual) {
      atual.contagem += 1;
      return;
    }
    const stomp = this.cliente.subscribe(destino, (mensagem: IMessage) => {
      let payload: unknown;
      try {
        payload = mensagem.body ? JSON.parse(mensagem.body) : null;
      } catch {
        payload = mensagem.body;
      }
      const ouvintes = this.ouvintes.get(destino);
      if (!ouvintes) return;
      for (const ouvinte of ouvintes) {
        try {
          ouvinte(payload);
        } catch (erro) {
          console.error('Erro em ouvinte STOMP para', destino, erro);
        }
      }
    });
    this.inscricoes.set(destino, { stomp, contagem: 1 });
  }

  subscribe<T>(destino: string, ouvinte: Ouvinte<T>): () => void {
    const conjuntoOuvintes = this.ouvintes.get(destino) ?? new Set<Ouvinte<unknown>>();
    conjuntoOuvintes.add(ouvinte as Ouvinte<unknown>);
    this.ouvintes.set(destino, conjuntoOuvintes);

    const cliente = this.garantirCliente();
    if (cliente.connected) {
      this.assinarNoBroker(destino);
    } else {
      this.pendentes.push(() => this.assinarNoBroker(destino));
    }

    return () => this.unsubscribe(destino, ouvinte as Ouvinte<unknown>);
  }

  private unsubscribe(destino: string, ouvinte: Ouvinte<unknown>): void {
    const conjunto = this.ouvintes.get(destino);
    if (!conjunto) return;
    conjunto.delete(ouvinte);
    if (conjunto.size === 0) {
      this.ouvintes.delete(destino);
      const atual = this.inscricoes.get(destino);
      if (atual) {
        atual.contagem -= 1;
        if (atual.contagem <= 0) {
          try {
            atual.stomp.unsubscribe();
          } catch {
            // ignora
          }
          this.inscricoes.delete(destino);
        }
      }
    }
    if (this.ouvintes.size === 0 && this.cliente) {
      // Nenhum ouvinte restante — fecha o socket para liberar recursos.
      this.cliente.deactivate();
      this.cliente = null;
      this.conectado = false;
      this.inscricoes.clear();
    }
  }
}

export const stompClient = new StompClienteTraderOperation();
