# ReservationPageResponse 학습 가이드

## 1. 역할·레이어·왜·언제

`ReservationPageResponse`는 관리자 예약 목록과 페이지 메타데이터를 한 번에 반환하는 응답 DTO입니다. Spring Data `Page` 객체를 API에 그대로 노출하지 않고 필요한 정보만 구성합니다.

## 2. 현재 원본 코드

```java
package com.cleaner.be.reservation.dto;

import java.util.List;

/** Spring Data Page를 직접 노출하지 않고 관리자 예약 목록의 페이지 정보를 함께 전달합니다. */
public record ReservationPageResponse(
	List<ReservationResponse> content,
	int page,
	int size,
	long totalElements,
	int totalPages,
	boolean first,
	boolean last
) {
}
```

## 3. 코드 한 줄씩

- `package`는 응답 DTO 패키지를 지정합니다.
- `List`는 이 페이지에서 보여줄 여러 예약 응답을 표현합니다.
- 클래스 주석은 Spring Data 타입과 API 응답을 분리하는 목적입니다.
- `public record`는 컴포넌트를 중심으로 하는 불변 데이터 타입 선언입니다.
- `content`는 현재 페이지의 `ReservationResponse` 목록입니다.
- `page`는 현재 페이지 번호, `size`는 페이지당 요청 크기입니다.
- `totalElements`는 전체 항목 수, `totalPages`는 전체 페이지 수입니다.
- `first`, `last`는 현재 페이지가 첫/마지막인지 나타냅니다.
- 본문은 비어 있고 명시 메서드는 없습니다.

## 4. 메서드 분석

명시적으로 선언한 메서드가 없습니다. record가 생성자와 컴포넌트 accessor를 자동 생성합니다. 값은 서비스가 Spring `Page<Reservation>`에서 가져와 생성자에 넣습니다. DTO에서 조회나 검증, 예외 발생은 하지 않습니다.

## 5. 실제 실행 흐름

관리자 서비스가 `Page<Reservation>`을 받은 다음 각 content 항목을 `ReservationResponse`로 변환합니다. 같은 `Page`의 현재 번호, 크기, 전체 건수, 페이지 수, 첫/마지막 플래그를 읽어 이 record를 만듭니다. Controller가 이 객체를 HTTP 본문으로 반환합니다.

## 6. 관계: 확인된 사실과 추론

- **코드로 확인됨:** 관리자 조회 서비스에서 생성됩니다.
- **코드로 확인됨:** `content` 타입은 [`ReservationResponse`](../../../src/main/java/com/cleaner/be/reservation/dto/ReservationResponse.java)입니다.
- **코드로 확인됨:** 관리자 조회 HTTP endpoint는 [`AdminReservationController.java`](../../../src/main/java/com/cleaner/be/admin/AdminReservationController.java)입니다.
- **추론:** 페이지 번호를 0부터 시작하는 계약은 Spring의 `PageRequest` 사용으로 확인되며, 프론트가 표시하는 번호 변환은 이 DTO에 포함되어 있지 않습니다.

## 7. 이 파일에만 적용되는 개념

페이지네이션은 전체 데이터를 한 응답에 싣지 않고 일부 항목과 전체 목록의 위치/크기 정보를 나눠 제공합니다. `long`인 `totalElements`는 항목 개수가 int 범위를 넘는 경우도 표현할 수 있습니다. `boolean` 필드는 첫/마지막 여부를 참/거짓으로 전달합니다.

## 8. 예시

```java
ReservationPageResponse response = new ReservationPageResponse(
	records, 0, 10, 24L, 3, true, false);
```

전체 24건 중 페이지 크기 10의 첫 번째(0번) 페이지 예시입니다. 실제 페이지 목록과 숫자는 조회 결과가 결정합니다.

## 9. 요약

예약 목록과 페이지 메타데이터를 함께 전달하는 record입니다. Spring Data 내부 `Page`를 외부 응답 전용 구조로 변환합니다.
