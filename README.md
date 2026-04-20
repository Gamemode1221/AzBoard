# Spring Boot + Kafka 실시간 커뮤니티 프로젝트 개발 일지

## 1. 프로젝트 개요
* **목표**: 대용량 트래픽 처리와 시스템 간 결합도를 낮추는(Decoupling) 실시간 커뮤니티 웹서비스 구축 (포트폴리오용)
* **핵심 기능**: 게시판 기본 CRUD, Kafka를 활용한 비동기 새 글 실시간 알림, 실시간 1:1 및 N:N 채팅 (예정)
* **주요 기술 스택**: Java 17+, Spring Boot 3.x, Spring Data JPA, MySQL, Apache Kafka, Docker Compose

---

## 2. 인프라 환경 구축 (Docker Compose)
복잡한 설치 없이 로컬 환경에서 Docker를 활용하여 인프라를 한 번에 구성했습니다.
* **Zookeeper**: Kafka의 분산 상태를 관리하는 코디네이터
* **Kafka Broker**: 실제 메시지가 발행되고 소비되는 메시지 큐 (Port: 9092)
* **MySQL 8.0**: 사용자 및 게시글 데이터 영구 저장소 (Port: 3306)

---

## 3. 백엔드 기본 구조 완성 (Layered Architecture)

### 3.1. Entity 설계 (`Post.java`)
* JPA `@Entity`를 사용하여 객체와 MySQL `post` 테이블을 매핑.
* `id(PK)`, `title`, `content`, `author`, `createdAt` 필드로 구성.
* `@PrePersist`를 활용하여 DB Insert 직전 생성 시간을 자동으로 세팅.

### 3.2. Repository & Service (`PostRepository`, `PostService`)
* `JpaRepository` 인터페이스를 상속받아 기본적인 데이터베이스 CRUD 작업 구현.
* `@Transactional`을 적용하여 데이터 무결성 보장.

### 3.3. Controller 및 입력값 검증 (`PostController`, `PostCreateRequest`)
* `spring-boot-starter-validation` 의존성을 추가하여 안전한 REST API 구현.
* 전용 DTO 객체에 `@NotBlank`, `@Size` 제약 조건을 걸어 비정상적인 데이터(예: 빈 제목) 유입을 1차적으로 차단 (400 Bad Request 반환 확인).

---

## 4. Kafka 비동기 이벤트 시스템 연동
게시글 저장 트랜잭션과 알림 전송 로직을 분리(Decoupling)하여 시스템 성능과 안정성을 높였습니다.

### 4.1. JSON 직렬화(Serialization) 설정
* 단순 문자열 통신의 한계를 극복하고자 `application.yml`에 직렬화 설정 추가.
* `StringSerializer` 대신 `JsonSerializer` 및 `JsonDeserializer`를 적용하여 객체 자체를 메시지로 송수신.

### 4.2. 이벤트 객체 (`PostCreatedEvent.java`)
* 알림 전송에 필수적인 데이터(`postId`, `author`, `title`)만을 담은 이벤트 DTO 생성.

### 4.3. Producer & Consumer 구현
* **KafkaProducerService**: `KafkaTemplate`을 사용하여 `community.new-post` 토픽으로 JSON 형태의 새 글 작성 이벤트를 브로드캐스팅.
* **KafkaConsumerService**: `@KafkaListener`를 사용하여 해당 토픽의 메시지를 성공적으로 수신 및 역직렬화 (로그 확인 완료).

---

## 5. 다음 단계 (Next Steps)
1. **SSE (Server-Sent Events) 연동**: Kafka Consumer가 받은 알림 데이터를 실제 접속 중인 클라이언트(웹 브라우저) 화면에 실시간 팝업으로 띄우기.
2. **실시간 채팅 기능 도입**: WebSocket과 STOMP 프로토콜, 그리고 Kafka를 연계하여 다중 서버 환경에서도 끊김 없는 채팅 기능 구현.
3. **조회수 처리 및 Redis 캐싱**: 어뷰징 방지 및 성능 최적화.