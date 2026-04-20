package com.community.azboard.service;

import com.community.azboard.dto.PostCreatedEvent;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class KafkaConsumerService {

    @KafkaListener(topics = "community.new-post", groupId = "community-group")
    public void consume(PostCreatedEvent event) {
        log.info("\uD83D\uDCE9 [Consumer] Kafka에서 메시지를 성공적으로 수신했습니다: {}", event);
    }
}
