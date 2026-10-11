package com.traderoperation.tempo_real.infrastructure.websocket;

import com.traderoperation.tempo_real.application.port.in.ContadorParticipantesQuery;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Pattern;
import org.springframework.context.event.EventListener;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.messaging.SessionDisconnectEvent;
import org.springframework.web.socket.messaging.SessionSubscribeEvent;
import org.springframework.web.socket.messaging.SessionUnsubscribeEvent;

/**
 * Mantém o contador de participantes por operação em Redis (SET por operação).
 * - Em SUBSCRIBE de /topic/operacoes/{id} → SADD + broadcast do novo total.
 * - Em UNSUBSCRIBE do mesmo destino, ou em DISCONNECT da sessão → SREM + broadcast.
 */
@Component
public class PresencaWebSocketListener {

    private static final Pattern DESTINO_OPERACAO =
            Pattern.compile("^/topic/operacoes/([0-9a-fA-F-]{36})$");

    private final ContadorParticipantesQuery contador;
    private final SimpMessagingTemplate messaging;
    /** Mapa de (sessaoId → subscriptionId → operacaoId) para saber de onde sair no unsubscribe/disconnect. */
    private final Map<String, Map<String, UUID>> sessoes = new ConcurrentHashMap<>();

    public PresencaWebSocketListener(
            ContadorParticipantesQuery contador,
            SimpMessagingTemplate messaging) {
        this.contador = contador;
        this.messaging = messaging;
    }

    @EventListener
    public void aoSubscribe(SessionSubscribeEvent evento) {
        StompHeaderAccessor acessor = StompHeaderAccessor.wrap(evento.getMessage());
        UUID operacaoId = extrairOperacao(acessor.getDestination());
        if (operacaoId == null) return;

        String sessaoId = Objects.requireNonNull(acessor.getSessionId(), "sessionId");
        String subscriptionId = acessor.getSubscriptionId();
        sessoes.computeIfAbsent(sessaoId, s -> new ConcurrentHashMap<>())
                .put(subscriptionId == null ? sessaoId : subscriptionId, operacaoId);

        long atual = contador.entrar(operacaoId, sessaoId);
        broadcast(operacaoId, atual);
    }

    @EventListener
    public void aoUnsubscribe(SessionUnsubscribeEvent evento) {
        StompHeaderAccessor acessor = StompHeaderAccessor.wrap(evento.getMessage());
        String sessaoId = acessor.getSessionId();
        String subscriptionId = acessor.getSubscriptionId();
        if (sessaoId == null || subscriptionId == null) return;

        Map<String, UUID> daSessao = sessoes.get(sessaoId);
        if (daSessao == null) return;
        UUID operacaoId = daSessao.remove(subscriptionId);
        if (operacaoId == null) return;
        if (daSessao.isEmpty()) sessoes.remove(sessaoId);

        long atual = contador.sair(operacaoId, sessaoId);
        broadcast(operacaoId, atual);
    }

    @EventListener
    public void aoDisconnect(SessionDisconnectEvent evento) {
        String sessaoId = evento.getSessionId();
        Map<String, UUID> daSessao = sessoes.remove(sessaoId);
        if (daSessao == null) return;
        for (UUID operacaoId : daSessao.values()) {
            long atual = contador.sair(operacaoId, sessaoId);
            broadcast(operacaoId, atual);
        }
    }

    private void broadcast(UUID operacaoId, long atual) {
        messaging.convertAndSend(
                "/topic/operacoes/" + operacaoId + "/participantes",
                Map.of("operacaoId", operacaoId, "participantes", atual));
    }

    private static UUID extrairOperacao(String destino) {
        if (destino == null) return null;
        var matcher = DESTINO_OPERACAO.matcher(destino);
        return matcher.matches() ? UUID.fromString(matcher.group(1)) : null;
    }
}
