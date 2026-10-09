package com.traderoperation.planos;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
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

/** Fluxo ponta-a-ponta: catálogo público, plano do cliente, troca pelo admin e barreira do cliente. */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Testcontainers
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class PlanosIntegracaoTest {

    @Container
    @ServiceConnection
    static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>(
            DockerImageName.parse("pgvector/pgvector:0.8.1-pg16").asCompatibleSubstituteFor("postgres"));

    private static final String CSRF = "token-csrf-de-teste";
    private static final String ID_CLIENTE = "00000000-0000-0000-0000-000000000003";

    @Autowired
    private MockMvc mvc;

    @Autowired
    private JdbcTemplate jdbc;

    @Autowired
    private ObjectMapper json;

    private static Cookie csrfCookie() {
        return new Cookie("XSRF-TOKEN", CSRF);
    }

    private Cookie loginE(String email) throws Exception {
        MvcResult res = mvc.perform(post("/api/auth/login").cookie(csrfCookie()).header("X-XSRF-TOKEN", CSRF)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"" + email + "\",\"senha\":\"trader123\"}"))
                .andExpect(status().isOk())
                .andReturn();
        return res.getResponse().getCookie("trader_access");
    }

    @Test
    @Order(1)
    void catalogoPublicoListaOsTresPlanos() throws Exception {
        mvc.perform(get("/api/planos"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(3))
                .andExpect(jsonPath("$[?(@.plano == 'FREE')]").exists())
                .andExpect(jsonPath("$[?(@.plano == 'PRO')]").exists())
                .andExpect(jsonPath("$[?(@.plano == 'PREMIUM')]").exists());
    }

    @Test
    @Order(2)
    void clienteVeOPlanoProDoSeedComDireitoDeCopiloto() throws Exception {
        Cookie access = loginE("cliente@trader.local");
        mvc.perform(get("/api/me/plano").cookie(access))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.plano").value("PRO"))
                .andExpect(jsonPath("$.direitos[?(@ == 'USAR_COPILOTO_MT5')]").exists());
    }

    @Test
    @Order(3)
    void clienteNaoAcessaEndpointAdmin() throws Exception {
        Cookie access = loginE("cliente@trader.local");
        mvc.perform(get("/api/admin/usuarios").cookie(access))
                .andExpect(status().isForbidden());
    }

    @Test
    @Order(4)
    void adminListaUsuarios() throws Exception {
        Cookie access = loginE("admin@trader.local");
        mvc.perform(get("/api/admin/usuarios").cookie(access).param("tamanho", "20"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.total").value(org.hamcrest.Matchers.greaterThanOrEqualTo(3)));
    }

    @Test
    @Order(5)
    void adminTrocaClienteParaFreeEClientePerdeCopiloto() throws Exception {
        Cookie admin = loginE("admin@trader.local");
        mvc.perform(put("/api/admin/usuarios/" + ID_CLIENTE + "/plano")
                        .cookie(admin, csrfCookie()).header("X-XSRF-TOKEN", CSRF)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"plano\":\"FREE\"}"))
                .andExpect(status().isNoContent());

        Cookie cliente = loginE("cliente@trader.local");
        MvcResult res = mvc.perform(get("/api/me/plano").cookie(cliente))
                .andExpect(status().isOk())
                .andReturn();
        JsonNode body = json.readTree(res.getResponse().getContentAsString());
        assertThat(body.get("plano").asText()).isEqualTo("FREE");
        boolean temCopiloto = false;
        for (JsonNode d : body.get("direitos")) {
            if ("USAR_COPILOTO_MT5".equals(d.asText())) {
                temCopiloto = true;
                break;
            }
        }
        assertThat(temCopiloto).isFalse();

        Integer total = jdbc.queryForObject(
                "SELECT count(*) FROM registros_auditoria WHERE acao = 'ATRIBUIR_PLANO'", Integer.class);
        assertThat(total).isPositive();
    }
}
