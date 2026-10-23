# `BaseTimeEntity.java` 학습 문서

원본: [`BaseTimeEntity.java`](../../../src/main/java/com/cleaner/be/common/entity/BaseTimeEntity.java)

## 1. 파일의 역할

이 추상 클래스는 DB 엔티티가 공통으로 사용할 생성 시각(`createdAt`)과 마지막 수정 시각(`updatedAt`)을 제공합니다. 각 엔티티가 같은 필드를 반복 선언하지 않도록 공통 규칙을 모았습니다.

- **계층:** 공통 JPA 엔티티 기반 클래스
- **필요한 이유:** 게시글이나 예약처럼 생성·수정 시각이 필요한 데이터가 같은 방식으로 시각을 기록하기 때문입니다.
- **사용되는 상황:** 이 클래스를 상속하고 JPA로 저장·수정되는 엔티티에서 사용됩니다. 프로젝트에서는 [`Board`](../../../src/main/java/com/cleaner/be/board/Board.java), [`Comment`](../../../src/main/java/com/cleaner/be/board/Comment.java), [`Reservation`](../../../src/main/java/com/cleaner/be/reservation/Reservation.java)가 상속합니다.

## 2. 전체 코드

아래는 분석한 원본 파일 전체입니다.

```java
package com.cleaner.be.common.entity;

import java.time.LocalDateTime;

import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;

import jakarta.persistence.Column;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.MappedSuperclass;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

/** 생성·수정 시각이 필요한 도메인 엔티티가 동일한 감사 규칙을 재사용하도록 하는 기반 클래스입니다. */
@MappedSuperclass
@EntityListeners(AuditingEntityListener.class)
public abstract class BaseTimeEntity {

	@CreatedDate
	@Column(nullable = false, updatable = false)
	private LocalDateTime createdAt;

	@LastModifiedDate
	@Column(nullable = false)
	private LocalDateTime updatedAt;

	public LocalDateTime getCreatedAt() {
		return createdAt;
	}

	public LocalDateTime getUpdatedAt() {
		return updatedAt;
	}
}
```

## 3. 코드 한 줄씩 자세히 설명

- `package com.cleaner.be.common.entity;`: 공통 엔티티 클래스가 모이는 패키지를 선언합니다.
- `import java.time.LocalDateTime;`: 날짜와 시각을 함께 표현하는 Java 타입을 가져옵니다.
- `CreatedDate`, `LastModifiedDate`: 생성 시각과 마지막 수정 시각을 자동 기록 대상으로 지정하는 애너테이션입니다.
- `Column`: JPA 엔티티의 필드가 DB 컬럼과 어떻게 연결되는지 설정합니다.
- `EntityListeners`: 엔티티의 저장 이벤트를 감지하는 리스너를 연결합니다.
- `MappedSuperclass`: 이 클래스 자체를 별도 테이블로 만들지 않고, 상속하는 엔티티에 필드 매핑을 물려줍니다.
- `AuditingEntityListener`: Spring Data JPA가 제공하는 리스너로 생성·수정 시각 필드를 기록합니다.
- `@EntityListeners(AuditingEntityListener.class)`: 이 엔티티 매핑을 물려받은 객체의 저장 시점에 감사 리스너가 동작하도록 연결합니다.
- `public abstract class BaseTimeEntity`: 공개된 추상 클래스입니다. `abstract`는 이 클래스 자체를 직접 생성하는 대신 하위 클래스가 상속해 사용한다는 뜻입니다.
- `@CreatedDate private LocalDateTime createdAt;`: 처음 저장된 시각을 자동 기록하는 필드입니다.
- `@Column(nullable = false, updatable = false)`: DB 컬럼 값은 null일 수 없고, 엔티티가 수정될 때 이 컬럼을 업데이트하지 않도록 설정합니다. 생성 시각은 최초 저장 뒤 바뀌지 않습니다.
- `@LastModifiedDate private LocalDateTime updatedAt;`: 저장 또는 수정 때 마지막 변경 시각을 자동 기록하는 필드입니다.
- `@Column(nullable = false)`: 수정 시각을 DB에 null로 저장하지 않도록 설정합니다.
- 두 getter는 각 시각 필드 값을 돌려줍니다. `public`이므로 상속받은 엔티티를 사용하는 코드가 읽을 수 있지만, setter는 제공하지 않습니다.

## 4. 메서드별 실행 과정

### `getCreatedAt()`

1. **목적:** 엔티티가 처음 생성된 시각을 읽습니다.
2. **입력:** 없음.
3. **순서:** 현재 객체의 `createdAt` 필드를 읽습니다.
4. **다른 클래스 호출:** 내부 호출은 없습니다.
5. **반환:** `LocalDateTime` 값.
6. **예외:** 메서드 안에 명시적인 예외 처리는 없으며, 필드를 그대로 반환합니다.

### `getUpdatedAt()`

