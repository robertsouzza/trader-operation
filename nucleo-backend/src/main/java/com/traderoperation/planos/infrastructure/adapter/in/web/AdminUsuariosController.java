package com.traderoperation.planos.infrastructure.adapter.in.web;

import com.traderoperation.planos.application.port.in.AtribuirPlanoUseCase;
import com.traderoperation.planos.application.port.in.ListarUsuariosQuery;
import com.traderoperation.planos.application.port.in.PaginaUsuarios;
import com.traderoperation.planos.domain.OrigemAssinatura;
import com.traderoperation.planos.domain.Plano;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import java.time.Duration;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Administração de usuários")
@RestController
@RequestMapping("/api/admin/usuarios")
@PreAuthorize("hasAuthority('ADMINISTRAR_USUARIOS')")
class AdminUsuariosController {

    private static final int DURACAO_PADRAO_DIAS = 30;

    private final ListarUsuariosQuery listar;
    private final AtribuirPlanoUseCase atribuir;

    AdminUsuariosController(ListarUsuariosQuery listar, AtribuirPlanoUseCase atribuir) {
        this.listar = listar;
        this.atribuir = atribuir;
    }

    record AtribuirPlanoRequest(@NotNull Plano plano, Integer duracaoDias) {
    }

    @Operation(summary = "Lista usuários paginada com plano atual")
    @GetMapping
    PaginaUsuarios listar(@RequestParam(name = "q", required = false) String busca,
                          @RequestParam(name = "pagina", defaultValue = "0") int pagina,
                          @RequestParam(name = "tamanho", defaultValue = "20") int tamanho) {
        return listar.listar(busca, pagina, tamanho);
    }

    @Operation(summary = "Atribui plano a um usuário (origem MANUAL). Cancela a assinatura ativa anterior.")
    @PutMapping("/{id}/plano")
    ResponseEntity<Void> atribuirPlano(@PathVariable UUID id, @Valid @RequestBody AtribuirPlanoRequest req) {
        Duration duracao = duracaoDe(req);
        atribuir.atribuir(id, req.plano(), OrigemAssinatura.MANUAL, duracao);
        return ResponseEntity.noContent().build();
    }

    private static Duration duracaoDe(AtribuirPlanoRequest req) {
        if (req.duracaoDias() != null) {
            return req.duracaoDias() <= 0 ? null : Duration.ofDays(req.duracaoDias());
        }
        return req.plano() == Plano.FREE ? null : Duration.ofDays(DURACAO_PADRAO_DIAS);
    }
}
