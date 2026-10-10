package com.traderoperation.chat;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
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
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

/** Fluxo REST do chat da operação. */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Testcontainers
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class ChatIntegracaoTest {

    @Container
    @ServiceConnection
    static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>(
            DockerImageName.parse("pgvector/pgvector:0.8.1-pg16").asCompatibleSubstituteFor("postgres"));

    private static final String CSRF = "token-csrf";

    @Autowired MockMvc mvc;
    @Autowired ObjectMapper json;

    private static Cookie csrf() {
        return new Cookie("XSRF-TOKEN", CSRF);
    }

    private Cookie login(String email) throws Exception {
        return mvc.perform(post("/api/auth/login")
                        .cookie(csrf()).header("X-XSRF-TOKEN", CSRF)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"" + email + "\",\"senha\":\"trader123\"}"))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse().getCookie("trader_access");
    }

    private String criarOperacao() throws Exception {
        Cookie master = login("master@trader.local");
        MvcResult res = mvc.perform(post("/api/operacoes")
                        .cookie(master, csrf()).header("X-XSRF-TOKEN", CSRF)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"ativo":"NAS100","direcao":"COMPRA","entrada":20000,"stop":19900,"alvos":[20100]}
                                """))
                .andExpect(status().isOk())
                .andReturn();
        return json.readTree(res.getResponse().getContentAsString()).get("id").asText();
    }

    @Test
    @Order(1)
    void clienteProEnviaELeMensagens() throws Exception {
        String operacaoId = criarOperacao();
        Cookie cliente = login("cliente@trader.local");

        mvc.perform(post("/api/operacoes/" + operacaoId + "/mensagens")
                        .cookie(cliente, csrf()).header("X-XSRF-TOKEN", CSRF)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"texto\":\"entrada boa, segui o sinal\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.texto").value("entrada boa, segui o sinal"))
                .andExpect(jsonPath("$.autorNome").value("Cliente Dev"));

        mvc.perform(get("/api/operacoes/" + operacaoId + "/mensagens").cookie(cliente))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1));
    }

    @Test
    @Order(2)
    void naoEnviaMensagemEmOperacaoEncerrada() throws Exception {
        String operacaoId = criarOperacao();
        Cookie master = login("master@trader.local");
        mvc.perform(post("/api/operacoes/" + operacaoId + "/encerrar")
                        .cookie(master, csrf()).header("X-XSRF-TOKEN", CSRF)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"resultado\":\"GAIN\"}"))
                .andExpect(status().isOk());

        Cookie cliente = login("cliente@trader.local");
        mvc.perform(post("/api/operacoes/" + operacaoId + "/mensagens")
                        .cookie(cliente, csrf()).header("X-XSRF-TOKEN", CSRF)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"texto\":\"ainda tô dentro?\"}"))
                .andExpect(status().isNotFound());
    }
}
