package com.traderoperation.tempo_real;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.traderoperation.support.AbstractIntegracaoTest;
import jakarta.servlet.http.Cookie;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.MediaType;
import org.springframework.messaging.converter.MappingJackson2MessageConverter;
import org.springframework.messaging.simp.stomp.StompFrameHandler;
import org.springframework.messaging.simp.stomp.StompHeaders;
import org.springframework.messaging.simp.stomp.StompSession;
import org.springframework.messaging.simp.stomp.StompSessionHandlerAdapter;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.socket.client.standard.StandardWebSocketClient;
import org.springframework.web.socket.messaging.WebSocketStompClient;

/** Fluxo WebSocket + Redis: master publica, cliente PRO conectado recebe via /topic/operacoes. */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
@ActiveProfiles("test")
class TempoRealIntegracaoTest extends AbstractIntegracaoTest {

    private static final String CSRF = "token-csrf-ws";

    @LocalServerPort int porta;
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

    @Test
    void clienteProConectaReceveNovaOperacaoViaStomp() throws Exception {
        Cookie accessCliente = login("cliente@trader.local");
        Cookie accessMaster = login("master@trader.local");

        WebSocketStompClient stomp = new WebSocketStompClient(new StandardWebSocketClient());
        stomp.setMessageConverter(new MappingJackson2MessageConverter());

        org.springframework.web.socket.WebSocketHttpHeaders handshake =
                new org.springframework.web.socket.WebSocketHttpHeaders();
        handshake.add("Cookie", "trader_access=" + accessCliente.getValue());

        StompSession sessao = stomp
                .connectAsync("ws://localhost:" + porta + "/ws/stomp", handshake,
                        new StompSessionHandlerAdapter() {})
                .get(5, TimeUnit.SECONDS);

        BlockingQueue<JsonNode> recebidas = new LinkedBlockingQueue<>();
        sessao.subscribe("/topic/operacoes", new StompFrameHandler() {
            @Override
            public java.lang.reflect.Type getPayloadType(StompHeaders headers) {
                return JsonNode.class;
            }

            @Override
            public void handleFrame(StompHeaders headers, Object payload) {
                recebidas.add((JsonNode) payload);
            }
        });

        // Dá um instante para o SUBSCRIBE propagar antes de publicar.
        Thread.sleep(200);

        mvc.perform(post("/api/operacoes")
                        .cookie(accessMaster, csrf()).header("X-XSRF-TOKEN", CSRF)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"ativo":"NAS100","direcao":"COMPRA","entrada":20000,"stop":19900,"alvos":[20100]}
                                """))
                .andExpect(status().isOk());

        JsonNode payload = recebidas.poll(5, TimeUnit.SECONDS);
        assertThat(payload).as("STOMP não entregou a operação publicada").isNotNull();
        assertThat(payload.get("ativo").asText()).isEqualTo("NAS100");

        sessao.disconnect();
        stomp.stop();
    }

    @Test
    void handshakeSemCookieDevolveErro() {
        WebSocketStompClient stomp = new WebSocketStompClient(new StandardWebSocketClient());
        stomp.setMessageConverter(new MappingJackson2MessageConverter());

        try {
            stomp.connectAsync("ws://localhost:" + porta + "/ws/stomp",
                            new StompSessionHandlerAdapter() {})
                    .get(2, TimeUnit.SECONDS);
            // Se chegou aqui, deveria ter falhado.
            throw new AssertionError("Handshake sem cookie deveria ter sido rejeitado.");
        } catch (Exception esperado) {
            // conexão rejeitada é o comportamento esperado
        } finally {
            stomp.stop();
        }
    }
}
