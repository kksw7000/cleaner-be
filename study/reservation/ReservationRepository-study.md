# ReservationRepository 학습 가이드

## 1. 역할·레이어·왜·언제

`ReservationRepository`는 데이터 접근 레이어의 Spring Data JPA 인터페이스입니다. 예약 저장, ID 조회 등 기본 CRUD와 회원별 및 관리자 페이지 조회를 제공하여 서비스가 쿼리 구현 세부사항을 직접 작성하지 않게 합니다.

## 2. 현재 원본 코드

```java
package com.cleaner.be.reservation;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

/** 예약의 저장과 회원별·관리자용 조회를 제공하며, 응답에 필요한 회원 정보를 함께 읽습니다. */
public interface ReservationRepository extends JpaRepository<Reservation, Long> {

	/** 로그인 회원의 예약만 최신 생성순으로 찾아 다른 회원 예약 노출을 막습니다. */
	@EntityGraph(attributePaths = "member")
	List<Reservation> findByMemberIdOrderByCreatedAtDesc(Long memberId);

	/** 관리자가 처리 대상을 페이지 단위로 확인하도록 예약자 정보를 포함해 조회합니다. */
	@EntityGraph(attributePaths = "member")
	Page<Reservation> findAllByOrderByCreatedAtDesc(Pageable pageable);
}
```

## 3. 코드 한 줄씩

- `package`는 예약 도메인에 위치시킵니다.
- `List`, `Page`, `Pageable`은 여러 결과와 페이지 결과·요청을 표현합니다.
- `EntityGraph`는 연관 엔티티를 조회에 포함하는 JPA 기능이며, `JpaRepository`는 Spring Data의 CRUD 기반 인터페이스입니다.
- 클래스 주석은 저장과 조회 역할을 설명합니다.
- `JpaRepository<Reservation, Long>`의 타입 인자는 관리 엔티티와 기본키 타입입니다. 상속으로 `save`, `findById`, `flush` 등 기본 동작을 얻습니다.
- `@EntityGraph(attributePaths = "member")`는 조회 시 예약의 `member` 연관도 함께 가져오도록 지정합니다.
- `findByMemberIdOrderByCreatedAtDesc(Long memberId)`는 이름 규칙으로 member의 ID 조건과 createdAt 내림차순 정렬을 표현합니다. 반환은 `List<Reservation>`입니다.
- `findAllByOrderByCreatedAtDesc(Pageable pageable)`는 모든 예약을 createdAt 내림차순으로 페이지 조회합니다. 반환은 `Page<Reservation>`입니다.

## 4. 메서드 분석

두 메서드는 인터페이스 선언뿐이며 메서드 본문을 직접 구현하지 않습니다. Spring Data가 이름 규칙과 매개변수 타입으로 구현을 생성합니다.

- `findByMemberIdOrderByCreatedAtDesc`: 회원 ID를 입력받아 해당 회원 예약을 최신 생성순으로 반환합니다. 서비스에서 회원용 목록 조회에 호출합니다. 쿼리 결과가 없으면 빈 목록이며, 구체적인 DB 예외는 JPA 실행 환경에 따릅니다.
- `findAllByOrderByCreatedAtDesc`: 페이지 요청을 입력받아 최신순 예약의 한 페이지를 반환합니다. 서비스의 관리자 목록 조회가 호출합니다. 반환 `Page`에는 content와 전체 페이지 정보가 들어갑니다.

## 5. 실제 실행 흐름

회원 조회 서비스가 첫 메서드에 인증된 회원 ID를 넘기고 응답 DTO로 변환합니다. 관리자 조회 서비스는 `PageRequest`를 두 번째 메서드에 전달하고, 반환된 페이지 메타데이터를 페이지 DTO에 복사합니다. EntityGraph가 실제 SQL에서 어떤 JOIN 형태로 실행될지는 JPA 제공자에 따라 달라질 수 있습니다.

## 6. 관계: 확인된 사실과 추론

- **코드로 확인됨:** 엔티티는 [`Reservation.java`](../../src/main/java/com/cleaner/be/reservation/Reservation.java)이며, 서비스에서 두 쿼리를 호출합니다.
- **코드로 확인됨:** `JpaRepository` 상속으로 기본 저장·조회 메서드도 서비스가 이용합니다.
- **코드로 확인됨:** `member`가 응답 변환에 필요하므로 두 조회에 EntityGraph가 선언되어 있습니다.
- **추론:** 생성되는 SQL의 조인 순서와 인덱스 활용은 이 인터페이스만으로 확정할 수 없습니다.

## 7. 이 파일에만 적용되는 개념

Spring Data의 파생 쿼리 메서드는 메서드 이름에서 조건과 정렬을 유추합니다. `Pageable`/`Page`는 데이터 전체를 한 번에 반환하지 않고 페이지 단위로 접근하게 해줍니다. `EntityGraph`는 연관 객체 조회 전략의 선언입니다.

## 8. 예시

```java
Page<Reservation> page = reservationRepository.findAllByOrderByCreatedAtDesc(
	PageRequest.of(0, 10));
```

첫 페이지에서 최대 열 건을 가져오는 개념 예시입니다. 실제 서비스는 정렬을 포함한 `PageRequest`를 전달합니다.

## 9. 요약

예약 엔티티 저장과 두 가지 조회를 선언하는 JPA repository입니다. 회원별 최신순 목록 및 관리자 전체 최신순 페이지에 예약자 연관을 함께 조회합니다.
