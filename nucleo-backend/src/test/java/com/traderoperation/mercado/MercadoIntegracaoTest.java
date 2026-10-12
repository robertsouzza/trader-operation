package com.traderoperation.mercado;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.traderoperation.mercado.application.port.out.MotorQuantPort;
import com.traderoperation.mercado.domain.Ativo;
import com.traderoperation.mercado.domain.Candle;
import com.traderoperation.mercado.domain.Timeframe;
import com.traderoperation.support.AbstractIntegracaoTest;
import jakarta.servlet.http.Cookie;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

/** Garante que /api/mercado/candles exige VER_OPERACAO_TEMPO_REAL e repassa ao MotorQuantPort. */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class MercadoIntegracaoTest extends AbstractIntegracaoTest {

    private static final String CSRF = "csrf-mercado";

    @Autowired MockMvc mvc;

    @MockitoBean
    MotorQuantPort motor;

    private static Cookie csrf() {
        return new Cookie("XSRF-TOKEN", CSRF);
    }

    private Cookie login(String email) throws Exception {
        MvcResult res = mvc.perform(post("/api/auth/login")
                        .cookie(csrf()).header("X-XSRF-TOKEN", CSRF)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"" + email + "\",\"senha\":\"trader123\"}"))
                .andExpect(status().isOk())
                .andReturn();
        return res.getResponse().getCookie("trader_access");
    }

    @Test
    void clienteProRecebeCandlesComDireito() throws Exception {
        when(motor.candles(Ativo.NAS100, Timeframe.M1, 5)).thenReturn(List.of(
                new Candle(Instant.parse("2026-10-12T12:00:00Z"),
                        new BigDecimal("100"), new BigDecimal("101"),
                        new BigDecimal("99"), new BigDecimal("100.5"), new BigDecimal("1"))));
        Cookie cliente = login("cliente@trader.local");

        mvc.perform(get("/api/mercado/candles")
                        .cookie(cliente)
                        .param("ativo", "NAS100")
                        .param("timeframe", "M1")
                        .param("limite", "5"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].fechamento").value(100.5));
    }

    @Test
    void adminSemDireitoDeOperacaoRecebe403() throws Exception {
        // ADMIN herda VER_OPERACAO_COM_ATRASO do plano FREE, mas não VER_OPERACAO_TEMPO_REAL.
        Cookie admin = login("admin@trader.local");

        mvc.perform(get("/api/mercado/candles")
                        .cookie(admin)
                        .param("ativo", "NAS100")
                        .param("timeframe", "M1"))
                .andExpect(status().isForbidden());
    }

    @Test
    void semLoginRecebe401() throws Exception {
        mvc.perform(get("/api/mercado/candles")
                        .param("ativo", "NAS100")
                        .param("timeframe", "M1"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void ativoDesconhecidoDevolveErroDeValidacao() throws Exception {
        Cookie cliente = login("cliente@trader.local");

        mvc.perform(get("/api/mercado/candles")
                        .cookie(cliente)
                        .param("ativo", "BTCUSD")
                        .param("timeframe", "M1"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void limiteForaDoIntervaloDevolveErroDeValidacao() throws Exception {
        Cookie cliente = login("cliente@trader.local");

        mvc.perform(get("/api/mercado/candles")
                        .cookie(cliente)
                        .param("ativo", "NAS100")
                        .param("timeframe", "M1")
                        .param("limite", "0"))
                .andExpect(status().isBadRequest());
    }
}
