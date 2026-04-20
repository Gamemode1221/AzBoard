package com.community.azboard.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor // 역직렬화(JSON -> Java 객체)를 위해 기본 생성자 필수
@AllArgsConstructor
public class PostCreatedEvent {
    private Long postId;
    private String author;
    private String title;
}
