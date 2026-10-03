package com.traderoperation.autenticacao.infrastructure.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

/** Lê o cookie trader_access e, se o token for válido, autentica a requisição com o perfil como ROLE_. */
class JwtCookieAuthFilter extends OncePerRequestFilter {

    private final JwtService jwt;

    JwtCookieAuthFilter(JwtService jwt) {
        this.jwt = jwt;
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
                new UsernamePasswordAuthenticationToken(usuario, null,
                        List.of(new SimpleGrantedAuthority("ROLE_" + usuario.perfil())))));

        chain.doFilter(request, response);
    }
}
