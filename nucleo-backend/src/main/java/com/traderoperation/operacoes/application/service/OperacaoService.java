package com.traderoperation.operacoes.application.service;

import com.traderoperation.auditoria.application.port.in.Auditavel;
import com.traderoperation.autenticacao.application.port.in.DadosUsuario;
import com.traderoperation.autenticacao.application.port.in.UsuarioAtualQuery;
import com.traderoperation.operacoes.application.port.in.ConsultarOperacoesQuery;
import com.traderoperation.operacoes.application.port.in.EncerrarOperacaoCmd;
import com.traderoperation.operacoes.application.port.in.EncerrarOperacaoUseCase;
import com.traderoperation.operacoes.application.port.in.OperacaoResumo;
import com.traderoperation.operacoes.application.port.in.PublicarOperacaoCmd;
import com.traderoperation.operacoes.application.port.in.PublicarOperacaoUseCase;
import com.traderoperation.operacoes.application.port.out.OperacaoEventosPort;
import com.traderoperation.operacoes.application.port.out.OperacaoRepositoryPort;
import com.traderoperation.operacoes.domain.Operacao;
import com.traderoperation.shared.error.NaoAutorizadoException;
import com.traderoperation.shared.error.RecursoNaoEncontradoException;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class OperacaoService
        implements PublicarOperacaoUseCase, EncerrarOperacaoUseCase, ConsultarOperacoesQuery {

    private final OperacaoRepositoryPort repositorio;
    private final OperacaoEventosPort eventos;
    private final UsuarioAtualQuery usuarios;
    private final Clock clock;

    public OperacaoService(
            OperacaoRepositoryPort repositorio,
            OperacaoEventosPort eventos,
            UsuarioAtualQuery usuarios,
            Clock clock) {
        this.repositorio = repositorio;
        this.eventos = eventos;
        this.usuarios = usuarios;
        this.clock = clock;
    }

    @Override
    @Transactional
    @Auditavel(acao = "PUBLICAR_OPERACAO", entidade = "operacao")
    public OperacaoResumo publicar(PublicarOperacaoCmd cmd) {
        Operacao operacao = Operacao.publicar(
                UUID.randomUUID(),
                cmd.ativo(),
                cmd.direcao(),
                cmd.entrada(),
                cmd.stop(),
                cmd.alvos(),
                cmd.estrategia(),
                cmd.masterId(),
                clock.instant());
        repositorio.salvar(operacao);
        eventos.aoPublicar(operacao);
        return paraResumo(operacao);
    }

    @Override
    @Transactional
    @Auditavel(acao = "ENCERRAR_OPERACAO", entidade = "operacao")
    public OperacaoResumo encerrar(EncerrarOperacaoCmd cmd) {
        Operacao operacao = repositorio.buscarPorId(cmd.operacaoId())
                .orElseThrow(() -> new RecursoNaoEncontradoException("Operação não encontrada."));
        if (!operacao.masterId().equals(cmd.masterId())) {
            throw new NaoAutorizadoException("Apenas o master que publicou pode encerrar a operação.");
        }
        Operacao encerrada = operacao.encerrar(cmd.resultado(), cmd.observacao(), clock.instant());
        repositorio.atualizar(encerrada);
        eventos.aoEncerrar(encerrada);
        return paraResumo(encerrada);
    }

    @Override
    public List<OperacaoResumo> listarAoVivo(Duration atrasoMinimo) {
        Instant limite = clock.instant().minus(atrasoMinimo);
        return repositorio.buscarPublicadasAte(limite).stream()
                .map(this::paraResumo)
                .toList();
    }

    @Override
    public Optional<OperacaoResumo> obter(UUID id, Duration atrasoMinimo) {
        Instant limite = clock.instant().minus(atrasoMinimo);
        return repositorio.buscarPorId(id)
                .filter(op -> !op.publicadaEm().isAfter(limite))
                .map(this::paraResumo);
    }

    @Override
    public boolean estaPublicada(UUID id) {
        return repositorio.buscarPorId(id)
                .map(op -> op.status() == com.traderoperation.operacoes.domain.StatusOperacao.PUBLICADA)
                .orElse(false);
    }

    private OperacaoResumo paraResumo(Operacao op) {
        String nomeMaster = nomeDoMaster(op.masterId());
        return new OperacaoResumo(
                op.id(), op.ativo(), op.direcao(), op.entrada(), op.stop(),
                op.alvos(), op.estrategia(), op.status(), op.resultado(), op.observacao(),
                op.masterId(), nomeMaster, op.publicadaEm(), op.encerradaEm());
    }

    private String nomeDoMaster(UUID masterId) {
        try {
            DadosUsuario dados = usuarios.buscar(masterId);
            return dados.nome();
        } catch (RecursoNaoEncontradoException e) {
            return "Master";
        }
    }
}
