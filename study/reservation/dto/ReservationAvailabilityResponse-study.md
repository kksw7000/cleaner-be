# ReservationAvailabilityResponse 학습 가이드

## 1. 역할·레이어·왜·언제

`ReservationAvailabilityResponse`는 예약 달력에서 날짜와 선택 가능 여부만 공개하는 응답 DTO입니다. 예약자 이름·주소·연락처를 달력에 노출하지 않고 날짜별 상태를 전달합니다.

## 2. 현재 원본 코드

```java
package com.cleaner.be.reservation.dto;

import java.time.LocalDate;

/** 예약 달력이 개인정보 없이 날짜별 선택 가능 여부만 표시하도록 전달하는 응답입니다. */
public record ReservationAvailabilityResponse(
	LocalDate date,
	boolean available
) {
}
```

## 3. 구성과 문법

- `LocalDate`는 시각이 아니라 달력 날짜를 표현합니다.
- `date`는 응답 대상 날짜이고, `available`은 그 날짜에 예약이 가능한지를 나타냅니다.
- `record`는 컴포넌트 선언으로 불변 데이터 전달 객체를 간결하게 정의하며 `date()`와 `available()` 접근자를 제공합니다.

## 4. 생성과 값의 의미

서비스가 해당 월의 날짜를 순회하면서 날짜별 예약 존재 여부를 확인해 DTO를 생성합니다. 예약된 날짜는 `available=false`, 예약되지 않은 날짜는 `available=true`입니다. DTO 자체에는 날짜 검증이나 예약 조회 로직이 없습니다.

## 5. 실제 실행 흐름

[`ReservationController`](../../../src/main/java/com/cleaner/be/reservation/ReservationController.java)의 월별 가용성 API가 서비스 호출 결과를 반환합니다. [`ReservationService`](../../../src/main/java/com/cleaner/be/reservation/ReservationService.java)는 예약 날짜만 조회해 해당 월의 모든 날짜를 응답 목록으로 만들고, Spring은 record 값을 JSON으로 직렬화합니다.

## 6. 관계: 확인된 사실과 추론

- **코드로 확인됨:** 월별 가용성 응답에 `date`, `available` 두 값만 포함합니다.
- **코드로 확인됨:** 서비스가 DTO를 만들고 Controller가 목록을 반환합니다.
- **추론:** JSON 속성 이름과 날짜 문자열 직렬화 형식은 Spring/Jackson의 설정에 따릅니다.

## 7. 이 파일에만 적용되는 개념

Java `record`는 데이터 운반에 필요한 필드, 생성자, 접근자 등을 선언에서 제공하는 타입입니다. 이 DTO는 도메인 엔티티를 직접 반환하지 않아 예약 개인정보 노출을 제한합니다.

## 8. 예시

```json
{"date":"2030-05-20","available":false}
```

예약된 날을 나타내는 응답 모양입니다. 실제 목록에는 요청한 해당 월의 모든 날짜가 포함됩니다.

## 9. 요약

달력 화면용으로 날짜별 예약 가능 여부만 전달하는 불변 DTO이며, 예약 상세 및 회원 개인정보를 포함하지 않습니다.
