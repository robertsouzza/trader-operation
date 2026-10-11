package com.traderoperation.tempo_real.application.service;

import com.traderoperation.tempo_real.application.port.in.ContadorParticipantesQuery;
import java.util.UUID;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

/**
 * Contador de participantes por operação (RF-06) usando Redis SET.
 * Chave: {@code trader:operacao:{id}:participantes}. Membro: id da sessão STOMP.
 */
@Service
public class ContadorParticipantesService implements ContadorParticipantesQuery {

    private static final String PREFIXO = "trader:operacao:";
    private static final String SUFIXO = ":participantes";

    private final StringRedisTemplate redis;

    public ContadorParticipantesService(StringRedisTemplate redis) {
        this.redis = redis;
    }

    @Override
    public long contar(UUID operacaoId) {
        Long tamanho = redis.opsForSet().size(chave(operacaoId));
        return tamanho == null ? 0L : tamanho;
    }

    @Override
    public long entrar(UUID operacaoId, String sessaoId) {
        redis.opsForSet().add(chave(operacaoId), sessaoId);
        return contar(operacaoId);
    }

    @Override
    public long sair(UUID operacaoId, String sessaoId) {
        redis.opsForSet().remove(chave(operacaoId), sessaoId);
        long atual = contar(operacaoId);
        if (atual == 0L) {
            redis.delete(chave(operacaoId));
        }
        return atual;
    }

    private static String chave(UUID operacaoId) {
        return PREFIXO + operacaoId + SUFIXO;
    }
}
