package com.community.azboard.controller;

import com.community.azboard.dto.ChatMessage;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.handler.annotation.SendTo;
import org.springframework.messaging.simp.SimpMessageHeaderAccessor;
import org.springframework.stereotype.Controller;

import java.util.Map;

@Slf4j
@Controller
public class ChatController {

    // 1. 메시지를 보낼 때 호출
    @MessageMapping("/chat.sendMessage")
    @SendTo("/topic/public") // 이 메소드가 반환하는 객체를 "/topic/public"을 구독중인 모든 사람에게 보냄
    public ChatMessage sendMessage(ChatMessage chatMessage) {
        log.info("\uD83D\uDCAC [채팅 수신] 보낸사람: {}, 내용: {}", chatMessage.getSender(), chatMessage);
        return chatMessage;
    }

    // 2. 채팅방에 처음 입장할 때 호출
    @MessageMapping("/chat.addUser")
    @SendTo("/topic/public")
    public ChatMessage addUser(@Payload ChatMessage chatMessage, SimpMessageHeaderAccessor headerAccessor) {
        // 웹소켓 세션(headerAccessor)에 입장한 유저의 이름을 저장 (나중에 퇴장할 때 누군기 알기 위함)
        Map<String, Object> sessionAttributes = headerAccessor.getSessionAttributes();
        if (sessionAttributes != null) {
            headerAccessor.getSessionAttributes().put("username", chatMessage.getSender());
        } else {
            log.error("유저의 이름을 저장하지 못했습니다!");
        }

        log.info("\uD83D\uDEAA [채팅 입장] {}", chatMessage.getSender());
        return chatMessage;
    }
}
