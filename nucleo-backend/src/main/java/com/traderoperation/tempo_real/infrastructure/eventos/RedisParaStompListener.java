package com.traderoperation.tempo_real.infrastructure.eventos;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.traderoperation.shared.eventos.CanaisRedis;
import jakarta.annotation.PostConstruct;
import java.nio.charset.StandardCharsets;
import org.springframework.data.redis.connection.Message;
import org.springframework.data.redis.connection.MessageListener;
import org.springframework.data.redis.listener.PatternTopic;
import org.springframework.data.redis.listener.RedisMessageListenerContainer;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;

/**
 * Assina os canais Redis publicados por {@link OperacaoEventosRedisAdapter} e
 * {@link MensagemEventosRedisAdapter} e re-publica via STOMP. Isso permite múltiplas
 * instâncias do backend compartilharem o broadcast sem RabbitMQ.
 */
@Component
public class RedisParaStompListener implements MessageListener {

    private final RedisMessageListenerContainer container;
    private final SimpMessagingTemplate messaging;
    private final ObjectMapper json;

    public RedisParaStompListener(
            RedisMessageListenerContainer container,
            SimpMessagingTemplate messaging,
            ObjectMapper json) {
        this.container = container;
        this.messaging = messaging;
        this.json = json;
    }

    @PostConstruct
    void assinar() {
        container.addMessageListener(this, new PatternTopic(CanaisRedis.OPERACAO_PUBLICADA));
        container.addMessageListener(this, new PatternTopic(CanaisRedis.OPERACAO_ENCERRADA));
        container.addMessageListener(this, new PatternTopic(CanaisRedis.CHAT_PREFIXO + "*"));
    }

    @Override
    public void onMessage(Message message, byte[] pattern) {
        String canal = new String(message.getChannel(), StandardCharsets.UTF_8);
        String corpo = new String(message.getBody(), StandardCharsets.UTF_8);
        try {
            JsonNode payload = json.readTree(corpo);
            if (CanaisRedis.OPERACAO_PUBLICADA.equals(canal)) {
                messaging.convertAndSend("/topic/operacoes", payload);
            } else if (CanaisRedis.OPERACAO_ENCERRADA.equals(canal)) {
                String id = payload.get("id").asText();
                messaging.convertAndSend("/topic/operacoes/" + id, payload);
                messaging.convertAndSend("/topic/operacoes/encerradas", payload);
            } else if (canal.startsWith(CanaisRedis.CHAT_PREFIXO)) {
                String operacaoId = canal.substring(CanaisRedis.CHAT_PREFIXO.length());
                messaging.convertAndSend("/topic/chat/" + operacaoId, payload);
            }
        } catch (Exception e) {
            // engolimos para o listener não derrubar; log já vem do Spring
            throw new IllegalStateException("Falha processando mensagem Redis em " + canal, e);
        }
    }
}
