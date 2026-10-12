package com.traderoperation.operacoes.infrastructure.adapter.in.web;

import com.traderoperation.operacoes.application.port.in.ConsultarOperacoesQuery;
import com.traderoperation.operacoes.application.port.in.EncerrarOperacaoCmd;
import com.traderoperation.operacoes.application.port.in.EncerrarOperacaoUseCase;
import com.traderoperation.operacoes.application.port.in.OperacaoResumo;
import com.traderoperation.operacoes.application.port.in.PublicarOperacaoCmd;
import com.traderoperation.operacoes.application.port.in.PublicarOperacaoUseCase;
import com.traderoperation.operacoes.domain.Direcao;
import com.traderoperation.operacoes.domain.ResultadoOperacao;
import com.traderoperation.shared.error.RecursoNaoEncontradoException;
import com.traderoperation.shared.security.UsuarioAutenticado;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.time.Duration;
import java.util.List;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.authority.AuthorityUtils;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Operações")
@RestController
@RequestMapping("/api/operacoes")
class OperacoesController {

    static final Duration ATRASO_PARA_FREE = Duration.ofMinutes(15);
    static final Duration SEM_ATRASO = Duration.ZERO;

    private final PublicarOperacaoUseCase publicar;
    private final EncerrarOperacaoUseCase encerrar;
    private final ConsultarOperacoesQuery consultar;

    OperacoesController(
            PublicarOperacaoUseCase publicar,
            EncerrarOperacaoUseCase encerrar,
            ConsultarOperacoesQuery consultar) {
        this.publicar = publicar;
        this.encerrar = encerrar;
        this.consultar = consultar;
    }

    public record PublicarRequest(
            @NotBlank @Size(max = 20) String ativo,
            @NotNull Direcao direcao,
            @NotNull @Positive BigDecimal entrada,
            @NotNull @Positive BigDecimal stop,
            @NotEmpty List<@NotNull @Positive BigDecimal> alvos,
            @Size(max = 120) String estrategia) {
    }

    public record EncerrarRequest(
            @NotNull ResultadoOperacao resultado,
            @Size(max = 500) String observacao) {
    }

    @Operation(summary = "Publica uma nova operação (apenas master)")
    @PostMapping
    @PreAuthorize("hasAuthority('PUBLICAR_OPERACAO')")
    OperacaoResumo publicar(
            @AuthenticationPrincipal UsuarioAutenticado usuario,
            @Valid @RequestBody PublicarRequest req) {
        return publicar.publicar(new PublicarOperacaoCmd(
                usuario.id(), req.ativo(), req.direcao(),
                req.entrada(), req.stop(), req.alvos(), req.estrategia()));
    }

    @Operation(summary = "Encerra a operação (apenas o master que a publicou)")
    @PostMapping("/{id}/encerrar")
    @PreAuthorize("hasAuthority('PUBLICAR_OPERACAO')")
    OperacaoResumo encerrar(
            @AuthenticationPrincipal UsuarioAutenticado usuario,
            @PathVariable UUID id,
            @Valid @RequestBody EncerrarRequest req) {
        return encerrar.encerrar(new EncerrarOperacaoCmd(
                id, usuario.id(), req.resultado(), req.observacao()));
    }

    @Operation(summary = "Lista operações ao vivo conforme direitos do usuário")
    @GetMapping("/ao-vivo")
    List<OperacaoResumo> listar() {
        return consultar.listarAoVivo(atrasoAplicavel());
    }

    @Operation(summary = "Obtém uma operação pelo id (respeitando o atraso aplicável)")
    @GetMapping("/{id}")
    ResponseEntity<OperacaoResumo> obter(@PathVariable UUID id) {
        return consultar.obter(id, atrasoAplicavel())
                .map(ResponseEntity::ok)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Operação não encontrada."));
    }

    private static Duration atrasoAplicavel() {
        List<String> authorities = AuthorityUtils.authorityListToSet(
                        SecurityContextHolder.getContext().getAuthentication().getAuthorities())
                .stream().toList();
        if (authorities.contains("VER_OPERACAO_TEMPO_REAL")) {
            return SEM_ATRASO;
        }
        if (authorities.contains("VER_OPERACAO_COM_ATRASO")) {
            return ATRASO_PARA_FREE;
        }
        throw new AccessDeniedException("Você não tem permissão para ver operações.");
    }
}
