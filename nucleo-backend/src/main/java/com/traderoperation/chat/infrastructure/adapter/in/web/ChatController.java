package com.traderoperation.chat.infrastructure.adapter.in.web;

import com.traderoperation.chat.application.port.in.EnviarMensagemCmd;
import com.traderoperation.chat.application.port.in.EnviarMensagemUseCase;
import com.traderoperation.chat.application.port.in.ListarMensagensQuery;
import com.traderoperation.chat.application.port.in.MensagemResumo;
import com.traderoperation.shared.security.UsuarioAutenticado;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.util.List;
import java.util.UUID;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Chat da operação")
@RestController
@RequestMapping("/api/operacoes/{operacaoId}/mensagens")
class ChatController {

    private final EnviarMensagemUseCase enviar;
    private final ListarMensagensQuery listar;

    ChatController(EnviarMensagemUseCase enviar, ListarMensagensQuery listar) {
        this.enviar = enviar;
        this.listar = listar;
    }

    public record EnviarRequest(@NotBlank @Size(max = 500) String texto) {
    }

    @Operation(summary = "Lista as mensagens mais recentes da operação")
    @GetMapping
    @PreAuthorize("hasAuthority('LER_CHAT_OPERACAO')")
    List<MensagemResumo> listar(
            @PathVariable UUID operacaoId,
            @RequestParam(name = "limite", defaultValue = "100") int limite) {
        return listar.listar(operacaoId, limite);
    }

    @Operation(summary = "Envia uma mensagem no chat da operação")
    @PostMapping
    @PreAuthorize("hasAuthority('PARTICIPAR_CHAT_OPERACAO')")
    MensagemResumo enviar(
            @AuthenticationPrincipal UsuarioAutenticado usuario,
            @PathVariable UUID operacaoId,
            @Valid @RequestBody EnviarRequest req) {
        return enviar.enviar(new EnviarMensagemCmd(operacaoId, usuario.id(), req.texto()));
    }
}
