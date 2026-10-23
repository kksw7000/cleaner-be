# ReservationStatusUpdateRequest 학습 가이드

## 1. 역할·레이어·왜·언제

`ReservationStatusUpdateRequest`는 관리자가 특정 예약 상태를 변경할 때 PATCH 본문으로 보내는 요청 DTO입니다. 단일 상태값만 받으며 null 상태를 막는 제약을 선언합니다.

## 2. 현재 원본 코드

```java
package com.cleaner.be.reservation.dto;

import com.cleaner.be.reservation.ReservationStatus;

import jakarta.validation.constraints.NotNull;

/** 관리자가 예약 처리 결과를 전달하며, null 상태가 저장되어 처리 흐름이 깨지는 것을 막습니다. */
public record ReservationStatusUpdateRequest(@NotNull ReservationStatus status) {
}
```

## 3. 코드 한 줄씩

- `package`는 reservation의 DTO 하위 패키지입니다.
- `ReservationStatus`는 허용 상태값을 지정합니다.
- `NotNull`은 상태가 null이 아니어야 한다는 Jakarta Validation 제약입니다.
- 클래스 주석은 관리자 상태 변경 입력 용도를 설명합니다.
- `public record`는 `status` 한 컴포넌트의 생성자와 `status()` accessor 등을 자동으로 제공합니다.
- `@NotNull ReservationStatus status`는 유일한 요청 필드입니다. 가능한 값은 enum에 선언된 값입니다.
- 빈 record 본문에는 직접 작성한 메서드가 없습니다.

## 4. 메서드 분석

명시적 메서드는 없습니다. record가 생성자와 `status()`를 생성합니다. 관리 Controller가 JSON 본문을 객체로 만들고 `@Valid`가 null 여부를 확인합니다. 이 DTO 자체는 예약 조회나 상태 변경을 수행하지 않으며 validation 오류 응답도 직접 만들지 않습니다.

## 5. 실제 실행 흐름

`PATCH /admin/reservations/{id}/status` 본문이 이 타입으로 역직렬화됩니다. [`AdminReservationController`](../../../src/main/java/com/cleaner/be/admin/AdminReservationController.java)는 `@Valid`로 검증하고 `request.status()`와 경로의 ID, 현재 관리자 ID를 서비스에 전달합니다. 서비스는 관리자 권한을 확인하고 엔티티 상태를 변경합니다. URL 보안 설정의 추가 역할 인가는 AdminReservationController 클래스 주석과 보안 설정에 의해 수행되는 것으로 안내되어 있으나, 요청 DTO가 이를 처리하지 않습니다.

## 6. 관계: 확인된 사실과 추론

- **코드로 확인됨:** 관리자 Controller만 이 타입을 import하고 `@Valid` 요청 본문으로 받습니다.
- **코드로 확인됨:** 서비스 `updateStatusByAdmin`이 받은 enum을 엔티티 `updateStatus`에 전달합니다.
- **코드로 확인됨:** 상태 타입은 [`ReservationStatus.java`](../../../src/main/java/com/cleaner/be/reservation/ReservationStatus.java)입니다.
- **추론:** enum에 존재하는 모든 상태를 모든 현재 상태에서 허용합니다. 상태 전이 제한은 요청 DTO나 서비스에 보이지 않습니다.

## 7. 이 파일에만 적용되는 개념

`@NotNull`은 값의 존재만 검사합니다. 허용 enum 값 중 특정 상태만 허용하거나 현재 상태에 따른 전이를 검사하지는 않습니다. JSON으로 enum을 받을 때 유효하지 않은 문자열의 처리 형식은 Spring/Jackson 설정에 따릅니다.

## 8. 예시

```json
{"status":"APPROVED"}
```

승인 상태 변경 본문 예시입니다. 실제 허용 문자열은 `ReservationStatus` enum에 정의된 상수여야 하고, 인증된 관리자 권한이 필요합니다.

## 9. 요약

관리자 상태 변경에서 상태 한 개를 전달하는 요청 record입니다. null 여부를 선언적으로 검증하지만, 권한과 상태 전이 규칙은 서비스 및 Controller 경로에서 다뤄집니다.
