package com.community.azboard;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class TestController {

    @GetMapping("/api/test")
    public String hello() {
        return "커뮤니티 프로젝트 서버가 성공적으로 실행되었습니다! Kafka 연동 준비 완료";
    }
}
