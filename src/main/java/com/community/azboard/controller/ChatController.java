package com.community.azboard.controller;

import com.community.azboard.domain.Chat;
import com.community.azboard.domain.ChatRepository;
import com.community.azboard.dto.ChatMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.SimpMessageHeaderAccessor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@Slf4j
@RestController
@RequiredArgsConstructor
public class ChatController {

    // Kafka로 메시지를 던지기 위한 템플릿
    private final KafkaTemplate<String, ChatMessage> kafkaTemplate;
    private final ChatRepository chatRepository;

    // 채팅 메시지를 뫄둘 Kafka 토픽 이름
    private static final String CHAT_TOPIC = "chat.message";

    // 메시지 발신 및 DB 저장
    @MessageMapping("/chat.sendMessage")
    public void sendMessage(@Payload ChatMessage chatMessage) {
        // DB에 저장 (CHAT 타입일 때만)
        if (chatMessage.getType() == ChatMessage.MessageType.CHAT) {
            chatRepository.save(Chat.builder()
                    .roomId(chatMessage.getRoomId())
                    .sender(chatMessage.getSender())
                    .content(chatMessage.getContent())
                    .build());
        }
        log.info("\uD83D\uDCAC [채팅 수신] 방: {}, 보낸사람: {}, 내용: {}", chatMessage.getRoomId(), chatMessage.getSender(), chatMessage.getContent());

        // 메시지를 웹소켓으로 바로 뿌리지 않고 Kafka 토픽으로 발행
        kafkaTemplate.send(CHAT_TOPIC, chatMessage);
    }

    // 과거 대화 내역 조회 API
    @GetMapping("/api/chat/{roomId}")
    public List<Chat> getCharHistory(@PathVariable String roomId) {
        return chatRepository.findTop100ByRoomIdOrderByCreatedAtAsc(roomId);
    }

    // 채팅방에 처음 입장할 때 호출
    @MessageMapping("/chat.addUser")
    public void addUser(@Payload ChatMessage chatMessage, SimpMessageHeaderAccessor headerAccessor) {
        // 웹소켓 세션(headerAccessor)에 입장한 유저 이름과 방 번호를 저장
        Map<String, Object> sessionAttributes = headerAccessor.getSessionAttributes();

        if (sessionAttributes != null) {
            headerAccessor.getSessionAttributes().put("username", chatMessage.getSender());
            headerAccessor.getSessionAttributes().put("roomId", chatMessage.getRoomId());
            log.info("\uD83D\uDEAA [채팅 입장 -> Kafka 발송] {}", chatMessage.getSender());

            // Kafka에 입장 메시지 던지기
            kafkaTemplate.send(CHAT_TOPIC, chatMessage);
        } else {
            log.error("유저의 이름을 저장하지 못했습니다!");
        }
    }
}
