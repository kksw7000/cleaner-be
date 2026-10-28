# NotificationNotFoundException 학습 가이드

## 1. 역할·레이어·왜·언제

`NotificationNotFoundException`은 요청한 알림이 없거나 로그인 회원의 소유가 아닐 때 발생합니다. 두 경우를 같은 방식으로 응답해 다른 회원의 알림 존재 여부가 노출되지 않도록 합니다.

## 2. 현재 원본 코드

```java
package com.cleaner.be.notification;

/** 존재하지 않거나 현재 회원 소유가 아닌 알림의 접근을 숨기기 위해 사용하는 예외입니다. */
public class NotificationNotFoundException extends RuntimeException {
	public NotificationNotFoundException() {
		super("알림을 찾을 수 없습니다.");
	}
}
```

## 3. 코드 설명

- `RuntimeException`을 상속하므로 별도의 checked exception 선언 없이 서비스에서 발생시킬 수 있습니다.
- 기본 생성자는 동일한 한국어 메시지를 설정합니다.
- 이 예외는 알림 조회를 수행하지 않습니다. 서비스가 repository 결과를 확인한 뒤 생성합니다.

## 4. 실제 실행 흐름

단건 읽음 처리에서 `NotificationService`가 `findByIdAndMemberId(notificationId, memberId)`를 호출합니다. 결과가 없으면 `orElseThrow(NotificationNotFoundException::new)`가 예외를 발생시킵니다. `GlobalExceptionHandler`는 이를 404와 메시지 응답으로 변환합니다.

## 5. 관계: 확인된 사실과 추론

- **코드로 확인됨:** [`NotificationService`](../../src/main/java/com/cleaner/be/notification/NotificationService.java)가 소유자 조건 조회 실패 시 사용합니다.
- **코드로 확인됨:** [`NotificationRepository`](../../src/main/java/com/cleaner/be/notification/NotificationRepository.java)는 ID와 회원 ID를 함께 조건에 사용합니다.
- **코드로 확인됨:** [`GlobalExceptionHandler`](../../src/main/java/com/cleaner/be/auth/GlobalExceptionHandler.java)가 예외를 HTTP 404로 변환합니다.
- **추론:** 실제 오류 JSON 직렬화 형식은 공통 예외 처리기의 반환 객체 구조로 결정됩니다.

## 6. 이 파일에만 적용되는 개념

존재하지 않는 데이터와 접근 권한이 없는 데이터를 같은 응답으로 처리하면 API가 타 회원 데이터의 존재를 확인하는 수단으로 사용되는 것을 줄일 수 있습니다. 이는 인증 자체를 대체하지 않으며, 올바른 회원 ID 조건 조회와 함께 적용됩니다.

## 7. 예시

```java
notificationRepository.findByIdAndMemberId(id, memberId)
	.orElseThrow(NotificationNotFoundException::new);
```

알림 ID가 존재하지 않거나 소유자가 다르면 같은 예외가 발생합니다.

## 8. 요약

이 예외는 알림 부재와 소유권 불일치를 같은 404 의미로 표현합니다. 실제 권한 범위는 서비스와 repository의 회원 ID 조건이 보장합니다.
