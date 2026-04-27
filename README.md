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

---

# Spring Boot + Kafka 실시간 커뮤니티 프로젝트 개발 일지 (2일차)

## 1. 2일차 개발 목표
* **핵심 목표**: Kafka로 발행된 '새 글 이벤트'를 프론트엔드(웹 브라우저)에 실시간 알림으로 전달하기
* **주요 기술**: SSE (Server-Sent Events), JavaScript (EventSource, Fetch API)

---

## 2. 아키텍처 결정: SSE vs WebSocket
알림과 채팅 기능의 성격을 분리하여 적합한 프로토콜을 선택했습니다.
* **SSE (채택)**: '새 글 알림'과 같은 서버 -> 클라이언트 단방향 브로드캐스팅에 적합. HTTP 기반으로 가볍고 브라우저의 자동 재접속을 지원함.
* **WebSocket (예정)**: 향후 '실시간 채팅'과 같은 양방향 통신이 필요한 기능에 전용으로 사용할 예정.

---

## 3. 백엔드 SSE 시스템 구현

### 3.1. SSE 연결 관리 (`SseController.java`)
* 클라이언트가 알림을 구독할 수 있는 `/api/sse` 엔드포인트 개설.
* `SseEmitter`를 생성하고 `ConcurrentHashMap`을 사용하여 현재 접속 중인 클라이언트들의 세션을 안전하게 관리(Thread-safe).

### 3.2. Kafka - SSE 파이프라인 연결 (`KafkaConsumerService.java`)
* Kafka Listener가 `community.new-post` 토픽에서 JSON 메시지를 수신.
* 수신 즉시 `SseController`에 저장된 모든 접속자(`emitter`)에게 해당 이벤트 데이터를 푸시(Push).

---

## 4. 프론트엔드 UI 및 비동기 연동

### 4.1. 정적 웹페이지 서빙 (`static/index.html`)
* Spring Boot의 기본 정적 자원 경로를 활용하여 별도 프론트엔드 서버 없이 HTML/CSS/JS 렌더링.
* 게시글 작성 폼, 알림용 Toast 메시지 컴포넌트, 게시글 목록 영역 UI 뼈대 작성.

### 4.2. JavaScript 실시간 통신 및 DOM 제어
* **EventSource API**: 서버의 SSE 엔드포인트를 구독하고 `newPost` 이벤트 수신 대기.
* **Fetch API**: 화면 새로고침 없이 비동기 POST 요청으로 게시글 저장 API 호출.

---

## 5. 트러블슈팅: Race Condition (경쟁 상태) 해결
* **증상**: 글 작성 성공 후 알림은 오지만, 서버에 목록(GET)을 다시 요청했을 때 방금 쓴 새 글이 목록에 보이지 않는 문제 발생.
* **원인**: 백엔드의 DB 트랜잭션(Commit)이 완료되기 전에, Kafka 이벤트 발송과 프론트엔드의 GET 요청이 더 빠르게 처리되어 과거의 데이터를 읽어옴 (분산 시스템의 전형적인 Race Condition).
* **해결**: 서버에 목록을 다시 요청하는 대신, SSE 알림으로 넘어온 데이터(작성자, 제목, 번호)를 활용하여 프론트엔드에서 직접 DOM(`prepend`)에 새 글 요소 `<li class="new">`를 끼워 넣는 방식으로 최적화.

---

## 6. ⚠️ 현재 진행 상태 및 실행 대기 (Pending Execution)
**현재 프론트엔드 최적화 코드까지 작성이 완료되었으나, 아직 실제 구동 테스트는 진행하지 않은 상태입니다.** 다음 단계로 넘어가기 전, **이대로 코드를 실행하여 정상 작동 여부를 확인**해야 합니다.

* **테스트 체크리스트**:
    1. 서버 재시작 후 `http://localhost:8080` 접속.
    2. 글 작성 시 비동기로 API가 호출되는지 확인.
    3. 우측 상단에 Toast 알림(작성자, 제목)이 정상적으로 팝업되는지 확인.
    4. 서버 새로고침(GET) 없이 화면 상단에 붉은색 `(New!)` 태그가 붙은 새 글 목록이 즉시 추가되는지 확인.
    5. 브라우저 창을 2개 띄워두고 한쪽에서 작성 시, 다른 쪽 창에도 실시간으로 전파되는지 확인.

