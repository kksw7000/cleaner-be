# NotificationStreamService 학습 가이드

## 1. 역할·레이어·왜·언제

`NotificationStreamService`는 로그인 회원별 SSE 연결(`SseEmitter`)을 관리합니다. 알림 이벤트가 DB 트랜잭션 커밋 후 도착하면 현재 연결된 해당 회원의 브라우저 탭에 실시간으로 전달합니다.

## 2. 연결 등록

```java
public SseEmitter subscribe(Long memberId) throws IOException {
	SseEmitter emitter = new SseEmitter(STREAM_TIMEOUT_MILLIS);
	emittersByMember.compute(memberId, (ignored, emitters) -> {
		Set<SseEmitter> memberEmitters = emitters == null ? ConcurrentHashMap.newKeySet() : emitters;
		memberEmitters.add(emitter);
		return memberEmitters;
	});
	// 종료·시간 초과·오류 callback 등록 및 connected 이벤트 전송
	return emitter;
}
```

회원 ID를 키로 하여 동시에 여러 탭을 보유할 수 있도록 emitter 집합을 둡니다. `ConcurrentMap.compute` 안에서 집합 생성과 emitter 추가를 함께 실행하므로 같은 회원의 동시 구독·제거가 경합해 새 연결이 맵에서 누락되는 일을 방지합니다. 현재 연결 시간 제한은 30분입니다.

## 3. 전송과 정리

`publish(NotificationCreatedEvent)`는 `@TransactionalEventListener(phase = AFTER_COMMIT)`으로 등록됩니다. 이벤트의 회원 ID로 연결 집합을 찾고, 각 emitter에 `notification` 이벤트를 보냅니다. 알림 ID는 SSE event ID로 포함하고 응답 DTO는 이벤트 data로 전송합니다. 전송이 실패한 연결은 제거하고 오류로 완료시킵니다.

`connected` 이벤트는 구독 직후 연결 확인용으로 전송합니다. completion, timeout, error callback은 `remove`를 호출합니다. `remove`도 `computeIfPresent`를 사용해 집합 변경과 빈 집합 제거를 같은 키의 원자적 맵 연산으로 처리합니다. 애플리케이션 종료 시 `closeAll()`이 모든 emitter를 완료합니다.

## 4. 실제 실행 흐름

1. 인증된 `GET /api/notifications/stream` 요청이 Controller에 도착합니다.
2. Controller가 JWT principal의 회원 ID를 서비스에 전달하고 emitter가 회원별 집합에 등록됩니다.
3. 서버가 `connected` 이벤트를 보냅니다.
4. 예약 업무가 알림을 DB에 저장하고 `NotificationCreatedEvent`를 발행합니다.
5. 업무 트랜잭션이 커밋되면 이 서비스가 그 회원의 열려 있는 모든 연결로 `notification`을 보냅니다.
6. 연결이 종료되거나 전송할 수 없으면 callback에서 emitter를 제거합니다.

## 5. 운영상 제약

연결 집합은 현재 프로세스 메모리에만 있습니다. 따라서 백엔드 인스턴스가 하나일 때는 해당 프로세스의 알림과 구독이 연결되지만, 여러 인스턴스에서는 알림 생성 서버가 다른 서버의 emitter를 직접 볼 수 없습니다. 다중 인스턴스 전달은 메시지 브로커 등을 이용한 fan-out이 필요합니다. 저장 알림 조회 API는 실시간 연결이 끊겼을 때의 동기화 경로입니다.

## 6. 관계: 확인된 사실과 추론

- **코드로 확인됨:** [`NotificationController`](../../src/main/java/com/cleaner/be/notification/NotificationController.java)가 SSE endpoint에서 `subscribe`를 호출합니다.
- **코드로 확인됨:** [`NotificationService`](../../src/main/java/com/cleaner/be/notification/NotificationService.java)는 알림 저장 후 이벤트를 발행합니다.
- **코드로 확인됨:** 이벤트 리스너의 단계는 `AFTER_COMMIT`입니다.
- **코드로 확인됨:** 통합 테스트는 JWT 인증 SSE 연결에서 신규 알림 데이터를 확인합니다.
- **추론:** 네트워크 장비가 실제 연결을 유지하는 최대 시간은 애플리케이션의 30분 timeout 외에도 프록시·서버 설정의 영향을 받을 수 있습니다.

## 7. 이 파일에만 적용되는 개념

SSE는 HTTP 연결을 열어 둔 채 서버에서 클라이언트 방향으로 이벤트를 보내는 표준입니다. `SseEmitter`는 Spring MVC에서 비동기 SSE 응답을 관리합니다. `ConcurrentMap.compute`는 동일 키의 값을 계산하는 연산을 원자화하고, thread-safe set은 한 회원의 동시 연결 집합을 안전하게 변경합니다.

## 8. 예시 이벤트

```text
event:notification
id:123
data:{"id":123,"type":"RESERVATION_APPROVED","message":"예약이 확정되었습니다."}
```

데이터 줄은 실제 직렬화된 `NotificationResponse` 내용에 따라 달라집니다. 실제 Access Token을 URL에 전달하지 말고 Authorization 헤더가 가능한 fetch 기반 스트리밍을 사용합니다.

## 9. 요약

이 서비스는 회원별 SSE 연결을 원자적으로 관리하고, 커밋된 알림만 열린 연결로 전송합니다. 연결은 프로세스 범위이므로 다중 인스턴스 전송에는 별도 fan-out 구성이 필요합니다.
