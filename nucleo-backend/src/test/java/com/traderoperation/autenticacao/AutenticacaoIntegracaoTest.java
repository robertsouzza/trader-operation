package com.traderoperation.autenticacao;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.cookie;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

/** Fluxo completo de login por cookie contra um Postgres real (mesma imagem do compose). */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Testcontainers
class AutenticacaoIntegracaoTest {

    @Container
    @ServiceConnection
    static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>(
            DockerImageName.parse("pgvector/pgvector:0.8.1-pg16").asCompatibleSubstituteFor("postgres"));

    private static final String CSRF = "token-csrf-de-teste";
    private static final String LOGIN_MASTER = """
            {"email":"master@trader.local","senha":"trader123"}""";

    @Autowired
    private MockMvc mvc;

    @Autowired
    private JdbcTemplate jdbc;

    private static Cookie csrfCookie() {
        return new Cookie("XSRF-TOKEN", CSRF);
    }

    private MvcResult loginMaster() throws Exception {
        return mvc.perform(post("/api/auth/login").cookie(csrfCookie()).header("X-XSRF-TOKEN", CSRF)
                        .contentType(MediaType.APPLICATION_JSON).content(LOGIN_MASTER))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.perfil").value("MASTER"))
                .andExpect(jsonPath("$.accessToken").doesNotExist())
                .andExpect(cookie().httpOnly("trader_access", true))
                .andExpect(cookie().httpOnly("trader_refresh", true))
                .andReturn();
    }

    @Test
    void meSemCookieDevolve401() throws Exception {
        mvc.perform(get("/api/auth/me")).andExpect(status().isUnauthorized());
    }

    @Test
    void loginSetaCookiesEMeDevolveOUsuario() throws Exception {
        Cookie access = loginMaster().getResponse().getCookie("trader_access");
        mvc.perform(get("/api/auth/me").cookie(access))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value("master@trader.local"));
    }

    @Test
    void loginSemCsrfDevolve403() throws Exception {
        mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON).content(LOGIN_MASTER))
                .andExpect(status().isForbidden());
    }

    @Test
    void senhaErradaDevolve401ComMensagemGenerica() throws Exception {
        mvc.perform(post("/api/auth/login").cookie(csrfCookie()).header("X-XSRF-TOKEN", CSRF)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"master@trader.local\",\"senha\":\"errada\"}"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.mensagem").value("E-mail ou senha inválidos."));
    }

    @Test
    void refreshEmiteNovoAccess() throws Exception {
        Cookie refresh = loginMaster().getResponse().getCookie("trader_refresh");
        mvc.perform(post("/api/auth/refresh").cookie(refresh, csrfCookie()).header("X-XSRF-TOKEN", CSRF))
                .andExpect(status().isOk())
                .andExpect(cookie().exists("trader_access"));
    }

    @Test
    void loginFicaNoDiarioDeAuditoria() throws Exception {
        loginMaster();
        Integer total = jdbc.queryForObject(
                "SELECT count(*) FROM registros_auditoria WHERE acao = 'LOGIN'", Integer.class);
        assertThat(total).isPositive();
    }

    @Test
    void csrfEmiteCookieLegivelPeloFrontend() throws Exception {
        mvc.perform(get("/api/auth/csrf"))
                .andExpect(status().isNoContent())
                .andExpect(cookie().exists("XSRF-TOKEN"))
                .andExpect(cookie().httpOnly("XSRF-TOKEN", false));
    }
}
