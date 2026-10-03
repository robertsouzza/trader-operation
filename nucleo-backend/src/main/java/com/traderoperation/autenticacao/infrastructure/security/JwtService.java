package com.traderoperation.autenticacao.infrastructure.security;

import com.traderoperation.autenticacao.application.port.out.TokenPort;
import com.traderoperation.autenticacao.domain.Usuario;
import com.traderoperation.shared.config.TraderProperties;
import com.traderoperation.shared.security.UsuarioAutenticado;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Date;
import java.util.Optional;
import java.util.UUID;
import javax.crypto.SecretKey;
import org.springframework.stereotype.Component;

/** Emite e valida JWT HS256. O claim "tipo" impede usar um refresh token como access e vice-versa. */
@Component
public class JwtService implements TokenPort {

    private static final String ISSUER = "trader-operation";
    private static final String TIPO_ACCESS = "access";
    private static final String TIPO_REFRESH = "refresh";

    private final SecretKey chave;
    private final Duration duracaoAccess;
    private final Duration duracaoRefresh;
    private final Clock clock;

    public JwtService(TraderProperties props, Clock clock) {
        this.chave = Keys.hmacShaKeyFor(props.jwt().secret().getBytes(StandardCharsets.UTF_8));
        this.duracaoAccess = Duration.ofMinutes(props.jwt().accessMinutos());
        this.duracaoRefresh = Duration.ofDays(props.jwt().refreshDias());
        this.clock = clock;
    }

    public Duration duracaoAccess() {
        return duracaoAccess;
    }

    public Duration duracaoRefresh() {
        return duracaoRefresh;
    }

    @Override
    public String gerarAccess(Usuario usuario) {
        return gerar(usuario, TIPO_ACCESS, duracaoAccess);
    }

    @Override
    public String gerarRefresh(Usuario usuario) {
        return gerar(usuario, TIPO_REFRESH, duracaoRefresh);
    }

    @Override
    public Optional<UUID> validarRefresh(String token) {
        return claims(token, TIPO_REFRESH).map(c -> UUID.fromString(c.getSubject()));
    }

    /** Usuário do access token, se válido. */
    public Optional<UsuarioAutenticado> validarAccess(String token) {
        return claims(token, TIPO_ACCESS).map(c -> new UsuarioAutenticado(
                UUID.fromString(c.getSubject()), c.get("email", String.class), c.get("perfil", String.class)));
    }

    private String gerar(Usuario usuario, String tipo, Duration duracao) {
        Instant agora = clock.instant();
        return Jwts.builder()
                .issuer(ISSUER)
                .subject(usuario.id().toString())
                .claim("tipo", tipo)
                .claim("email", usuario.email())
                .claim("perfil", usuario.perfil().name())
                .issuedAt(Date.from(agora))
                .expiration(Date.from(agora.plus(duracao)))
                .signWith(chave, Jwts.SIG.HS256)
                .compact();
    }

    private Optional<Claims> claims(String token, String tipoEsperado) {
        if (token == null || token.isBlank()) {
            return Optional.empty();
        }
        try {
            Claims c = Jwts.parser()
                    .verifyWith(chave)
                    .requireIssuer(ISSUER)
                    .clock(() -> Date.from(clock.instant()))
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();
            return tipoEsperado.equals(c.get("tipo", String.class)) ? Optional.of(c) : Optional.empty();
        } catch (JwtException | IllegalArgumentException e) {
            return Optional.empty();
        }
    }
}
