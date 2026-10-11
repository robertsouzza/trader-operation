package com.traderoperation.chat.infrastructure.adapter.out.eventos;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.traderoperation.chat.application.port.out.MensagemEventosPort;
import com.traderoperation.chat.domain.Mensagem;
import com.traderoperation.shared.eventos.CanaisRedis;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

@Component
class MensagemEventosRedisAdapter implements MensagemEventosPort {

    private final StringRedisTemplate redis;
    private final ObjectMapper json;

    MensagemEventosRedisAdapter(StringRedisTemplate redis, ObjectMapper json) {
        this.redis = redis;
        this.json = json;
    }

    @Override
    public void aoEnviar(Mensagem mensagem) {
        try {
            redis.convertAndSend(
                    CanaisRedis.chatDaOperacao(mensagem.operacaoId().toString()),
                    json.writeValueAsString(mensagem));
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Falha ao serializar mensagem para Redis.", e);
        }
    }
}
