package com.traderoperation.tempo_real.infrastructure.websocket;

import java.util.Set;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.MessagingException;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.stereotype.Component;

/**
 * Valida direitos a cada SUBSCRIBE. Destinos:
 * - /topic/operacoes e /topic/operacoes/** → exige VER_OPERACAO_TEMPO_REAL
 * - /topic/chat/{id} → exige LER_CHAT_OPERACAO
 */
@Component
public class AutenticacaoChannelInterceptor implements ChannelInterceptor {

    @Override
    public Message<?> preSend(Message<?> message, MessageChannel channel) {
        StompHeaderAccessor acessor = MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);
        if (acessor == null) {
            return message;
        }
        if (StompCommand.CONNECT.equals(acessor.getCommand())) {
            // Já validado no handshake; nada a fazer.
            return message;
        }
        if (StompCommand.SUBSCRIBE.equals(acessor.getCommand())) {
            Set<String> direitos = direitosDoPrincipal(acessor);
            String destino = acessor.getDestination();
            if (destino == null) {
                throw new MessagingException("SUBSCRIBE sem destino.");
            }
            if (destino.startsWith("/topic/operacoes") && !direitos.contains("VER_OPERACAO_TEMPO_REAL")) {
                throw new MessagingException("Sem direito VER_OPERACAO_TEMPO_REAL para " + destino);
            }
            if (destino.startsWith("/topic/chat/") && !direitos.contains("LER_CHAT_OPERACAO")) {
                throw new MessagingException("Sem direito LER_CHAT_OPERACAO para " + destino);
            }
        }
        return message;
    }

    private static Set<String> direitosDoPrincipal(StompHeaderAccessor acessor) {
        if (acessor.getUser() instanceof UsuarioWsPrincipal principal) {
            return principal.direitos();
        }
        return Set.of();
    }
}