---

# Spring Boot + Kafka 실시간 커뮤니티 프로젝트 개발 일지 (3일차)

## 1. 3일차 개발 목표
* **핵심 목표**: WebSocket과 STOMP 프로토콜을 활용한 '실시간 다대다(N:N) 채팅방' 구축
* **주요 기술**: Spring WebSocket, STOMP (Simple Text Oriented Messaging Protocol), SockJS

---

## 2. 백엔드 채팅 시스템 구축 (STOMP Pub/Sub)

### 2.1. 웹소켓 환경 설정 (`WebSocketConfig.java`)
* `spring-boot-starter-websocket` 의존성 추가.
* **Endpoint 설정**: 클라이언트가 웹소켓 통신을 시작할 연결점(Handshake)으로 `/ws-chat` 지정 및 `SockJS` Fallback 적용.
* **Message Broker 설정**:
  * 발행(Publish) 목적지 접두사: `/app` (클라이언트 -> 서버)
  * 구독(Subscribe) 목적지 접두사: `/topic` (서버 -> 클라이언트 브로드캐스팅)

### 2.2. 데이터 전송 객체 (`ChatMessage.java`)
* 채팅 메시지의 성격을 구분하기 위해 `MessageType` Enum 적용 (`JOIN`, `CHAT`, `LEAVE`).
* `sender`(보낸 사람), `content`(내용) 필드 구성.

### 2.3. 채팅 컨트롤러 (`ChatController.java`)
* `@MessageMapping`: HTTP의 서블릿 맵핑처럼 웹소켓으로 들어오는 메시지를 라우팅 (`/chat.sendMessage`, `/chat.addUser`).
* `@SendTo`: 가공된 메시지를 `/topic/public` 채널을 구독 중인 모든 클라이언트에게 브로드캐스트.
* `SimpMessageHeaderAccessor`를 활용해 웹소켓 세션에 유저 닉네임 저장(추후 퇴장 알림 등에 활용).

---

## 3. 프론트엔드 실시간 채팅 UI 구현 (`chat.html`)

### 3.1. 화면 레이아웃 및 라이브러리
* 기존 게시판(`index.html`)과 분리된 독립적인 채팅 테스트 페이지 작성.
* CDN을 통해 `SockJS`와 `STOMP.js` 라이브러리 로드.
* 내가 보낸 메시지(우측/파란색), 남이 보낸 메시지(좌측/회색), 시스템 알림(중앙) CSS 분리.

### 3.2. JavaScript STOMP 클라이언트 로직
* `connect()`: 닉네임 입력 후 `/ws-chat` 엔드포인트로 소켓 연결.
* `onConnected()`: 연결 성공 시 즉시 `/topic/public` 채널을 구독(Subscribe)하고, 서버에 `JOIN` 메시지 전송.
* `sendMessage()`: 입력한 텍스트를 JSON 형태로 묶어 `/app/chat.sendMessage`로 발행(Publish).
* `onMessageReceived()`: 구독 채널에서 메시지가 수신되면, 보낸 사람(나/타인/시스템)에 따라 동적으로 DOM(채팅 말풍선)을 생성하여 화면에 렌더링.

---

## 4. ⚠️ 현재 진행 상태 및 실행 대기 (Pending Execution)
**채팅 시스템의 백엔드와 프론트엔드 연동 코드가 모두 작성되었으나, 아직 실제 양방향 통신 테스트를 진행하지 않은 상태입니다.** 다음 단계(Kafka를 통한 채팅 고도화 등)로 넘어가기 전, **반드시 아래 체크리스트에 따라 코드를 실행하고 테스트해야 합니다.**

