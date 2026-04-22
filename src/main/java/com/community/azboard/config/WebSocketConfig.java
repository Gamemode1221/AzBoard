package com.community.azboard.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

@Configuration
@EnableWebSocketMessageBroker
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry){
        // 클라이언트가 웹소켓 서버에 연결할 때 사용할 시작 주소
        registry.addEndpoint("/ws-chat")
                .setAllowedOriginPatterns("*") // 모든 도메인 접속 허용
                .withSockJS(); // 웹소켓을 지원하지 않는 구형 브라우저에서도 동작하게 하는 설정
    }

    @Override
    public void configureMessageBroker(MessageBrokerRegistry registry) {
        // 1. 메시지 구독 요청의 prefix를 지정
        // 클라이언트가 "/topic/room/1"을 구독하면 1번 방의 메시지를 받게됨.
        registry.enableSimpleBroker("/topic");

        // 2. 메시지 발행 요청의 prefix를 지정
        // 클라이언트가 메시지를 보낼 때 "/app/chat"으로 보내면 서버의 Controller가 받게됨.
        registry.setApplicationDestinationPrefixes("/app");
    }
}
