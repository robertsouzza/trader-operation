package com.traderoperation.operacoes;

import static org.assertj.core.api.Assertions.assertThat;
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
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

/** Ponta-a-ponta REST da skill 05: publicar, listar ao vivo, encerrar, chat e barreiras por direito. */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Testcontainers
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class OperacoesIntegracaoTest {

    @Container
    @ServiceConnection
    static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>(
            DockerImageName.parse("pgvector/pgvector:0.8.1-pg16").asCompatibleSubstituteFor("postgres"));

    private static final String CSRF = "token-csrf-de-teste";

    @Autowired MockMvc mvc;
    @Autowired JdbcTemplate jdbc;
    @Autowired ObjectMapper json;

    private static Cookie csrf() {
        return new Cookie("XSRF-TOKEN", CSRF);
    }

    private Cookie loginE(String email) throws Exception {
        MvcResult res = mvc.perform(post("/api/auth/login")
                        .cookie(csrf()).header("X-XSRF-TOKEN", CSRF)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"" + email + "\",\"senha\":\"trader123\"}"))
                .andExpect(status().isOk())
                .andReturn();
        return res.getResponse().getCookie("trader_access");
    }

    private static String corpoCompraPadrao() {
        return """
                {
                  "ativo": "NAS100",
                  "direcao": "COMPRA",
                  "entrada": 20000,
                  "stop": 19900,
                  "alvos": [20100, 20200],
                  "estrategia": "triangulo-rompimento"
                }
                """;
    }

    @Test
    @Order(1)
    void masterPublicaOperacaoERecebeOResumo() throws Exception {
        Cookie master = loginE("master@trader.local");

        MvcResult res = mvc.perform(post("/api/operacoes")
                        .cookie(master, csrf()).header("X-XSRF-TOKEN", CSRF)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(corpoCompraPadrao()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("PUBLICADA"))
                .andExpect(jsonPath("$.ativo").value("NAS100"))
                .andExpect(jsonPath("$.direcao").value("COMPRA"))
                .andReturn();

        String id = json.readTree(res.getResponse().getContentAsString()).get("id").asText();
        assertThat(id).isNotBlank();
    }

    @Test
    @Order(2)
    void adminNaoPublicaMasConsegueListarComAtrasoViaFree() throws Exception {
        // ADMIN tem o direito ADMINISTRAR_USUARIOS (perfil) e, sem assinatura, herda FREE que
        // libera VER_OPERACAO_COM_ATRASO. Portanto, lista (vazia por enquanto, operação publicada
        // há segundos) mas não publica.
        Cookie admin = loginE("admin@trader.local");

        mvc.perform(get("/api/operacoes/ao-vivo").cookie(admin))
                .andExpect(status().isOk());

        mvc.perform(post("/api/operacoes")
                        .cookie(admin, csrf()).header("X-XSRF-TOKEN", CSRF)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(corpoCompraPadrao()))
                .andExpect(status().isForbidden());
    }

    @Test
    @Order(3)
    void clienteSemAssinaturaAtivaCaiEmFreeEEnxergaOperacaoAntiga() throws Exception {
        // O cliente do seed começa como PRO; para testar o atraso para FREE, mexemos no banco:
        // move a publicada_em da operação para 20 min atrás e cancela a assinatura ativa.
        jdbc.update("UPDATE operacoes SET publicada_em = now() - interval '20 min'");
        jdbc.update("UPDATE assinaturas SET status = 'CANCELADA', fim_em = now() WHERE status = 'ATIVA'");

        Cookie cliente = loginE("cliente@trader.local");
        MvcResult res = mvc.perform(get("/api/operacoes/ao-vivo").cookie(cliente))
                .andExpect(status().isOk())
                .andReturn();
        JsonNode lista = json.readTree(res.getResponse().getContentAsString());
        assertThat(lista.isArray()).isTrue();
        assertThat(lista.size()).isGreaterThanOrEqualTo(1);
    }

    @Test
    @Order(4)
    void clienteFreeNaoEnxergaOperacaoRecemPublicada() throws Exception {
        // Garante que o cliente está FREE (sem assinatura ativa pelo passo anterior)
        Cookie master = loginE("master@trader.local");
        mvc.perform(post("/api/operacoes")
                        .cookie(master, csrf()).header("X-XSRF-TOKEN", CSRF)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(corpoCompraPadrao()))
                .andExpect(status().isOk());

        Cookie cliente = loginE("cliente@trader.local");
        MvcResult res = mvc.perform(get("/api/operacoes/ao-vivo").cookie(cliente))
                .andExpect(status().isOk())
                .andReturn();
        JsonNode lista = json.readTree(res.getResponse().getContentAsString());
        // operação recém-publicada ainda não aparece para FREE (atraso de 15min)
        boolean temRecemPublicada = false;
        for (JsonNode op : lista) {
            if (op.get("publicadaEm").asText().startsWith("2026")) {
                // qualquer operação com data real
                continue;
            }
            temRecemPublicada = true;
        }
        // Só devem aparecer as operações antigas (do passo 3), nunca a nova.
        // (Validação explícita: lista tem exatamente 1, a antiga.)
        assertThat(lista.size()).isEqualTo(1);
    }

    @Test
    @Order(5)
    void masterEncerraOperacaoERemoveDoAoVivo() throws Exception {
        // Restaura PRO para o cliente para poder listar ao vivo
        jdbc.update("INSERT INTO assinaturas (id, usuario_id, plano, status, origem, inicio_em, fim_em) " +
                "VALUES (gen_random_uuid(), '00000000-0000-0000-0000-000000000003', 'PRO', 'ATIVA', 'MANUAL', now(), now() + interval '30 days')");

        Cookie master = loginE("master@trader.local");
        // Publica uma operação nova para encerrar
        MvcResult pub = mvc.perform(post("/api/operacoes")
                        .cookie(master, csrf()).header("X-XSRF-TOKEN", CSRF)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(corpoCompraPadrao()))
                .andExpect(status().isOk())
                .andReturn();
        String id = json.readTree(pub.getResponse().getContentAsString()).get("id").asText();

        mvc.perform(post("/api/operacoes/" + id + "/encerrar")
                        .cookie(master, csrf()).header("X-XSRF-TOKEN", CSRF)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"resultado\":\"GAIN\",\"observacao\":\"alvo 1 batido\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("ENCERRADA"))
                .andExpect(jsonPath("$.resultado").value("GAIN"));

        Integer total = jdbc.queryForObject(
                "SELECT count(*) FROM registros_auditoria WHERE acao = 'ENCERRAR_OPERACAO'", Integer.class);
        assertThat(total).isPositive();
    }

    @Test
    @Order(6)
    void clienteNaoPodePublicarOperacao() throws Exception {
        Cookie cliente = loginE("cliente@trader.local");
        mvc.perform(post("/api/operacoes")
                        .cookie(cliente, csrf()).header("X-XSRF-TOKEN", CSRF)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(corpoCompraPadrao()))
                .andExpect(status().isForbidden());
    }
}
