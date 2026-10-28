# NotificationController 학습 가이드

## 1. 역할·레이어·왜·언제

`NotificationController`는 로그인 회원의 알림 HTTP API를 제공합니다. 사용자 ID는 요청값을 신뢰하지 않고 JWT 인증 필터가 `SecurityContext`에 저장한 principal에서 가져와 회원 간 데이터 접근을 제한합니다.

## 2. 현재 원본 코드의 핵심

```java
@RequestMapping("/api/notifications")
public class NotificationController {
	@GetMapping("/home")
	public NotificationHomeResponse getHomeNotifications() {
		return notificationService.getHomeNotifications(currentMemberId());
	}

	@GetMapping
	public NotificationPageResponse getNotifications(@RequestParam(defaultValue = "0") @Min(0) int page,
		@RequestParam(defaultValue = "10") @Min(1) @Max(100) int size) {
		PageRequest pageable = PageRequest.of(page, size, Sort.by(Sort.Order.desc("createdAt"), Sort.Order.desc("id")));
		return notificationService.getNotifications(currentMemberId(), pageable);
	}

	@GetMapping("/unread-count")
	public long getUnreadCount() {
		return notificationService.getUnreadCount(currentMemberId());
	}

	@GetMapping(value = "/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
	public SseEmitter streamNotifications() throws IOException {
		return notificationStreamService.subscribe(currentMemberId());
	}

	@PatchMapping("/{id}/read")
	public NotificationResponse markAsRead(@PathVariable Long id) {
		return notificationService.markAsRead(currentMemberId(), id);
	}

	@PatchMapping("/read-all")
	public int markAllAsRead() {
		return notificationService.markAllAsRead(currentMemberId());
	}
}
```

실제 구현에는 생성자 주입, 인증 principal 검증용 `currentMemberId()`가 있으며 각 API는 이 메서드로 인증된 회원 ID를 구합니다.

## 3. 선언과 입력 검증

- `@RestController`는 반환 객체를 HTTP 응답 본문으로 직렬화합니다.
- `@Validated`는 컨트롤러 메서드 매개변수의 `@Min`, `@Max` 검증을 활성화합니다.
- `@RequestMapping("/api/notifications")`는 각 메서드 경로의 공통 접두사입니다.
- 페이지는 0부터 시작하고 기본값은 `page=0`, `size=10`입니다. `@Min(1) @Max(100)`으로 페이지 크기를 제한합니다.
- `Sort.Order.desc("createdAt")`, 이어서 `id` 내림차순 정렬을 적용해 같은 생성 시각에서도 결과 순서를 안정적으로 정합니다.
- `MediaType.TEXT_EVENT_STREAM_VALUE`는 `/stream` 응답이 SSE 형식임을 선언합니다.

## 4. 실제 실행 흐름

1. Spring Security가 요청 JWT를 확인하고 principal을 `SecurityContext`에 둡니다.
2. Controller가 `currentMemberId()`를 호출합니다. principal이 숫자가 아니거나 인증 정보가 없으면 `InvalidCredentialsException`을 던집니다.
3. API별 입력값과 회원 ID를 `NotificationService` 또는 `NotificationStreamService`에 전달합니다.
4. 서비스 결과를 Spring MVC가 JSON 또는 SSE 스트림 응답으로 반환합니다.

## 5. 경로별 동작

- `GET /home`은 최신 세 건과 전체 미읽음 개수입니다.
- `GET /`는 검증된 페이지 번호·크기로 보관 목록을 조회합니다.
- `GET /unread-count`는 배지 표시를 위한 전체 미읽음 개수입니다.
- `GET /stream`은 현재 회원의 SSE emitter를 생성하며, 스트림 연결에도 JWT 인증이 필요합니다.
- `PATCH /{id}/read`는 특정 알림을, `PATCH /read-all`은 회원의 미읽음 알림을 읽음 처리합니다.

## 6. 관계: 확인된 사실과 추론

- **코드로 확인됨:** [`NotificationService`](../../src/main/java/com/cleaner/be/notification/NotificationService.java)가 저장소 작업과 소유자 검사를 수행합니다.
- **코드로 확인됨:** [`NotificationStreamService`](../../src/main/java/com/cleaner/be/notification/NotificationStreamService.java)가 SSE 구독을 등록합니다.
- **코드로 확인됨:** [`SecurityConfig`](../../src/main/java/com/cleaner/be/auth/SecurityConfig.java)의 기본 규칙은 공개 경로 외 요청에 인증을 요구합니다.
- **코드로 확인됨:** 잘못되거나 누락된 principal은 인증 예외로 처리됩니다.
- **추론:** 실제 JSON 필드 표기와 오류 응답 형태는 DTO 및 공통 예외 처리 설정에 따라 직렬화됩니다.

## 7. 이 파일에만 적용되는 개념

Spring MVC의 매개변수 어노테이션은 URL의 query, path, body 등을 Java 메서드 인자에 연결합니다. `@Validated`와 Jakarta Validation 제약은 컨트롤러 경계에서 잘못된 페이지 입력을 서비스로 넘기기 전에 거부합니다. SSE는 일반 JSON 응답을 모두 받은 뒤 닫는 대신 연결을 유지하며 이벤트를 보냅니다.

## 8. 예시

```text
GET /api/notifications?page=0&size=10
Authorization: Bearer <access-token>
```

인증 토큰은 예시 자리표시자이며 실제 비밀값이 아닙니다. 서버는 토큰에서 회원 ID를 얻고 첫 페이지를 조회합니다.

## 9. 요약

Controller는 HTTP 경로와 입력 검증을 담당하고, 회원 소유 범위 및 데이터 처리는 서비스에 위임합니다. 회원 ID를 요청 매개변수로 받지 않는 것이 알림 데이터 격리의 핵심입니다.
