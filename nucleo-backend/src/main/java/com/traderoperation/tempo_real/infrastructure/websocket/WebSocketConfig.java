package com.traderoperation.tempo_real.infrastructure.websocket;

import com.traderoperation.shared.config.TraderProperties;
import com.traderoperation.shared.security.UsuarioAutenticado;
import java.security.Principal;
import java.util.Map;
import java.util.Set;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.messaging.simp.config.ChannelRegistration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;
import org.springframework.web.socket.server.support.DefaultHandshakeHandler;

/** Habilita WebSocket + STOMP (D-25). Broker in-memory; Redis pub/sub é a ponte entre instâncias. */
@Configuration
@EnableWebSocketMessageBroker
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

    private final AutenticacaoHandshakeInterceptor handshake;
    private final AutenticacaoChannelInterceptor canal;
    private final TraderProperties props;

    public WebSocketConfig(
            AutenticacaoHandshakeInterceptor handshake,
            AutenticacaoChannelInterceptor canal,
            TraderProperties props) {
        this.handshake = handshake;
        this.canal = canal;
        this.props = props;
    }

    @Override
    public void configureMessageBroker(MessageBrokerRegistry registry) {
        registry.enableSimpleBroker("/topic", "/queue");
        registry.setApplicationDestinationPrefixes("/app");
        registry.setUserDestinationPrefix("/user");
    }

    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        registry.addEndpoint("/ws/stomp")
                .setAllowedOrigins(props.cors().origens().toArray(new String[0]))
                .addInterceptors(handshake)
                .setHandshakeHandler(new DefaultHandshakeHandler() {
                    @Override
                    protected Principal determineUser(
                            ServerHttpRequest request,
                            WebSocketHandler wsHandler,
                            Map<String, Object> attributes) {
                        Object usuario = attributes.get(AutenticacaoHandshakeInterceptor.ATTR_USUARIO);
                        Object direitos = attributes.get(AutenticacaoHandshakeInterceptor.ATTR_DIREITOS);
                        if (usuario instanceof UsuarioAutenticado u
                                && direitos instanceof Set<?> set) {
                            @SuppressWarnings("unchecked")
                            Set<String> direitosString = (Set<String>) set;
                            return new UsuarioWsPrincipal(u, direitosString);
                        }
                        return null;
                    }
                });
    }

    @Override
    public void configureClientInboundChannel(ChannelRegistration registration) {
        registration.interceptors(canal);
    }
}
