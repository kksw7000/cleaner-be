# NotificationType 학습 가이드

## 1. 역할·레이어·왜·언제

`NotificationType`은 알림의 업무상 종류를 고정된 값으로 나타내는 enum입니다. 프론트엔드는 type을 이용해 예약 이벤트의 종류를 구분할 수 있습니다.

## 2. 현재 원본 코드

```java
package com.cleaner.be.notification;

/** 알림 유형을 고정된 값으로 관리해 프론트엔드가 이벤트별 표시와 이동 동작을 구분하게 합니다. */
public enum NotificationType {
	RESERVATION_RECEIVED,
	RESERVATION_APPROVED,
	RESERVATION_REJECTED,
	RESERVATION_CANCELLED,
	RESERVATION_COMPLETED,
	RESERVATION_DELETED
}
```

## 3. 값의 의미

- `RESERVATION_RECEIVED`: 예약 요청이 접수된 상태를 알립니다.
- `RESERVATION_APPROVED`: 예약이 승인·확정되었음을 알립니다.
- `RESERVATION_REJECTED`: 예약이 거절되었음을 알립니다.
- `RESERVATION_CANCELLED`: 예약이 취소되었음을 알립니다.
- `RESERVATION_COMPLETED`: 예약이 완료되었음을 알립니다.
- `RESERVATION_DELETED`: 관리자가 예약을 삭제했음을 알립니다.

## 4. 실제 실행 흐름

예약 서비스가 예약 생성 시 `RESERVATION_RECEIVED`를 사용합니다. 관리자 상태 변경 시 상태에 맞는 type을 선택하며, 상태가 실제로 바뀐 경우에만 알림을 생성합니다. 관리자 예약 삭제는 예약 ID를 잃더라도 의미를 보존하도록 `RESERVATION_DELETED` 알림을 만듭니다. 엔티티는 `EnumType.STRING` 방식으로 저장합니다.

## 5. 관계: 확인된 사실과 추론

- **코드로 확인됨:** [`ReservationService`](../../src/main/java/com/cleaner/be/reservation/ReservationService.java)가 예약 상태별 유형을 선택합니다.
- **코드로 확인됨:** [`Notification`](../../src/main/java/com/cleaner/be/notification/Notification.java)이 `type`을 문자열 enum으로 저장합니다.
- **코드로 확인됨:** [`NotificationResponse`](../../src/main/java/com/cleaner/be/notification/dto/NotificationResponse.java)가 API 응답에 type을 포함합니다.
- **추론:** enum 상수 이름을 변경하면 DB에 이미 저장된 문자열 및 프론트엔드 조건문과 호환성 문제가 생길 수 있습니다.

## 6. 이 파일에만 적용되는 개념

Java enum은 정해진 선택지 이외의 값을 만들지 못하게 합니다. `EnumType.STRING`은 ordinal 숫자 대신 이름을 저장하므로 상수 순서를 바꿔도 저장된 값의 의미가 달라지지 않지만, 이름 자체는 스키마 계약처럼 신중히 변경해야 합니다.

## 7. 예시

```java
NotificationType type = NotificationType.RESERVATION_APPROVED;
```

관리자 승인 시 예약 서비스에서 선택하는 유형과 같은 값입니다.

## 8. 요약

알림 유형은 예약 이벤트를 API와 DB에서 일관되게 식별하는 고정 문자열 값입니다. 새 유형을 추가할 때 서비스의 매핑과 화면 처리도 함께 확인해야 합니다.
