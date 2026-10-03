package com.traderoperation.autenticacao;

import static org.assertj.core.api.Assertions.assertThat;

import com.traderoperation.autenticacao.domain.Perfil;
import com.traderoperation.autenticacao.domain.Usuario;
import com.traderoperation.autenticacao.infrastructure.security.JwtService;
import com.traderoperation.shared.config.TraderProperties;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class JwtServiceTest {

    private static final Instant AGORA = Instant.parse("2026-10-03T12:00:00Z");
    private static final Usuario MASTER = new Usuario(UUID.randomUUID(), "Master", "master@trader.local",
            "hash", Perfil.MASTER, true);

    private static TraderProperties props(String segredo) {
        return new TraderProperties(new TraderProperties.Jwt(segredo, 15, 7), new TraderProperties.Cookie(false),
                new TraderProperties.Cors(List.of("http://localhost:5173")),
                new TraderProperties.S3("http://s3", "a", "b", "c", "us-east-1"));
    }

    private static JwtService servico(Instant agora) {
        return new JwtService(props("x".repeat(64)), Clock.fixed(agora, ZoneOffset.UTC));
    }

    @Test
    void accessTokenValidoDevolveUsuarioEPerfil() {
        JwtService jwt = servico(AGORA);
        var usuario = jwt.validarAccess(jwt.gerarAccess(MASTER)).orElseThrow();
        assertThat(usuario.id()).isEqualTo(MASTER.id());
        assertThat(usuario.perfil()).isEqualTo("MASTER");
    }

    @Test
    void accessTokenExpiraDepoisDe15Minutos() {
        String token = servico(AGORA).gerarAccess(MASTER);
        assertThat(servico(AGORA.plus(Duration.ofMinutes(16))).validarAccess(token)).isEmpty();
    }

    @Test
    void refreshNaoServeComoAccessENemOContrario() {
        JwtService jwt = servico(AGORA);
        assertThat(jwt.validarAccess(jwt.gerarRefresh(MASTER))).isEmpty();
        assertThat(jwt.validarRefresh(jwt.gerarAccess(MASTER))).isEmpty();
        assertThat(jwt.validarRefresh(jwt.gerarRefresh(MASTER))).contains(MASTER.id());
    }

    @Test
    void tokenAssinadoComOutroSegredoEhRejeitado() {
        String alheio = new JwtService(props("y".repeat(64)), Clock.fixed(AGORA, ZoneOffset.UTC)).gerarAccess(MASTER);
        assertThat(servico(AGORA).validarAccess(alheio)).isEmpty();
        assertThat(servico(AGORA).validarAccess("lixo")).isEmpty();
        assertThat(servico(AGORA).validarAccess(null)).isEmpty();
    }
}
