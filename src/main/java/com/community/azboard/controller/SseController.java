package com.community.azboard.controller;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
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
    public ResponseEntity<SseEmitter> subscribe() {
        // 연결 유효 시간
        SseEmitter emitter = new SseEmitter(60 * 60 * 1000L);
        String emitterId = String.valueOf(System.currentTimeMillis());

        emitters.put(emitterId, emitter);
        log.info("새로운 SSE 연결 시도 - Emitter ID: {}", emitterId);

        emitter.onCompletion(() -> {
            log.info("SSE 연결 완료(종료) - Emitter ID: {}", emitterId);
            emitters.remove(emitterId);
        });
        emitter.onTimeout(() -> {
            log.info("SSE 연결 타임아웃 - Emitter ID: {}", emitterId);
            emitter.complete();
            emitters.remove(emitterId);
        });
        emitter.onError((e) -> {
            log.error("SSE 연결 오류 - Emitter ID: {}", emitterId, e);
            emitters.remove(emitterId);
        });

        // 처음 연결 시 503 에러 방지용 더미 데이터 전송
        try {
            emitter.send(SseEmitter.event()
                    .name("connect")
                    .data("connected!"));
            log.info("더미 이벤트 전송 완료");
        } catch (IOException e) {
            log.error("더미 이벤트 전송 실패", e);
            emitters.remove(emitterId);
        }

        HttpHeaders headers = new HttpHeaders();
        headers.setCacheControl("no-cache, no-store, must-revalidate");
        headers.setPragma("no-cache");
        headers.setExpires(0L);

        return ResponseEntity.ok()
                .headers(headers)
                .body(emitter);
    }
}
