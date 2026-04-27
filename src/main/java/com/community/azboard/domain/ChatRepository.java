package com.community.azboard.domain;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ChatRepository extends JpaRepository<Chat, Long> {
    // 특정 방의 최근 메시지 100개만 가져오기
    List<Chat> findTop100ByRoomIdOrderByCreatedAtAsc(String roodId);
}
