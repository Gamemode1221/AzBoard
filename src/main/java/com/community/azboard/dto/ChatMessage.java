package com.community.azboard.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ChatMessage {

    // 메시지 타입: 입장, 채팅, 퇴장
    public enum MessageType {
        JOIN, CHAT, LEAVE
    }

    private MessageType type; // 메시지 타입
    private String content;   // 메시지 내용
    private String sender;    // 보낸 사람
}
