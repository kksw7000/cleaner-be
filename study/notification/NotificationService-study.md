# NotificationService 학습 가이드

## 1. 역할·레이어·왜·언제

`NotificationService`는 알림 생성, 최신 미리보기, 페이지 조회, 읽음 처리의 업무 흐름을 담당합니다. Controller와 Repository 사이에 두어 회원 소유 조건, 표시 정책, 트랜잭션을 한 곳에서 적용합니다.

## 2. 저장과 이벤트 발행

```java
@Transactional
public Notification create(Member member, Reservation reservation, NotificationType type, String message) {
	// 알림 생성 시각을 주입된 Clock 기준으로 정합니다.
	Notification notification = notificationRepository.saveAndFlush(
		new Notification(member, reservation, type, message, LocalDateTime.now(koreaClock)));
	// 회원 ID와 API 응답 형태의 알림 데이터를 담아 애플리케이션 이벤트를 발행합니다.
	eventPublisher.publishEvent(new NotificationCreatedEvent(member.getId(), toResponse(notification)));
	return notification;
}
```

### 입력값은 어디서 오는가

- `member`: 알림을 받을 회원입니다. 예약 서비스가 예약 소유 회원을 전달합니다.
- `reservation`: 관련 예약입니다. 예약 접수나 상태 변경 알림은 예약을 연결하고, 예약 삭제 알림은 삭제될 예약을 참조하지 않도록 `null`을 전달합니다.
- `type`: 예약 접수·확정·거절·취소·완료·삭제 중 어떤 알림인지 나타내는 [`NotificationType`](NotificationType-study.md) 값입니다.
- `message`: 화면에 표시할 문구입니다. 이 메서드는 문구를 새로 만들지 않고 호출자가 전달한 값을 저장합니다.

### 실행 순서

1. **알림 생성 시각을 정합니다.** `Clock`을 주입받아 `LocalDateTime.now(koreaClock)`을 호출합니다. 이 시각은 예약 자체의 날짜(`scheduledAt`)가 아니라, 예약 접수·상태 변경·삭제 같은 처리가 `NotificationService.create`에 도달해 알림을 만드는 시점입니다. 또한 SSE 전송 시각, 사용자가 읽은 시각과도 다릅니다. 애플리케이션의 `koreaClock`은 `Asia/Seoul` 시간대를 사용하며, 이 방식은 테스트에서 고정 시각을 주입할 수 있게 합니다.
2. **엔티티를 만들고 저장소에 전달합니다.** `new Notification(...)`에 회원, 예약, 유형, 문구, 생성 시각을 넣은 뒤 `saveAndFlush`를 호출합니다.
3. **DB에 SQL을 반영하고 생성된 ID를 확보합니다.** `flush`는 영속성 컨텍스트의 변경 내용을 DB에 보내므로 identity 방식으로 생성된 알림 ID를 이벤트 payload에 넣을 수 있습니다. `createdAt`은 이 저장 전에 1단계에서 정한 시각으로 기록되며, 엔티티에서도 생성 후 변경할 수 없게 설정되어 있습니다. 다만 `flush`는 트랜잭션 커밋이 아닙니다. 이후 트랜잭션이 롤백되면 이 알림과 생성 시각도 DB에 남지 않습니다.
4. **SSE 전송용 응답 데이터를 만듭니다.** `toResponse(notification)`가 엔티티를 `NotificationResponse`로 바꿉니다. JPA 엔티티 전체를 이벤트로 노출하는 대신 화면에 필요한 필드와 생성된 알림 ID만 전달합니다.
5. **애플리케이션 이벤트를 발행합니다.** `NotificationCreatedEvent`에 수신 회원 ID와 응답 데이터를 담아 `ApplicationEventPublisher`에 전달합니다. `publishEvent` 호출 자체는 이벤트를 Spring 내부에 알리는 것이며, 이 시점에 브라우저로 SSE를 보내는 것은 아닙니다.
6. **업무 트랜잭션이 끝납니다.** 예약 생성·상태 변경 같은 호출 경로에서는 `create`가 예약 서비스의 트랜잭션에 참여합니다. 따라서 예약 변경과 알림 저장이 함께 커밋되거나, 실패 시 함께 롤백됩니다.
7. **커밋 성공 뒤 SSE를 보냅니다.** [`NotificationStreamService`](NotificationStreamService-study.md)의 이벤트 리스너는 `@TransactionalEventListener(phase = AFTER_COMMIT)`으로 등록되어 있습니다. 트랜잭션이 커밋된 다음에만 해당 회원의 열린 SSE 연결로 `notification` 이벤트를 보냅니다. 롤백된 예약이나 알림은 실시간 이벤트로 전달되지 않습니다.

