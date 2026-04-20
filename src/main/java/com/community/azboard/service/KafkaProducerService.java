package com.community.azboard.service;

import com.community.azboard.dto.PostCreatedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class KafkaProducerService {

    private final KafkaTemplate<String, PostCreatedEvent> kafkaTemplate;
    private static final String TOPIC = "community.new-post";

    public void sendNewPostNotification(Long postId, String author, String title) {
        // String 대신 객체 생성
        PostCreatedEvent event = new PostCreatedEvent(postId, author, title);

        log.info("🚀 [Producer] Kafka로 이벤트를 발행합니다: 글번호={}, 작성자={}", event.getPostId(), event.getAuthor());
        kafkaTemplate.send(TOPIC, event);
    }
}
