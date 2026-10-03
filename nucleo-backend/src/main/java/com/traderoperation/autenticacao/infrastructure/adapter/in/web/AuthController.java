package com.traderoperation.autenticacao.infrastructure.adapter.in.web;

import com.traderoperation.autenticacao.application.port.in.DadosUsuario;
import com.traderoperation.autenticacao.application.port.in.LoginUseCase;
import com.traderoperation.autenticacao.application.port.in.RenovarSessaoUseCase;
import com.traderoperation.autenticacao.application.port.in.SessaoEmitida;
import com.traderoperation.autenticacao.application.port.in.UsuarioAtualQuery;
import com.traderoperation.autenticacao.infrastructure.security.CookiesSessao;
import com.traderoperation.shared.security.UsuarioAutenticado;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Autenticação")
@RestController
@RequestMapping("/api/auth")
class AuthController {

    private final LoginUseCase login;
    private final RenovarSessaoUseCase renovacao;
    private final UsuarioAtualQuery usuarioAtual;
    private final CookiesSessao cookies;

    AuthController(LoginUseCase login, RenovarSessaoUseCase renovacao, UsuarioAtualQuery usuarioAtual,
                   CookiesSessao cookies) {
        this.login = login;
        this.renovacao = renovacao;
        this.usuarioAtual = usuarioAtual;
        this.cookies = cookies;
    }

    record LoginRequest(@NotBlank @Email String email, @NotBlank String senha) {
    }

    @Operation(summary = "Entra na plataforma e grava os cookies de sessão")
    @PostMapping("/login")
    ResponseEntity<DadosUsuario> login(@Valid @RequestBody LoginRequest req) {
        SessaoEmitida sessao = login.login(req.email(), req.senha());
        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, cookies.access(sessao.accessToken()).toString())
                .header(HttpHeaders.SET_COOKIE, cookies.refresh(sessao.refreshToken()).toString())
                .body(sessao.usuario());
    }

    @Operation(summary = "Renova o cookie de acesso usando o cookie de refresh")
    @PostMapping("/refresh")
    ResponseEntity<DadosUsuario> refresh(@CookieValue(name = CookiesSessao.REFRESH, required = false) String token) {
        SessaoEmitida sessao = renovacao.renovar(token);
        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, cookies.access(sessao.accessToken()).toString())
                .body(sessao.usuario());
    }

    @Operation(summary = "Sai da plataforma apagando os cookies de sessão")
    @PostMapping("/logout")
    ResponseEntity<Void> logout() {
        return ResponseEntity.noContent()
                .header(HttpHeaders.SET_COOKIE, cookies.expirarAccess().toString())
                .header(HttpHeaders.SET_COOKIE, cookies.expirarRefresh().toString())
                .build();
    }

    @Operation(summary = "Dados do usuário logado")
    @GetMapping("/me")
    DadosUsuario me(@AuthenticationPrincipal UsuarioAutenticado usuario) {
        return usuarioAtual.buscar(usuario.id());
    }

    @Operation(summary = "Emite o cookie XSRF-TOKEN, necessário antes de qualquer POST")
    @GetMapping("/csrf")
    ResponseEntity<Void> csrf(CsrfToken token) {
        // Ler o token força a gravação do cookie (o Spring Security carrega o token de forma preguiçosa).
        token.getToken();
        return ResponseEntity.noContent().build();
    }
}
