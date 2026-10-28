# NotificationCreatedEvent 학습 가이드

## 1. 역할·레이어·왜·언제

`NotificationCreatedEvent`는 새 알림을 저장한 서비스에서 SSE 전달 계층으로 전달하는 애플리케이션 이벤트입니다. 예약 서비스가 SSE 구현을 직접 호출하지 않도록 계층을 분리합니다.

## 2. 현재 원본 코드

```java
package com.cleaner.be.notification;

import com.cleaner.be.notification.dto.NotificationResponse;

/** 커밋된 알림을 회원별 SSE 구독자에게 전달하기 위해 사용하는 애플리케이션 이벤트입니다. */
public record NotificationCreatedEvent(Long memberId, NotificationResponse notification) {
}
```

## 3. 코드 한 줄씩

- `package`는 알림 기능의 기본 패키지에 이벤트 타입을 둡니다.
- `NotificationResponse`는 전달할 알림의 공개 API 필드만 담습니다.
- `record`는 이벤트 데이터의 두 구성 요소를 불변 형태로 보관하고 accessor를 자동 생성합니다.
- `memberId`는 어떤 회원의 SSE 연결을 찾아야 하는지를 나타냅니다.
- `notification`은 event ID와 data로 전달할 알림 응답입니다.

## 4. 사용 흐름

`NotificationService.create`가 알림을 DB에 `saveAndFlush`한 뒤 이 이벤트를 발행합니다. `NotificationStreamService.publish`는 `@TransactionalEventListener(AFTER_COMMIT)`으로 이벤트를 받으므로 DB 커밋이 확인된 경우에만 실시간 전달합니다. 이벤트 자체는 저장이나 전송을 수행하지 않습니다.

## 5. 관계: 확인된 사실과 추론

- **코드로 확인됨:** [`NotificationService`](../../src/main/java/com/cleaner/be/notification/NotificationService.java)가 이벤트를 생성·발행합니다.
- **코드로 확인됨:** [`NotificationStreamService`](../../src/main/java/com/cleaner/be/notification/NotificationStreamService.java)가 이벤트를 처리합니다.
- **코드로 확인됨:** 알림 payload는 [`NotificationResponse`](../../src/main/java/com/cleaner/be/notification/dto/NotificationResponse.java)입니다.
- **추론:** 이 애플리케이션 이벤트는 프로세스 내부 전송이며, 메시지 브로커에 기록되는 durable 메시지는 아닙니다.

## 6. 이 파일에만 적용되는 개념

Java `record`는 데이터 전달 객체를 간결하게 표현하며 구성 요소가 생성자 매개변수 및 accessor가 됩니다. Spring application event는 같은 애플리케이션 내부에서 발행자와 리스너를 느슨하게 연결합니다.

## 7. 예시

```java
new NotificationCreatedEvent(memberId, notificationResponse)
```

서비스가 이벤트 publisher를 통해 전달하는 데이터 형태입니다. 이벤트의 처리는 트랜잭션 커밋 시점까지 지연됩니다.

## 8. 요약

이 이벤트는 수신 회원과 알림 응답을 담는 불변 전달 자료입니다. DB 저장은 서비스, 커밋 후 SSE 전송은 스트림 서비스의 책임입니다.
