# ReservationResponse 학습 가이드

## 1. 역할·레이어·왜·언제

`ReservationResponse`는 예약 생성·목록·관리자 작업 결과를 API에서 표현하는 응답 DTO입니다. JPA 엔티티를 직접 노출하지 않고 필요한 필드만 전달합니다.

## 2. 현재 원본 코드

```java
package com.cleaner.be.reservation.dto;

import java.time.LocalDateTime;

import com.cleaner.be.reservation.ReservationStatus;
import com.cleaner.be.reservation.ServiceCategory;

/** 회원 예약 내역과 관리자 처리 화면이 공통으로 사용하는 예약 상세 응답입니다. */
public record ReservationResponse(
	Long id,
	ServiceCategory serviceCategory,
	LocalDateTime scheduledAt,
	String address,
	String contactName,
	String contactPhone,
	String requestNote,
	ReservationStatus status,
	Long memberId,
	String memberName,
	LocalDateTime createdAt,
	LocalDateTime updatedAt
) {
}
```

## 3. 코드 한 줄씩

- `package`는 요청/응답 객체를 모으는 DTO 패키지입니다.
- `LocalDateTime`은 예약 일정, 생성 및 수정 시각에 사용됩니다.
- 두 enum import는 분류와 진행 상태를 나타냅니다.
- 클래스 주석은 회원 및 관리자 화면 공통 응답이라는 용도를 설명합니다.
- `public record`는 컴포넌트 기반 불변 데이터 전달 타입입니다.
- `id`는 예약 식별자입니다.
- `serviceCategory`, `scheduledAt`은 선택 서비스와 예정 시각입니다.
- `address`, `contactName`, `contactPhone`, `requestNote`는 방문/연락/요청 상세입니다.
- `status`는 현재 상태입니다.
- `memberId`, `memberName`은 예약자에 관해 응답에 포함하는 값입니다.
- `createdAt`, `updatedAt`은 상위 감사 엔티티에서 오는 생성/수정 시간입니다.
- record 본문이 비어 있어 별도 메서드를 선언하지 않습니다.

## 4. 메서드 분석

명시적 메서드는 없습니다. Java record가 canonical constructor, 각 컴포넌트 accessor 및 표준 `equals`/`hashCode`/`toString`을 제공합니다. 서비스는 `new ReservationResponse(...)`로 엔티티 데이터를 전달하며 반환 타입으로 사용합니다. DTO 자체에서 입력 검증, DB 조회, 예외 처리는 하지 않습니다.

## 5. 실제 실행 흐름

서비스의 `toResponse`가 엔티티 getter와 회원 getter 및 감사 시각 getter를 호출해 컴포넌트 순서로 새 record를 만듭니다. Controller가 이를 HTTP 응답 본문으로 반환합니다. 관리자 목록은 이 DTO들의 목록을 페이지 DTO `content`에 넣습니다. 응답은 엔티티 객체 자체가 아닙니다.

## 6. 관계: 확인된 사실과 추론

- **코드로 확인됨:** 매핑 메서드는 [`ReservationService.java`](../../../src/main/java/com/cleaner/be/reservation/ReservationService.java)의 `toResponse`입니다.
- **코드로 확인됨:** Controller 및 페이지 응답이 이 타입을 반환/포함합니다.
- **코드로 확인됨:** `memberId`와 `memberName`을 포함해 예약자 요약을 제공합니다.
- **추론:** 모든 화면에 모든 필드가 필요한지 여부는 프론트 사용처를 확인해야 판단할 수 있습니다.
- 관련: [`Reservation.java`](../../../src/main/java/com/cleaner/be/reservation/Reservation.java), [`ReservationPageResponse.java`](../../../src/main/java/com/cleaner/be/reservation/dto/ReservationPageResponse.java)

## 7. 이 파일에만 적용되는 개념

응답 DTO는 영속성 엔티티와 외부 API 계약을 분리합니다. record의 구성요소는 기본적으로 final 성격의 상태를 가지므로 생성 이후 값을 바꾸는 setter가 없습니다. 필드명은 JSON 직렬화 프로퍼티로 쓰이는 것이 일반적이지만 최종 JSON은 serializer 설정의 영향을 받습니다.

## 8. 예시

```java
ReservationResponse response = new ReservationResponse(
	42L, ServiceCategory.MATTRESS, scheduledAt, "서울시 예시 주소",
	"홍길동", "01012345678", null, ReservationStatus.PENDING,
	7L, "김회원", createdAt, updatedAt);
```

컴포넌트 순서를 보여주는 예입니다. 실제 시각과 ID는 저장된 엔티티에서 서비스가 가져옵니다.

## 9. 요약

예약 한 건을 외부에 전달하는 공통 응답 record입니다. 엔티티 내부 구조 대신 필요한 예약·회원·감사 필드만 노출합니다.
