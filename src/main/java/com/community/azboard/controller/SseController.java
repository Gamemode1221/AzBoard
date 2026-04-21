package com.community.azboard.controller;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@RestController
public class SseController {

    public static Map<String, SseEmitter> emitters = new ConcurrentHashMap<>();

    @GetMapping(value = "/api/sse", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter subscribe() {
        // 연결 유효 시간
        SseEmitter emitter = new SseEmitter(60 * 60 * 1000L);
        String emitterId = String.valueOf(System.currentTimeMillis());

        // 연결 종료/타임아웃 시 Map에서 제거
        emitter.onCompletion(() -> emitters.remove(emitterId));
        emitter.onTimeout(() -> emitters.remove(emitterId));

        // 처음 연결 시 503 에러 방지용 더미 데이터 전송
        try {
            emitter.send(SseEmitter.event()
                    .name("connect")
                    .data("connected!"));
        } catch (IOException e) {
            log.error("SSE 연결 중 오류 발생", e);
        }

        return emitter;
    }
}
