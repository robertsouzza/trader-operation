package com.traderoperation.planos.application.service;

import com.traderoperation.auditoria.application.port.in.Auditavel;
import com.traderoperation.planos.application.port.in.AtribuirPlanoUseCase;
import com.traderoperation.planos.application.port.in.CatalogoPlano;
import com.traderoperation.planos.application.port.in.ConsultarPlanoDoUsuarioQuery;
import com.traderoperation.planos.application.port.in.ListarUsuariosQuery;
import com.traderoperation.planos.application.port.in.PaginaUsuarios;
import com.traderoperation.planos.application.port.in.PlanoAtual;
import com.traderoperation.planos.application.port.in.ResumoUsuario;
import com.traderoperation.planos.application.port.in.VerificarDireitoUseCase;
import com.traderoperation.planos.application.port.out.AssinaturaRepositoryPort;
import com.traderoperation.planos.application.port.out.UsuarioLookupPort;
import com.traderoperation.planos.domain.Assinatura;
import com.traderoperation.planos.domain.Direito;
import com.traderoperation.planos.domain.OrigemAssinatura;
import com.traderoperation.planos.domain.Plano;
import com.traderoperation.planos.domain.StatusAssinatura;
import com.traderoperation.shared.error.RecursoNaoEncontradoException;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class PlanosService implements
        ConsultarPlanoDoUsuarioQuery,
        AtribuirPlanoUseCase,
        VerificarDireitoUseCase,
        ListarUsuariosQuery {

    private final AssinaturaRepositoryPort assinaturas;
    private final UsuarioLookupPort usuarios;
    private final MatrizDeDireitos matriz;
    private final Clock clock;

    public PlanosService(AssinaturaRepositoryPort assinaturas, UsuarioLookupPort usuarios,
                         MatrizDeDireitos matriz, Clock clock) {
        this.assinaturas = assinaturas;
        this.usuarios = usuarios;
        this.matriz = matriz;
        this.clock = clock;
    }

    public List<CatalogoPlano> catalogo() {
        return Arrays.stream(Plano.values())
                .map(p -> new CatalogoPlano(p, matriz.doPlano(p)))
                .toList();
    }

    @Override
    public PlanoAtual consultar(UUID usuarioId) {
        UsuarioLookupPort.UsuarioResumido usuario = usuarios.buscarPorId(usuarioId)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Usuário não encontrado."));
        Optional<Assinatura> ativa = assinaturas.buscarAtivaDoUsuario(usuarioId);
        Plano plano = ativa.map(Assinatura::plano).orElse(Plano.FREE);
        Set<Direito> direitos = matriz.paraUsuario(usuario.perfil(), plano);
        return new PlanoAtual(
                plano,
                ativa.map(Assinatura::inicioEm).orElse(null),
                ativa.map(Assinatura::fimEm).orElse(null),
                ativa.map(Assinatura::origem).orElse(null),
                direitos);
    }

    @Override
    @Transactional
    @Auditavel(acao = "ATRIBUIR_PLANO", entidade = "assinatura")
    public void atribuir(UUID usuarioId, Plano plano, OrigemAssinatura origem, Duration duracao) {
        usuarios.buscarPorId(usuarioId)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Usuário não encontrado."));
        Instant agora = clock.instant();
        assinaturas.buscarAtivaDoUsuario(usuarioId)
                .map(a -> a.cancelar(agora))
                .ifPresent(assinaturas::atualizar);
        Instant fim = duracao == null ? null : agora.plus(duracao);
        assinaturas.salvar(new Assinatura(
                UUID.randomUUID(), usuarioId, plano, StatusAssinatura.ATIVA, origem, agora, fim));
    }

    @Override
    public Set<String> direitosDoUsuario(UUID usuarioId) {
        return usuarios.buscarPorId(usuarioId)
                .map(u -> matriz.paraUsuario(u.perfil(),
                        assinaturas.buscarAtivaDoUsuario(usuarioId).map(Assinatura::plano).orElse(Plano.FREE)))
                .orElse(Set.<Direito>of())
                .stream()
                .map(Direito::name)
                .collect(java.util.stream.Collectors.toUnmodifiableSet());
    }

    @Override
    public PaginaUsuarios listar(String busca, int pagina, int tamanho) {
        UsuarioLookupPort.PaginaUsuarioResumido pagina0 = usuarios.listar(busca, pagina, tamanho);
        Set<UUID> ids = new HashSet<>();
        pagina0.itens().forEach(u -> ids.add(u.id()));
        Map<UUID, Assinatura> ativas = assinaturas.buscarAtivasDeVarios(ids);
        List<ResumoUsuario> itens = pagina0.itens().stream()
                .map(u -> {
                    Assinatura a = ativas.get(u.id());
                    Plano plano = a == null ? Plano.FREE : a.plano();
                    Instant fim = a == null ? null : a.fimEm();
                    return new ResumoUsuario(u.id(), u.nome(), u.email(), u.perfil(), plano, fim);
                })
                .toList();
        return new PaginaUsuarios(pagina0.total(), pagina, tamanho, itens);
    }
}