여기서 `@Transactional`은 기본 전파 방식으로 이미 진행 중인 업무 트랜잭션에 참여합니다. `create`가 기존 트랜잭션 안에서 호출되면 메서드가 끝나는 순간 별도로 커밋하는 것이 아니라, 바깥 트랜잭션이 최종 커밋될 때 함께 확정됩니다. 트랜잭션 없이 호출되는 경우에는 이 메서드의 트랜잭션이 저장과 이벤트 리스너의 커밋 기준이 됩니다.

### 생성 시각은 어디에 사용되는가

저장된 `createdAt`은 알림이 발생한 순서를 사용자에게 보여주는 기준입니다. 홈 미리보기는 `createdAt` 내림차순으로 최신 세 건을 선택하고, 알림 보관 목록도 같은 순서로 페이지를 나눕니다. 생성 시각이 같은 경우에는 알림 ID 내림차순으로 정렬해 결과 순서를 안정적으로 결정합니다.

`createdAt`은 알림을 만든 시각을 나타내고, `readAt`은 회원이 알림을 처음 확인한 시각을 나타냅니다. 알림 생성 직후 아직 읽지 않았다면 `readAt`은 `null`입니다. 읽음 처리 API가 호출되면 별도로 현재 한국 시각을 기록하며, 이미 값이 있으면 최초 읽음 시각을 유지합니다. 생성 시각은 응답 DTO와 SSE 알림 데이터에도 포함되므로 화면에서 보관 내역의 시각을 표시하는 데 사용할 수 있습니다.

### 왜 저장과 이벤트를 같이 사용하는가

DB 저장은 알림 내역의 영구 기준 데이터이고, 이벤트는 현재 연결된 브라우저에 빠르게 알리는 전달 경로입니다. SSE 연결이 없거나 브라우저가 오프라인이어도 알림은 저장되어 있으므로 재연결 후 `GET /api/notifications/home` 또는 `GET /api/notifications`로 조회할 수 있습니다. 반대로 DB 저장 전에 SSE부터 보내면 나중에 트랜잭션이 실패했을 때 실제로 존재하지 않는 알림이 화면에 보일 수 있으므로, 이 구현은 커밋 이후 전송을 사용합니다.

## 3. 조회와 읽음 규칙

- `getHomeNotifications(memberId)`는 repository에서 회원의 최신 3개를 조회하고 별도 집계로 전체 미읽음 개수를 구합니다. 홈 표시 개수와 미읽음 총수는 서로 다른 범위입니다.
- `getNotifications(memberId, pageable)`는 회원 ID 조건이 포함된 페이지 조회를 DTO 페이지 응답으로 변환합니다.
- `getUnreadCount(memberId)`는 회원의 `readAt == null`인 전체 개수를 반환합니다.
- `markAsRead(memberId, notificationId)`는 ID와 회원 ID가 모두 일치하는 알림만 찾습니다. 없으면 `NotificationNotFoundException`으로 처리하고, 있으면 `readAt`이 비어 있을 때만 현재 시각을 기록합니다.
- `markAllAsRead(memberId)`는 현재 회원의 미읽음 행만 한 번의 업데이트로 처리하고 변경 행 수를 반환합니다.

## 4. 시간과 응답 변환

`Clock`을 주입받아 `LocalDateTime.now(koreaClock)`으로 알림 생성 시각과 읽음 처리 시각을 각각 구합니다. 두 시각은 별도 필드이며, 생성 시각은 알림이 만들어진 순서를 정하고 읽음 시각은 회원의 확인 여부를 나타냅니다. 고정 시계를 테스트에 주입할 수 있어 시간 검증을 결정적으로 수행할 수 있습니다. `toResponse`는 두 시각을 포함한 엔티티 데이터를 API DTO로 변환하고 예약이 삭제되어 참조가 비어 있으면 `reservationId`를 `null`로 반환합니다.

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
