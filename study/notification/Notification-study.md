# Notification 엔티티 및 기능 학습 가이드

## 1. 역할과 사용자 흐름

알림 기능은 예약 접수·상태 변경·예약 삭제 이벤트를 로그인한 회원의 알림으로 저장합니다. 홈 미리보기는 최신 세 건과 전체 미읽음 개수를 보여주고, 알림 보관 화면은 읽은 알림을 포함해 페이지 단위로 조회합니다. 알림을 읽으면 최초 읽음 시각을 기록하며, 현재 자동 보관 기간이나 자동 삭제 정책은 없습니다.

예약 서비스가 알림 생성을 요청하면 `NotificationService`가 알림을 DB에 저장하고 애플리케이션 이벤트를 발행합니다. SSE 구독이 열려 있다면 트랜잭션 커밋 이후에만 브라우저로 전달합니다. 연결이 끊긴 동안 놓친 알림은 재연결 후 `GET /api/notifications/home` 또는 `GET /api/notifications`로 조회합니다.

## 2. 데이터와 삭제 관계

[`V7__create_member_notifications.sql`](../../src/main/resources/db/migration/V7__create_member_notifications.sql)은 알림 유형, 표시 문구, 생성 시각, 읽음 시각, 회원 ID, 예약 ID를 저장합니다. 회원 외래 키의 `ON DELETE CASCADE`로 회원 탈퇴 시 해당 회원 알림도 삭제합니다. 예약 외래 키의 `ON DELETE SET NULL`로 예약이 삭제되어도 알림 자체와 문구는 남고 예약 연결만 사라집니다.

읽지 않은 알림은 `readAt == null`, 읽은 알림은 `readAt`에 최초 읽음 시각이 기록된 상태입니다. 이 값으로 화면에서 미읽음과 읽음을 구분할 수 있습니다.

## 3. 현재 원본 코드

```java
@Entity
@Table(name = "notifications")
public class Notification {
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "member_id", nullable = false)
	private Member member;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "reservation_id")
	private Reservation reservation;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 40)
	private NotificationType type;

	@Column(nullable = false, length = 200)
	private String message;

	@Column(nullable = false, updatable = false)
	private LocalDateTime createdAt;

	@Column
	private LocalDateTime readAt;

	public void markAsRead(LocalDateTime readAt) {
		if (this.readAt == null) {
			this.readAt = readAt;
		}
	}
}
```

실제 클래스에는 JPA 기본 생성자, 생성자, getter와 읽음 처리 메서드 주석이 있습니다. 위 코드는 핵심 필드와 읽음 상태 변경을 보여주며 import 및 단순 접근자 일부는 생략했습니다.

## 4. 필드와 엔티티 규칙

- `id`는 DB identity 전략으로 생성되는 알림 고유 ID입니다.
- `member`는 필수 회원 연관 관계입니다. 데이터베이스에서도 회원 삭제 시 알림을 cascade 삭제합니다.
- `reservation`은 nullable 연관 관계입니다. 예약 삭제 뒤에도 알림을 남기기 위해 FK는 `SET NULL`을 사용합니다.
- `type`은 enum 이름 문자열로 저장합니다. 상수는 [`NotificationType`](NotificationType-study.md)에서 정의합니다.
- `message`는 화면에 표시할 알림 문구이며 DB 길이 제한은 200자입니다.
- `createdAt`은 필수 생성 시각이며 생성 후 변경할 수 없습니다.
- `readAt`은 미읽음이면 null, 읽음이면 최초 확인 시각입니다.

## 5. 읽음 상태 변경

`markAsRead(readAt)`는 기존 `readAt`이 null일 때에만 전달받은 시각을 저장합니다. 따라서 중복 요청이 들어와도 최초 읽음 시각은 유지됩니다. 이 메서드가 직접 DB를 호출하지는 않으며, 서비스의 트랜잭션에서 엔티티 변경 감지로 반영됩니다.

## 6. API와 인증

모든 경로는 `/api/notifications` 아래에 있고, 회원 ID는 요청 파라미터 대신 JWT 인증 정보에서 가져옵니다.