1. **목적:** 엔티티가 마지막으로 수정된 시각을 읽습니다.
2. **입력:** 없음.
3. **순서:** 현재 객체의 `updatedAt` 필드를 읽습니다.
4. **다른 클래스 호출:** 내부 호출은 없습니다.
5. **반환:** `LocalDateTime` 값.
6. **예외:** 메서드 안에 명시적인 예외 처리는 없으며, 필드를 그대로 반환합니다.

이 클래스에는 생성자나 필드 수정 메서드가 없습니다. 시각 값의 기록은 `AuditingEntityListener`가 JPA 저장 시점에 수행합니다. 이 자동 기록을 사용할 수 있도록 애플리케이션 시작 클래스인 [`BeApplication`](../../../src/main/java/com/cleaner/be/BeApplication.java)에 `@EnableJpaAuditing`이 있습니다.

## 5. 실제 실행 흐름

예약 생성 흐름에서 확인할 수 있는 연결은 다음과 같습니다.

```text
ReservationService.createReservation(...)
  → ReservationRepository.save(...)
  → JPA가 Reservation 엔티티를 저장
  → 상속한 BaseTimeEntity의 createdAt / updatedAt 기록
  → ReservationService가 createdAt / updatedAt을 응답 DTO에 전달
```

수정 때는 `updatedAt`이 갱신되는 용도로 사용됩니다. 이 클래스는 시간 기록을 직접 수행하지 않고, 필드와 JPA 매핑 규칙을 제공하며 실제 기록은 활성화된 감사 리스너가 담당합니다.

## 6. 다른 파일과의 관계

- [`BeApplication.java`](../../../src/main/java/com/cleaner/be/BeApplication.java)의 `@EnableJpaAuditing`이 Spring Data JPA 감사 기능을 켭니다.
- [`Board.java`](../../../src/main/java/com/cleaner/be/board/Board.java), [`Comment.java`](../../../src/main/java/com/cleaner/be/board/Comment.java), [`Reservation.java`](../../../src/main/java/com/cleaner/be/reservation/Reservation.java)는 이 클래스를 상속합니다.
- 이 클래스의 getter를 통해 Service가 응답 DTO를 만들 때 생성·수정 시각을 가져올 수 있습니다. 예를 들어 [`ReservationService`](../../../src/main/java/com/cleaner/be/reservation/ReservationService.java)는 두 값을 `ReservationResponse`에 전달합니다.
- 이 코드가 확인해 주는 것은 필드 매핑과 감사 리스너 연결입니다. 실제 DB 컬럼명이나 DB 테이블 정의는 별도 Flyway 마이그레이션에서 확인해야 합니다.

## 7. 핵심 개념 정리

- **상속:** 자식 클래스가 부모 클래스의 필드와 동작을 이어받는 Java 기능입니다.
- **추상 클래스:** 공통 구현을 제공하지만 그 자체로 직접 사용하지 않는 기반 클래스입니다.
- **JPA 매핑:** Java 필드와 DB 컬럼을 연결하는 규칙입니다.
- **Mapped superclass:** 부모 클래스의 매핑 필드를 자식 엔티티에 물려주며, 부모 자체 테이블은 만들지 않는 방식입니다.
- **엔티티 리스너:** 엔티티 저장 등 특정 시점에 자동 작업을 수행하는 객체입니다.
- **Auditing:** 데이터가 언제 만들어지고 변경됐는지 기록하는 기능입니다.
- **`updatable = false`:** 엔티티 수정 시 생성 시각 컬럼이 덮어써지지 않도록 합니다.

## 8. 예시로 이해하기

예약 엔티티를 처음 저장하면, 예약 생성 코드가 `createdAt`이나 `updatedAt`을 직접 입력하지 않아도 활성화된 JPA Auditing이 해당 시각을 채웁니다. 이후 Service는 다음처럼 getter로 값을 읽어 응답을 만들 수 있습니다.

```java
new ReservationResponse(
    reservation.getId(),
    // ...예약 정보...
    reservation.getCreatedAt(),
    reservation.getUpdatedAt()
);
```

위 코드는 사용 흐름을 설명하기 위한 축약 예시이며, 실제 응답 생성은 [`ReservationService.toResponse`](../../../src/main/java/com/cleaner/be/reservation/ReservationService.java)에서 전체 필드를 전달해 수행합니다.

## 9. 핵심 요약

- `BaseTimeEntity`는 생성·수정 시각 필드를 공통 제공하는 추상 기반 클래스입니다.
- `@MappedSuperclass`로 자식 엔티티에 필드 매핑을 물려줍니다.
- `@CreatedDate`, `@LastModifiedDate`와 `AuditingEntityListener`가 자동 시각 기록을 구성합니다.
- 실제 기록 기능의 전역 활성화는 [`BeApplication.java`](../../../src/main/java/com/cleaner/be/BeApplication.java)의 `@EnableJpaAuditing`에 달려 있습니다.
