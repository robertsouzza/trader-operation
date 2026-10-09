package com.traderoperation.autenticacao.infrastructure.security;

import com.traderoperation.planos.application.port.in.VerificarDireitoUseCase;
import com.traderoperation.shared.security.UsuarioAutenticado;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * Lê o cookie trader_access e, se o token for válido, autentica a requisição.
 * Popula o {@code Authentication} com {@code ROLE_<PERFIL>} (para {@code hasRole}) e
 * uma authority por nome de direito (para {@code @PreAuthorize("hasAuthority('...')")}).
 * Os nomes vêm da port in de planos — o enum fica confinado àquele módulo.
 */
class JwtCookieAuthFilter extends OncePerRequestFilter {

    private final JwtService jwt;
    private final VerificarDireitoUseCase direitos;

    JwtCookieAuthFilter(JwtService jwt, VerificarDireitoUseCase direitos) {
        this.jwt = jwt;
        this.direitos = direitos;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        String token = Arrays.stream(request.getCookies() == null ? new Cookie[0] : request.getCookies())
                .filter(c -> CookiesSessao.ACCESS.equals(c.getName()))
                .map(Cookie::getValue)
                .findFirst()
                .orElse(null);

        jwt.validarAccess(token).ifPresent(usuario -> SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(usuario, null, autoridadesDe(usuario))));

        chain.doFilter(request, response);
    }

    private List<GrantedAuthority> autoridadesDe(UsuarioAutenticado usuario) {
        List<GrantedAuthority> autoridades = new ArrayList<>();
        autoridades.add(new SimpleGrantedAuthority("ROLE_" + usuario.perfil()));
        direitos.direitosDoUsuario(usuario.id())
                .forEach(nome -> autoridades.add(new SimpleGrantedAuthority(nome)));
        return autoridades;
    }
}
