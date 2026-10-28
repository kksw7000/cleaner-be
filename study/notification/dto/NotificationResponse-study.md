# NotificationResponse 학습 가이드

## 1. 역할·레이어·왜·언제

`NotificationResponse`는 API와 SSE에서 회원에게 공개할 알림 필드만 전달하는 DTO입니다. JPA 엔티티를 직접 직렬화하지 않아 회원·예약 연관 객체나 내부 상태가 노출되는 것을 막습니다.

## 2. 현재 원본 코드

```java
package com.cleaner.be.notification.dto;

import java.time.LocalDateTime;
import com.cleaner.be.notification.NotificationType;

/** 알림 목록과 읽음 처리 응답에 필요한 공개 필드만 전달합니다. */
public record NotificationResponse(
	Long id,
	NotificationType type,
	String message,
	Long reservationId,
	LocalDateTime createdAt,
	LocalDateTime readAt
) {
}
```

## 3. 필드 설명

- `id`: 개별 알림의 식별자이며 읽음 처리 경로에서 사용합니다.
- `type`: 예약 이벤트 유형입니다.
- `message`: 화면에 표시할 문구입니다.
- `reservationId`: 관련 예약 식별자이며 예약 삭제 후 참조가 끊기면 `null`일 수 있습니다.
- `createdAt`: 알림 생성 시각입니다.
- `readAt`: 읽음 시각이며 `null`이면 미읽음입니다.

## 4. 실제 실행 흐름

`NotificationService.toResponse`가 엔티티에서 각 필드를 추출해 record를 생성합니다. 홈 미리보기, 페이지 조회, 읽음 처리 응답에서 사용하며 신규 알림 이벤트의 SSE data에도 동일한 DTO를 전달합니다.

## 5. 관계: 확인된 사실과 추론

- **코드로 확인됨:** [`Notification`](../../../src/main/java/com/cleaner/be/notification/Notification.java)가 데이터 원본입니다.
- **코드로 확인됨:** [`NotificationService`](../../../src/main/java/com/cleaner/be/notification/NotificationService.java)가 생성합니다.
- **코드로 확인됨:** [`NotificationCreatedEvent`](../../../src/main/java/com/cleaner/be/notification/NotificationCreatedEvent.java)가 SSE payload로 포함합니다.
- **추론:** JSON 날짜 문자열의 구체적인 포맷은 Jackson 및 애플리케이션 직렬화 설정에 따릅니다.

## 6. 이 파일에만 적용되는 개념

DTO는 계층 간 전달할 데이터 형태를 제한합니다. `record`는 불변 데이터 전달 타입에 적합하며 accessor와 생성자를 자동으로 제공합니다.

## 7. 예시

```java
new NotificationResponse(12L, NotificationType.RESERVATION_APPROVED,
	"예약이 확정되었습니다.", 44L, createdAt, null);
```

아직 읽지 않은 예약 확정 알림을 나타내는 예시입니다.

## 8. 요약

이 record는 알림의 API 공개 필드를 정의하며, `readAt`과 `reservationId`는 알림 상태 및 예약 삭제 여부에 따라 null이 될 수 있습니다.
