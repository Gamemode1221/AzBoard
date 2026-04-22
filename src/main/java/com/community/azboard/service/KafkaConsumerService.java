package com.community.azboard.service;

import com.community.azboard.controller.SseController;
import com.community.azboard.dto.PostCreatedEvent;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
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
}
