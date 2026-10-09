package com.traderoperation.planos.infrastructure.adapter.in.web;

import com.traderoperation.planos.application.port.in.CatalogoPlano;
import com.traderoperation.planos.application.port.in.ConsultarPlanoDoUsuarioQuery;
import com.traderoperation.planos.application.port.in.PlanoAtual;
import com.traderoperation.planos.application.service.PlanosService;
import com.traderoperation.shared.security.UsuarioAutenticado;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Planos")
@RestController
@RequestMapping("/api")
class PlanoController {

    private final PlanosService planos;
    private final ConsultarPlanoDoUsuarioQuery consultarPlano;

    PlanoController(PlanosService planos, ConsultarPlanoDoUsuarioQuery consultarPlano) {
        this.planos = planos;
        this.consultarPlano = consultarPlano;
    }

    @Operation(summary = "Catálogo público de planos, com os direitos de cada um")
    @GetMapping("/planos")
    List<CatalogoPlano> catalogo() {
        return planos.catalogo();
    }

    @Operation(summary = "Plano atual do usuário logado, com a lista efetiva de direitos")
    @GetMapping("/me/plano")
    PlanoAtual meuPlano(@AuthenticationPrincipal UsuarioAutenticado usuario) {
        return consultarPlano.consultar(usuario.id());
    }
}
