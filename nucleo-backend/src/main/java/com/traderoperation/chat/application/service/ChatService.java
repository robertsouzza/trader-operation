package com.traderoperation.chat.application.service;

import com.traderoperation.auditoria.application.port.in.Auditavel;
import com.traderoperation.autenticacao.application.port.in.DadosUsuario;
import com.traderoperation.autenticacao.application.port.in.UsuarioAtualQuery;
import com.traderoperation.chat.application.port.in.EnviarMensagemCmd;
import com.traderoperation.chat.application.port.in.EnviarMensagemUseCase;
import com.traderoperation.chat.application.port.in.ListarMensagensQuery;
import com.traderoperation.chat.application.port.in.MensagemResumo;
import com.traderoperation.chat.application.port.out.MensagemEventosPort;
import com.traderoperation.chat.application.port.out.MensagemRepositoryPort;
import com.traderoperation.chat.domain.Mensagem;
import com.traderoperation.operacoes.application.port.in.ConsultarOperacoesQuery;
import com.traderoperation.shared.error.RecursoNaoEncontradoException;
import java.time.Clock;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class ChatService implements EnviarMensagemUseCase, ListarMensagensQuery {

    private final MensagemRepositoryPort repositorio;
    private final MensagemEventosPort eventos;
    private final ConsultarOperacoesQuery operacoes;
    private final UsuarioAtualQuery usuarios;
    private final Clock clock;

    public ChatService(
            MensagemRepositoryPort repositorio,
            MensagemEventosPort eventos,
            ConsultarOperacoesQuery operacoes,
            UsuarioAtualQuery usuarios,
            Clock clock) {
        this.repositorio = repositorio;
        this.eventos = eventos;
        this.operacoes = operacoes;
        this.usuarios = usuarios;
        this.clock = clock;
    }

    @Override
    @Transactional
    @Auditavel(acao = "ENVIAR_MENSAGEM", entidade = "mensagem")
    public MensagemResumo enviar(EnviarMensagemCmd cmd) {
        if (!operacoes.estaPublicada(cmd.operacaoId())) {
            throw new RecursoNaoEncontradoException(
                    "Operação não encontrada ou já encerrada; não aceita mais mensagens.");
        }
        Mensagem mensagem = new Mensagem(
                UUID.randomUUID(),
                cmd.operacaoId(),
                cmd.autorId(),
                cmd.texto(),
                clock.instant());
        repositorio.salvar(mensagem);
        eventos.aoEnviar(mensagem);
        return comNome(mensagem);
    }

    @Override
    public List<MensagemResumo> listar(UUID operacaoId, int limite) {
        List<Mensagem> mensagens = repositorio.listarMaisRecentes(operacaoId, Math.max(1, Math.min(limite, 500)));
        List<MensagemResumo> resumos = new ArrayList<>(mensagens.size());
        for (Mensagem m : mensagens) {
            resumos.add(comNome(m));
        }
        return resumos;
    }

    private MensagemResumo comNome(Mensagem m) {
        String nome;
        try {
            DadosUsuario autor = usuarios.buscar(m.autorId());
            nome = autor.nome();
        } catch (RecursoNaoEncontradoException e) {
            nome = "Participante";
        }
        return new MensagemResumo(m.id(), m.operacaoId(), m.autorId(), nome, m.texto(), m.enviadaEm());
    }
}
