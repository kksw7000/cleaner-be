# NotificationService 학습 가이드

## 1. 역할·레이어·왜·언제

`NotificationService`는 알림 생성, 최신 미리보기, 페이지 조회, 읽음 처리의 업무 흐름을 담당합니다. Controller와 Repository 사이에 두어 회원 소유 조건, 표시 정책, 트랜잭션을 한 곳에서 적용합니다.

## 2. 저장과 이벤트 발행

```java
@Transactional
public Notification create(Member member, Reservation reservation, NotificationType type, String message) {
	Notification notification = notificationRepository.saveAndFlush(
		new Notification(member, reservation, type, message, LocalDateTime.now(koreaClock)));
	eventPublisher.publishEvent(new NotificationCreatedEvent(member.getId(), toResponse(notification)));
	return notification;
}
```

먼저 알림을 저장하고 생성된 ID를 포함한 응답 데이터로 애플리케이션 이벤트를 발행합니다. 이 메서드는 예약 서비스의 트랜잭션 안에서 호출되므로 저장과 예약 상태 변경은 같은 트랜잭션에 참여합니다. 이벤트 수신부는 `AFTER_COMMIT` 단계로 제한되어 롤백된 알림이 SSE로 전송되지 않습니다.

## 3. 조회와 읽음 규칙

- `getHomeNotifications(memberId)`는 repository에서 회원의 최신 3개를 조회하고 별도 집계로 전체 미읽음 개수를 구합니다. 홈 표시 개수와 미읽음 총수는 서로 다른 범위입니다.
- `getNotifications(memberId, pageable)`는 회원 ID 조건이 포함된 페이지 조회를 DTO 페이지 응답으로 변환합니다.
- `getUnreadCount(memberId)`는 회원의 `readAt == null`인 전체 개수를 반환합니다.
- `markAsRead(memberId, notificationId)`는 ID와 회원 ID가 모두 일치하는 알림만 찾습니다. 없으면 `NotificationNotFoundException`으로 처리하고, 있으면 `readAt`이 비어 있을 때만 현재 시각을 기록합니다.
- `markAllAsRead(memberId)`는 현재 회원의 미읽음 행만 한 번의 업데이트로 처리하고 변경 행 수를 반환합니다.

## 4. 시간과 응답 변환

`Clock`을 주입받아 `LocalDateTime.now(koreaClock)`으로 생성·읽음 시각을 구합니다. 고정 시계를 테스트에 주입할 수 있어 시간 검증을 결정적으로 수행할 수 있습니다. `toResponse`는 엔티티를 API DTO로 변환하고 예약이 삭제되어 참조가 비어 있으면 `reservationId`를 `null`로 반환합니다.

## 5. 실제 실행 흐름

예약 서비스가 알림 이벤트를 만들면 `create`가 DB 저장 후 이벤트를 발행합니다. 조회 요청은 회원 ID를 repository 메서드에 전달하여 다른 회원 데이터가 결과에 섞이지 않도록 합니다. 단건 읽음 처리도 repository 소유자 조회 후 엔티티를 변경합니다. `@Transactional`은 쓰기 작업을, `@Transactional(readOnly = true)`는 조회 작업을 감쌉니다.

## 6. 관계: 확인된 사실과 추론

- **코드로 확인됨:** [`NotificationRepository`](../../src/main/java/com/cleaner/be/notification/NotificationRepository.java)가 실제 조회 조건과 일괄 업데이트를 제공합니다.
- **코드로 확인됨:** [`NotificationCreatedEvent`](../../src/main/java/com/cleaner/be/notification/NotificationCreatedEvent.java)는 회원 ID와 응답 DTO를 이벤트로 운반합니다.
- **코드로 확인됨:** [`NotificationStreamService`](../../src/main/java/com/cleaner/be/notification/NotificationStreamService.java)는 트랜잭션 커밋 이후 이벤트를 수신합니다.
- **코드로 확인됨:** [`NotificationServiceTest`](../../src/test/java/com/cleaner/be/notification/NotificationServiceTest.java)는 홈 제한, 페이지, 소유자 필터, 읽음 규칙을 검증합니다.
- **추론:** 실제 알림 생성 호출자는 현재 예약 생성·관리자 상태 변경·삭제 경로이며, 새 이벤트 종류를 추가하면 업무 서비스 연결도 필요합니다.

## 7. 이 파일에만 적용되는 개념

Spring의 `@TransactionalEventListener(phase = AFTER_COMMIT)`는 DB 트랜잭션이 성공적으로 커밋된 경우에만 이벤트를 처리합니다. 일반 이벤트 발행은 메서드 호출 시점에 발생하지만, 리스너 실행 시점을 트랜잭션 종료 단계로 연기하는 방식입니다.

## 8. 예시

```java
notificationService.create(member, reservation, NotificationType.RESERVATION_RECEIVED,
	"예약 요청이 접수되었습니다.");
```

예약 생성 서비스에서 사용하는 형태입니다. 실제 호출은 예약 저장과 같은 트랜잭션 안에서 이뤄집니다.

## 9. 요약

서비스는 알림 저장·조회·읽음 규칙과 DTO 변환을 맡습니다. 커밋 전에는 이벤트 데이터만 준비되고, SSE 전달은 커밋 성공 뒤에 수행됩니다.
