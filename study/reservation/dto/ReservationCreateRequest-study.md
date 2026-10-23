# ReservationCreateRequest 학습 가이드

## 1. 역할·레이어·왜·언제

`ReservationCreateRequest`는 회원이 예약 생성 API에 보내는 입력 DTO입니다. 서비스에서 필요한 예약 종류, 날짜, 방문 위치와 연락 정보를 전달하며 Jakarta Validation 어노테이션으로 입력 제약을 선언합니다.

## 2. 현재 원본 코드

```java
package com.cleaner.be.reservation.dto;

import java.time.LocalDateTime;

import com.cleaner.be.reservation.ServiceCategory;

import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/** 회원이 예약할 서비스·일정·방문 정보를 전달하며, 과거 일정과 빈 필수 입력을 요청 단계에서 차단합니다. */
public record ReservationCreateRequest(
	@NotNull ServiceCategory serviceCategory,
	@NotNull @Future LocalDateTime scheduledAt,
	@NotBlank @Size(max = 200) String address,
	@NotBlank @Size(max = 30) String contactName,
	@NotBlank @Size(max = 20) String contactPhone,
	@Size(max = 2000) String requestNote
) {
}
```

## 3. 코드 한 줄씩

- `package`는 DTO 하위 패키지로 분류합니다.
- `LocalDateTime`은 일정 입력 타입이고 `ServiceCategory`는 예약 분류입니다.
- `Future`는 미래 시각, `NotBlank`는 null/빈칸/공백-only가 아닌 문자열, `NotNull`은 null 금지, `Size`는 길이 제한을 나타냅니다.
- 클래스 주석은 요청 용도와 입력 제약을 요약합니다.
- `public record`는 컴포넌트 목록으로 불변 데이터 전달 타입을 선언합니다. 컴파일러가 각 항목에 accessor와 생성자 등을 생성합니다.
- `serviceCategory`는 null 불가 enum입니다.
- `scheduledAt`은 null이 아니고 미래여야 합니다. 두 어노테이션은 각각 별도 제약입니다.
- `address`, `contactName`, `contactPhone`은 공백-only 불가이며 최대 길이는 각각 200, 30, 20입니다.
- `requestNote`는 선택 항목이고 최대 길이 2000입니다. null도 허용됩니다.
- record 본문은 비어 있으며 명시적 메서드가 없습니다.

## 4. 메서드 분석

이 파일은 명시적 메서드를 작성하지 않습니다. `record`가 데이터 컴포넌트 accessor(예: `serviceCategory()`, `scheduledAt()`)와 생성자를 자동 생성합니다. 외부 입력은 Spring이 JSON에서 record 생성자로 전달합니다. 유효성 오류는 이 타입의 자체 코드가 아니라 Controller의 `@Valid`와 Bean Validation 연동에서 발생합니다.

## 5. 실제 실행 흐름

회원 Controller의 `@RequestBody`가 HTTP 본문을 이 record로 변환하고 `@Valid`가 제약을 검사합니다. 검증 통과 시 `ReservationService.createReservation`이 record accessor를 통해 값을 읽고, 주소·연락처를 trim하고, 메모를 정규화해 엔티티를 생성합니다. Controller 위치는 [`ReservationController.java`](../../../src/main/java/com/cleaner/be/reservation/ReservationController.java)입니다.

## 6. 관계: 확인된 사실과 추론

- **코드로 확인됨:** 회원 생성 Controller가 요청 타입을 받고 서비스가 accessor로 데이터를 사용합니다.
- **코드로 확인됨:** 이 타입에는 상태값 필드가 없습니다. 새 예약의 초기 상태는 [`Reservation.java`](../../../src/main/java/com/cleaner/be/reservation/Reservation.java)에서 `PENDING`으로 정해집니다.
- **추론:** Validation 실패에 대한 응답 본문과 상태 코드는 현재 파일만으로 특정할 수 없으며 Spring 설정/예외 처리 설정을 추가 확인해야 합니다.
- 관련: [`ServiceCategory.java`](../../../src/main/java/com/cleaner/be/reservation/ServiceCategory.java), [`ReservationResponse.java`](../../../src/main/java/com/cleaner/be/reservation/dto/ReservationResponse.java)

## 7. 이 파일에만 적용되는 개념

record는 데이터 운반 목적에 맞춘 Java 문법으로, 생성된 accessor 이름은 `getX()`가 아니라 컴포넌트 이름 그대로입니다. Bean Validation 어노테이션은 제약을 선언하며, 실제 실행을 위해 Controller에서 `@Valid`가 사용됩니다.

## 8. 예시

```json
{
  "serviceCategory": "MATTRESS",
  "scheduledAt": "2030-05-20T10:00:00",
  "address": "서울시 예시 주소",
  "contactName": "홍길동",
  "contactPhone": "01012345678",
  "requestNote": null
}
```

DTO 제약에 맞춘 형식 예입니다. `scheduledAt`은 실행 시점보다 미래여야 하며, 서버 JSON 날짜 변환 설정 및 지원하는 enum 값에 맞춰야 합니다.

## 9. 요약

예약 생성 입력 필드를 record로 정의하고 null·공백·미래 날짜·길이 제한을 선언합니다. 인증 회원 연결과 저장은 서비스가 담당합니다.