| 메서드와 경로 | 목적 |
|---|---|
| `GET /api/notifications/home` | 최신 3개와 전체 미읽음 수를 반환합니다. |
| `GET /api/notifications?page=0&size=10` | 최신순 보관 목록을 페이지로 반환합니다. 크기는 1~100입니다. |
| `GET /api/notifications/unread-count` | 전체 미읽음 수를 반환합니다. |
| `GET /api/notifications/stream` | 인증된 SSE 연결에 새 알림 이벤트를 전달합니다. |
| `PATCH /api/notifications/{id}/read` | 로그인 회원 본인의 알림 하나를 읽음 처리합니다. |
| `PATCH /api/notifications/read-all` | 로그인 회원의 미읽음 알림을 모두 읽음 처리하고 변경 개수를 반환합니다. |

알림 ID와 회원 ID를 함께 사용해 소유자를 확인합니다. 다른 회원의 알림 ID는 존재하지 않는 알림과 같은 404 응답으로 처리합니다.

## 7. 예약 이벤트와 트랜잭션

예약 생성 시 `RESERVATION_RECEIVED` 알림을 저장합니다. 관리자의 상태 변경은 실제 상태가 달라진 경우에만 알림을 생성하며 승인·거절·취소·완료별 유형과 문구를 사용합니다. 예약 상태 변경과 알림 저장이 같은 업무 트랜잭션에 있으므로 알림 저장 실패가 예약 변경과 함께 롤백됩니다.

관리자 예약 삭제는 예약 연결 없이 `RESERVATION_DELETED` 알림을 먼저 저장한 뒤 예약을 삭제합니다. 트랜잭션이 커밋되면 삭제 알림은 남고, 기존 예약을 참조하는 다른 알림의 예약 FK는 DB의 `SET NULL` 동작으로 비워집니다.

## 8. 실시간 알림과 운영 제약

SSE는 브라우저로만 이벤트를 보내는 단방향 연결입니다. 연결은 회원 ID별로 관리되고, 모든 브라우저 탭에 `notification` 이벤트를 전달합니다. `connected` 이벤트는 연결 확인용이며 알림 내용이 아닙니다. 연결 완료·오류·30분 시간 초과 시 emitter를 정리합니다.

SSE 연결 정보는 각 JVM 메모리에만 저장되므로 여러 백엔드 인스턴스 중 알림을 생성한 인스턴스와 사용자의 SSE가 연결된 인스턴스가 다르면 실시간 이벤트가 전달되지 않을 수 있습니다. 다중 인스턴스 환경의 fan-out은 메시지 브로커가 필요합니다. 브로커가 없어도 DB 알림 조회 API로 저장된 알림을 확인할 수 있습니다.

현재 Access Token은 `Authorization` 헤더로 전달됩니다. 기본 브라우저 `EventSource`는 임의 Authorization 헤더를 설정할 수 없으므로 프론트엔드는 인증 헤더를 설정할 수 있는 `fetch` 스트리밍 클라이언트를 사용해야 합니다. 토큰을 URL 쿼리 파라미터에 넣지 않습니다. 재연결 후 홈/알림 API를 조회해 연결이 끊긴 동안의 알림을 동기화합니다.

## 9. 클래스별 학습 노트

- [`NotificationController`](NotificationController-study.md): HTTP 경로, JWT 회원 ID, 페이지 입력 검증을 설명합니다.
- [`NotificationService`](NotificationService-study.md): 저장·조회·읽음 처리와 트랜잭션 경계를 설명합니다.
- [`NotificationRepository`](NotificationRepository-study.md): 최신순 조회, 회원 소유 조건, 일괄 읽음 JPQL을 설명합니다.
- [`NotificationStreamService`](NotificationStreamService-study.md): SSE emitter 등록·전송·정리와 커밋 후 이벤트 처리를 설명합니다.
- [`NotificationCreatedEvent`](NotificationCreatedEvent-study.md): 저장 후 SSE 서비스로 전달하는 이벤트 데이터를 설명합니다.
- [`NotificationType`](NotificationType-study.md): API와 화면에서 공유하는 알림 유형을 설명합니다.
- [`NotificationNotFoundException`](NotificationNotFoundException-study.md): 소유하지 않은 알림을 포함한 404 예외 흐름을 설명합니다.
- DTO 학습 노트: [`NotificationResponse`](dto/NotificationResponse-study.md), [`NotificationHomeResponse`](dto/NotificationHomeResponse-study.md), [`NotificationPageResponse`](dto/NotificationPageResponse-study.md).

## 10. 요약

DB에 저장된 알림이 기준 데이터이고 SSE는 연결된 브라우저에 새 알림을 빠르게 알려주는 보조 전달 경로입니다. 인증된 회원의 알림만 조회·변경하며, 예약 트랜잭션이 커밋된 뒤에 실시간 이벤트를 보냅니다.
