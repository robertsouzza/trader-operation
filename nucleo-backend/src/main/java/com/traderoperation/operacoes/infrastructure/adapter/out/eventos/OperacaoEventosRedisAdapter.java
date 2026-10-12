package com.traderoperation.operacoes.infrastructure.adapter.out.eventos;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.traderoperation.operacoes.application.port.out.OperacaoEventosPort;
import com.traderoperation.operacoes.domain.Operacao;
import com.traderoperation.shared.eventos.CanaisRedis;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

/** Publica operações no Redis para a ponte Redis↔STOMP do módulo tempo_real redirecionar. */
@Component
class OperacaoEventosRedisAdapter implements OperacaoEventosPort {

    private final StringRedisTemplate redis;
    private final ObjectMapper json;

    OperacaoEventosRedisAdapter(StringRedisTemplate redis, ObjectMapper json) {
        this.redis = redis;
        this.json = json;
    }

    @Override
    public void aoPublicar(Operacao operacao) {
        publicar(CanaisRedis.OPERACAO_PUBLICADA, operacao);
    }

    @Override
    public void aoEncerrar(Operacao operacao) {
        publicar(CanaisRedis.OPERACAO_ENCERRADA, operacao);
    }

    private void publicar(String canal, Operacao operacao) {
        try {
            redis.convertAndSend(canal, json.writeValueAsString(operacao));
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Falha ao serializar operação para Redis.", e);
        }
    }
}