* **테스트 체크리스트**:
  1. 서버 재시작 후 `http://localhost:8080/chat.html` 접속.
  2. 원활한 다중 접속 테스트를 위해 **일반 브라우저 창 1개, 시크릿 모드 창 1개**를 나란히 배치.
  3. 양쪽 창에서 서로 다른 닉네임(예: user1, user2)으로 입장.
  4. 입장 시 양쪽 화면 중앙에 `[닉네임]님이 입장하셨습니다.` 시스템 메시지가 뜨는지 확인.
  5. 채팅을 입력하고 전송했을 때, 보낸 쪽에서는 우측 말풍선으로, 받는 쪽에서는 좌측 말풍선으로 **새로고침 없이 즉각적으로** 나타나는지 확인.

---

# Spring Boot + Kafka 실시간 커뮤니티 프로젝트 개발 일지 (4일차)

## 1. 4일차 개발 목표
* **핵심 목표**: Kafka를 활용한 다중 서버 웹소켓 통신 환경 구축 및 동적 채팅방 분리 (채팅방 라우팅)
* **주요 기술**: Spring Kafka, Spring WebSocket (STOMP), 동적 Topic 라우팅

---

## 2. 분산 아키텍처 도입 (Kafka + WebSocket)
단일 서버 웹소켓 통신의 한계(서버 간 채팅 단절)를 극복하기 위해 중앙 메시지 브로커(Kafka)를 연동했습니다.

### 2.1. 아키텍처 변경점
* **기존 (단일 서버)**: 클라이언트 ➡️ Controller ➡️ 내장 SimpleBroker ➡️ 클라이언트
* **변경 (분산 서버)**: 클라이언트 ➡️ Controller ➡️ **Kafka 토픽 발행** ➡️ **Kafka 컨슈머 수신** ➡️ 내장 SimpleBroker ➡️ 클라이언트
* **효과**: 서버가 N대로 늘어나더라도 모든 채팅 메시지가 Kafka를 거치기 때문에, 어떤 서버에 접속해 있든 동일한 채팅 메시지를 동기화받을 수 있습니다.

---

## 3. 백엔드: 채팅방 분리 (Dynamic Routing) 구현

### 3.1. DTO 확장 (`ChatMessage.java`)
* 어떤 채팅방의 메시지인지 식별하기 위해 `roomId` 필드 추가.

### 3.2. 메시지 발행 최적화 (`ChatController.java`)
* 기존 `@SendTo` 브로드캐스트를 제거하고, 들어온 메시지를 즉시 `chat.messages` 단일 Kafka 토픽으로 전송(`kafkaTemplate.send()`)하도록 수정.
* 입장 이벤트(`/chat.addUser`) 발생 시 웹소켓 세션에 `username`과 `roomId`를 모두 보관하여 추후 퇴장/연결 끊김 시 활용 가능하도록 구성.

### 3.3. 동적 토픽 분배 (`KafkaConsumerService.java`)
* Kafka의 `chat.messages` 토픽을 구독하는 전용 `@KafkaListener` 추가.
* 수신한 메시지의 `roomId`를 읽고, `SimpMessageSendingOperations`를 사용하여 `/topic/room/{roomId}` 형태의 **동적 목적지(Destination)로만 메시지를 전파**하도록 라우팅 처리.

---

## 4. 프론트엔드: 채팅방 접속 UI 개선 (`chat.html`)
* 닉네임과 함께 **접속할 방 번호(`roomId`)**를 입력받을 수 있도록 UI 수정.
* `connect()` 시, 입력한 방 번호를 기반으로 `stompClient.subscribe('/topic/room/' + currentRoomId)` 를 호출하여 해당 방의 메시지만 선별적으로 수신하도록 설정.
* 메시지 송신(`sendMessage`) 시, payload 내부에 `roomId`를 포함하여 전송 (이로 인해 발생했던 Silent Fail 버그 해결 완료).

---

