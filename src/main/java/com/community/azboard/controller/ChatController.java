package com.community.azboard.controller;

import com.community.azboard.dto.ChatMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.SimpMessageHeaderAccessor;
import org.springframework.stereotype.Controller;

import java.util.Map;

@Slf4j
@Controller
@RequiredArgsConstructor
public class ChatController {

    // Kafka로 메시지를 던지기 위한 템플릿
    private final KafkaTemplate<String, ChatMessage> kafkaTemplate;

    // 채팅 메시지를 뫄둘 Kafka 토픽 이름
    private static final String CHAT_TOPIC = "chat.message";

    // 1. 메시지를 보낼 때 호출
    @MessageMapping("/chat.sendMessage")
    public void sendMessage(ChatMessage chatMessage) {
        log.info("\uD83D\uDCAC [채팅 수신] 방: {}, 보낸사람: {}, 내용: {}", chatMessage.getRoomId(), chatMessage.getSender(), chatMessage.getContent());

        // 1. 여기서 DB에 채팅 내역을 저장하는 로직을 추가 할 수 있음
        // 2. 메시지를 웹소켓으로 바로 뿌리지 않고 Kafka 토픽으로 발행
        kafkaTemplate.send(CHAT_TOPIC, chatMessage);
    }

    // 2. 채팅방에 처음 입장할 때 호출
    @MessageMapping("/chat.addUser")
    public void addUser(@Payload ChatMessage chatMessage, SimpMessageHeaderAccessor headerAccessor) {
        // 웹소켓 세션(headerAccessor)에 입장한 유저의 이름을 저장 (나중에 퇴장할 때 누군기 알기 위함)
        Map<String, Object> sessionAttributes = headerAccessor.getSessionAttributes();
        if (sessionAttributes != null) {
            headerAccessor.getSessionAttributes().put("username", chatMessage.getSender());
            // 세션에 유저 이름과 '방 번호'도 같이 저장함 (퇴장처리 유용)
            headerAccessor.getSessionAttributes().put("roomId", chatMessage.getRoomId());
            log.info("\uD83D\uDEAA [채팅 입장 -> Kafka 발송] {}", chatMessage.getSender());

            // Kafka에 입장 메시지 던지기
            kafkaTemplate.send(CHAT_TOPIC, chatMessage);
        } else {
            log.error("유저의 이름을 저장하지 못했습니다!");
        }
    }
}
