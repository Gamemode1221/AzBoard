package com.community.azboard.domain;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Chat {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String roomId; // 방 번호
    private String sender; // 보낸 사람

    @Column(columnDefinition = "TEXT")
    private String content;

    private LocalDateTime createdAt; // 전송 시간

    @Builder
    public Chat(String roomId, String sender, String content) {
        this.roomId = roomId;
        this.sender = sender;
        this.content = content;
        this.createdAt = LocalDateTime.now();
    }
}
