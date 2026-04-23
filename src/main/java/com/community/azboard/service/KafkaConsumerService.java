package com.community.azboard.service;

import com.community.azboard.controller.SseController;
import com.community.azboard.dto.ChatMessage;
import com.community.azboard.dto.PostCreatedEvent;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.messaging.simp.SimpMessageSendingOperations;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class KafkaConsumerService {

    private final ObjectMapper objectMapper;

    // 특정 브로커(/topic/public)로 메시지를 쏴주는 String 내장 객체
    private final SimpMessageSendingOperations messagingTemplate;

    @KafkaListener(topics = "community.new-post", groupId = "community-group")
    public void consume(PostCreatedEvent event) {
        log.info("\uD83D\uDCE9 [Consumer] Kafka 메시지 수신: {}", event.getTitle());
        log.info("\uD83D\uDCE1 현재 연결된 SSE 클라이언트 수: {}", SseController.emitters.size());

        // 유령 세션 ID 리스트
        List<String> deadEmitters = new ArrayList<>();

        // 접속 중인 모든 브라우저에 알림 전송
        SseController.emitters.forEach((id, emitter) -> {
            try {
                String jsonData = objectMapper.writeValueAsString(event);
                log.info("\uD83D\uDE80 브라우저로 전송할 데이터: {}", jsonData);
                emitter.send(SseEmitter.event()
                        .name("newPost")
                        .data(jsonData));
            } catch (IOException e) {
                log.info("\uD83D\uDD0C 클라이언트 연결 끊김 감지. Emitter ID: {}", id);
                deadEmitters.add(id);
            } catch (Exception e) {
                log.error("❌ SSE 전송 중 심각한 오류 발생! 클라이언트 ID: {}", id, e);
                deadEmitters.add(id);
            }
        });

        deadEmitters.forEach(SseController.emitters::remove);

        if (!deadEmitters.isEmpty()) {
            log.info("\uD83E\uDDF9 정리된 연결 수: {}", deadEmitters.size());
        }
    }

    // ** 서버를 여러 대 띄울 경우, 모든 서버가 메시지를 받아야 하므로 groupId를 무작위로 만들어줌
    // 퍼블리싱 할 경우, #{T(java.util.UUID).randomUUID().toString()} 사용
    @KafkaListener(topics = "chat.message", groupId = "chat-group")
    public void consumeChatMessage(ChatMessage message) {
        log.info("\uD83D\uDCE9 [Kafka 수신 -> 방 분배] 방 번호: {}, 내용: {}", message.getRoomId(), message.getContent());

        String destination = "/topic/room/" + message.getRoomId();
        // Kafka에서 받은 메시지를 이 서버에 물려있는 웹소켓 클라이언트들에게 뿌려줌
        messagingTemplate.convertAndSend(destination, message);
    }
}
