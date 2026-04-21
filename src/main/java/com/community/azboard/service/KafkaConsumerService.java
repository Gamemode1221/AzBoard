package com.community.azboard.service;

import com.community.azboard.controller.SseController;
import com.community.azboard.dto.PostCreatedEvent;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;

@Slf4j
@Service
public class KafkaConsumerService {

    @KafkaListener(topics = "community.new-post", groupId = "community-group")
    public void consume(PostCreatedEvent event) {
        log.info("\uD83D\uDCE9 [Consumer] Kafka 메시지 수신: {}", event.getTitle());

        // 접속 중인 모든 브라우저에 알림 전송
        SseController.emitters.forEach((id, emitter) -> {
            try {
                emitter.send(SseEmitter.event()
                        .name("newPost")
                        .data(event));
            } catch (IOException e) {
                SseController.emitters.remove(id);
            }
        });
    }
}
