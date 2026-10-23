# ServiceCategory 학습 가이드

## 1. 역할·레이어·왜·언제

`ServiceCategory`는 예약에 선택할 수 있는 청소 서비스 종류를 정의하는 도메인 enum입니다. 예약 엔티티와 생성 요청 DTO에서 같은 타입을 사용해 허용된 분류값을 일치시킵니다. 사용자가 예약 종류를 고르거나 저장된 예약 종류를 읽을 때 관련 파일을 함께 봅니다.

## 2. 현재 원본 코드

```java
package com.cleaner.be.reservation;

/** 프론트의 서비스 카테고리 코드와 예약 데이터가 같은 분류 기준을 사용하도록 정의합니다. */
public enum ServiceCategory {
	MATTRESS,
	SOFA_CARPET,
	MOVE_IN,
	APPLIANCE,
	CONSTRUCTION,
	COMMERCIAL,
	WAX_COATING,
	REGULAR_CLEANING,
	ESTATE_CLEANING
}
```

## 3. 코드 한 줄씩

- `package`는 이 enum의 소속 패키지를 지정합니다.
- 클래스 위 주석은 프론트와 예약 데이터가 분류 코드를 공유한다는 역할을 설명합니다.
- `public enum`은 외부 클래스도 사용할 수 있는 고정 선택지 타입을 선언합니다.
- 각 상수는 허용 서비스 코드입니다. `MATTRESS`는 매트리스, `SOFA_CARPET`은 소파·카펫, `MOVE_IN`은 입주, `APPLIANCE`는 가전, `CONSTRUCTION`은 공사, `COMMERCIAL`은 상업 공간, `WAX_COATING`은 왁스 코팅, `REGULAR_CLEANING`은 정기 청소, `ESTATE_CLEANING`은 유품 정리를 나타냅니다. 이 의미는 코드 이름을 바탕으로 한 해석입니다.
- 메서드나 필드는 선언되어 있지 않습니다.

## 4. 메서드 분석

이 enum은 메서드를 선언하지 않습니다. enum이 제공하는 각 상수는 입력·호출 순서가 없고, `ReservationCreateRequest`와 `Reservation`의 필드 타입으로 사용됩니다. 별도 예외 처리도 이 파일에는 없습니다.

## 5. 실제 실행 흐름

예약 생성 JSON의 `serviceCategory` 문자열이 요청 DTO의 `ServiceCategory`로 변환되고, 검증된 DTO 값을 서비스가 엔티티 생성자에 전달합니다. enum 값은 엔티티의 `serviceCategory` 필드에 들어갑니다. 요청 DTO의 `@NotNull` 검증과 엔티티의 문자열 enum 저장 선언은 각각 실제 코드로 확인됩니다. 프론트에서 실제 전송하는 모든 값과 DB 스키마의 실데이터는 이 파일만으로 확인할 수 없습니다.

## 6. 관계: 확인된 사실과 추론

- **코드로 확인됨:** `ReservationCreateRequest`와 `Reservation`이 이 타입을 import/사용하며, `ReservationResponse`에도 포함됩니다.
- **코드로 확인됨:** `Reservation`은 `@Enumerated(EnumType.STRING)`으로 분류를 문자열로 저장합니다.
- **추론:** 각 enum 상수의 사용자에게 보이는 한국어 라벨은 프론트 매핑을 확인해야 확정할 수 있습니다.
- 관련: [`Reservation.java`](../../src/main/java/com/cleaner/be/reservation/Reservation.java), [`ReservationCreateRequest.java`](../../src/main/java/com/cleaner/be/reservation/dto/ReservationCreateRequest.java), [`ReservationResponse.java`](../../src/main/java/com/cleaner/be/reservation/dto/ReservationResponse.java)

## 7. 이 파일에만 적용되는 개념

enum은 임의 문자열 대신 소스에 선언된 상수만 값으로 허용합니다. 새 분류를 추가하면 API 소비자와 데이터 호환성을 함께 점검해야 합니다. 문자열 저장은 enum 순서가 바뀌어도 숫자 ordinal 저장보다 안전하지만, 상수 이름을 변경하면 저장값과 호환성 문제가 생길 수 있습니다.

## 8. 예시

```java
ServiceCategory category = ServiceCategory.MATTRESS;
```

예약 요청에서 이 코드에 대응하는 값으로 `MATTRESS`를 보냈다고 가정한 예입니다. 실제 API JSON 표기나 프론트 라벨은 별도 매핑을 확인해야 합니다.

## 9. 요약

예약 서비스 종류를 제한하는 공통 enum입니다. 이 파일에는 아홉 가지 선택지만 있으며, 예약 DTO와 엔티티에서 분류 타입을 통일합니다.
