package com.traderoperation.auditoria;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.traderoperation.auditoria.domain.RegistroAuditoria;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import org.junit.jupiter.api.Test;

class RegistroAuditoriaTest {

    @Test
    void acaoEhObrigatoria() {
        assertThatThrownBy(() -> RegistroAuditoria.novo(null, " ", null, null, Map.of(), Instant.now()))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void dadosSaoCopiadosParaORegistroSerImutavel() {
        Map<String, Object> dados = new HashMap<>(Map.of("a", 1));
        RegistroAuditoria r = RegistroAuditoria.novo(null, "TESTE", null, null, dados, Instant.now());
        dados.put("b", 2);
        assertThat(r.dados()).containsOnlyKeys("a");
    }
}
