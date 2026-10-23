# ReservationStatus 학습 가이드

## 1. 역할·레이어·왜·언제

`ReservationStatus`는 예약의 처리 단계를 제한하는 도메인 enum입니다. 예약 생성 시 초기값을 정하고, 관리자가 변경할 수 있는 상태와 API 응답 상태를 표현합니다.

## 2. 현재 원본 코드

```java
package com.cleaner.be.reservation;

/** 예약의 처리 단계를 고정된 값으로 제한해 회원과 관리자가 서로 다른 상태를 해석하지 않게 합니다. */
public enum ReservationStatus {
	PENDING,
	APPROVED,
	COMPLETED,
	CANCELED
}
```

## 3. 코드 한 줄씩

- `package`는 예약 패키지 소속을 선언합니다.
- 클래스 주석은 상태 선택지를 통일하는 목적을 설명합니다.
- `public enum`은 외부 코드에서 사용 가능한 고정 상수 타입을 선언합니다.
- `PENDING`, `APPROVED`, `COMPLETED`, `CANCELED`는 각각 대기, 승인, 완료, 취소를 뜻하는 이름으로 해석됩니다. 구체적인 상태 전이 규칙은 이 파일에 정의되어 있지 않습니다.
- 이 파일에는 필드와 메서드가 없습니다.

## 4. 메서드 분석

선언된 메서드가 없습니다. 서비스의 상태 변경 메서드는 `ReservationStatus` 값을 인자로 받고, 엔티티는 해당 값을 필드에 저장합니다. 이 enum 자체는 순서 검사나 상태 전이를 수행하지 않으므로 별도 입력·반환·예외가 없습니다.

## 5. 실제 실행 흐름

생성 요청은 상태를 받지 않습니다. `Reservation` 생성자의 초기 필드값 `PENDING`이 새 예약에 사용됩니다. 관리자의 상태 변경은 PATCH 요청 DTO의 `status`를 서비스에 전달하고, 서비스가 엔티티의 `updateStatus`를 호출합니다. 실제로 허용되는 전이 순서는 검증되지 않습니다. 확인된 것은 상태를 enum 타입으로 제한한다는 사실뿐입니다.

## 6. 관계: 확인된 사실과 추론

- **코드로 확인됨:** [`Reservation.java`](../../src/main/java/com/cleaner/be/reservation/Reservation.java)에서 초기 상태를 `PENDING`으로 지정하고 문자열 enum으로 저장합니다.
- **코드로 확인됨:** [`ReservationService.java`](../../src/main/java/com/cleaner/be/reservation/ReservationService.java), [`ReservationStatusUpdateRequest.java`](../../src/main/java/com/cleaner/be/reservation/dto/ReservationStatusUpdateRequest.java), [`ReservationResponse.java`](../../src/main/java/com/cleaner/be/reservation/dto/ReservationResponse.java)가 이 타입을 사용합니다.
- **코드로 확인됨:** 관리자 HTTP 경로는 [`AdminReservationController.java`](../../src/main/java/com/cleaner/be/admin/AdminReservationController.java)이며 상태 변경 요청을 서비스에 전달합니다.
- **추론:** `APPROVED` 뒤에 반드시 `COMPLETED`만 허용하는 등의 상태 전이는 현재 엔티티에 규칙이 없어 보장되지 않습니다.

## 7. 이 파일에만 적용되는 개념

enum 값은 허용 선택지를 컴파일 시점에 제한하고 오타가 있는 임의 상태 문자열을 방지합니다. `EnumType.STRING`으로 저장되므로 DB에는 상수명이 기록되며, 이름 변경은 기존 데이터와의 호환성에 영향을 줄 수 있습니다.

## 8. 예시

```java
ReservationStatus status = ReservationStatus.PENDING;
```

새 예약 엔티티의 기본 상태와 같은 예입니다. 상태값만 대입한 것이며, 실제 저장은 서비스의 트랜잭션 안에서 JPA가 수행합니다.

## 9. 요약

예약이 가질 수 있는 네 상태를 고정합니다. 초기값은 `PENDING`이고, 유효한 상태 간 전이 규칙은 이 enum에 구현되어 있지 않습니다.