## 5. ⚠️ 트러블슈팅: Silent Fail (주소 불일치 버그)
* **증상**: 프론트엔드에서 채팅 전송 시 백엔드 컨슈머까지 정상 수신되었으나, 프론트엔드 화면에 다른 사람의 채팅이 뜨지 않음.
* **원인**: 백엔드는 `/topic/room/{roomId}` (슬래시 포함)로 메시지를 쏘고 있었으나, 프론트엔드의 구독 주소는 `/topic/room{roomId}` (슬래시 누락, 예: `/topic/room1`)로 설정되어 있어 목적지 불일치가 발생.
* **해결**: 프론트엔드의 `subscribe` URL 문자열 조합 시 누락된 `/`를 추가하여 주소를 정확히 매칭시킴으로써 버그 해결.

---

# Spring Boot + Kafka 실시간 커뮤니티 프로젝트 개발 일지 (5일차)

## 1. 5일차 개발 목표
* **핵심 목표**: 채팅 데이터 영구 저장(Persistence) 및 과거 대화 내역 불러오기(History Load) 구현
* **주요 기술**: Spring Data JPA, MySQL, REST API 연동

---

## 2. 데이터 영속성 계층 구현 (MySQL)

### 2.1. 채팅 엔티티 설계 (`Chat.java`)
* 채팅 메시지를 저장할 `Chat` 엔티티 생성.
* `roomId`, `sender`, `content`, `createdAt` 필드로 구성.
* **시간 자동 설정**: 생성자 호출 시 `LocalDateTime.now()`를 통해 메시지 전송 시간을 서버 기준으로 자동 기록하도록 개선.

### 2.2. 리포지토리 최적화 (`ChatRepository.java`)
* **JPA 메서드 쿼리**: `findTop100ByRoomIdOrderByCreatedAtAsc`를 사용하여 특정 방의 메시지 중 최근 100개만 시간순으로 추출.
* **트러블슈팅**: 숫자 없는 `Top` 키워드 사용 시 1개만 조회되는 현상을 발견하고 `Top100`으로 명시하여 해결.

---

## 3. 기능 고도화: 저장 및 복구 프로세스

### 3.1. 메시지 저장 로직 (`ChatController.java`)
* 클라이언트로부터 `CHAT` 타입의 메시지가 수신되면 Kafka로 발행하기 전 `ChatRepository.save()`를 통해 DB에 영구 저장.
* 데이터베이스 저장과 실시간 전파(Kafka)가 동시에 이루어지는 파이프라인 완성.

### 3.2. 대화 기록 API 구축
* `GET /api/chat/{roomId}` 엔드포인트를 개설하여 방 입장 시 클라이언트에게 과거 데이터를 JSON 형태로 전달.

### 3.3. 프론트엔드 연동 (`chat.html`)
* **loadChatHistory()**: 방 접속 성공 직후 API를 호출하여 화면에 과거 메시지를 렌더링.
* **UI 공통화**: 실시간 수신 메시지와 과거 내역 메시지를 그리는 로직을 `drawMessage()` 함수로 통합하여 코드 가독성 향상.

---

## 4. 🏁 5일간의 프로젝트 총평
* **Day 1**: Docker 기반 Kafka/DB 인프라 구축 및 게시판 기초 완성.
* **Day 2**: SSE와 Kafka를 이용한 비동기 실시간 알림 도입 및 Race Condition 해결.
* **Day 3**: WebSocket + STOMP 기반 실시간 양방향 채팅 구현.
* **Day 4**: 분산 서버 대응을 위한 Kafka 기반 채팅 라우팅 및 동적 채팅방 분리.
* **Day 5**: DB 연동을 통한 데이터 영속성 확보 및 서비스 완성도 제고.

---

## 5. ✨ 향후 확장 가능성 (Next Steps)
본 프로젝트는 **확장성(Extensibility)**을 고려한 아키텍처를 채택하고 있어 다음 기능을 추가하기에 용이합니다:
1. **Redis 도입**: 채팅방 목록이나 유저의 온라인 상태(Presence)를 관리하여 성능 최적화.
2. **Spring Security**: JWT 기반 인증을 통해 로그인 유저만 채팅 및 게시글 작성이 가능하도록 보안 강화.
3. **파일 서버 연동**: 채팅 내 이미지/파일 전송 기능을 위한 S3 연동.
4. **고도화된 검색**: Elasticsearch를 연동하여 방대한 게시글 및 채팅 내역 검색 기능 추가.