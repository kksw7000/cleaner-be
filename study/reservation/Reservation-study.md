# Reservation 학습 가이드

## 1. 역할·레이어·왜·언제

`Reservation`은 JPA 엔티티로서 예약 한 건의 서비스 종류, 일정, 방문 정보, 상태와 예약 회원 연관관계를 저장합니다. API 입출력 DTO와 DB 사이의 도메인 데이터 모델입니다.

## 2. 현재 원본 코드

```java
package com.cleaner.be.reservation;

import java.time.LocalDate;
import java.time.LocalDateTime;

import com.cleaner.be.auth.Member;
import com.cleaner.be.common.entity.BaseTimeEntity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

/** 회원의 청소 요청과 관리자가 처리할 상태를 보관하며, 예약자는 회원 엔티티로 연결합니다. */
@Entity
@Table(name = "reservations")
public class Reservation extends BaseTimeEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 30)
	private ServiceCategory serviceCategory;

	@Column(nullable = false)
	private LocalDateTime scheduledAt;

	@Column(nullable = false)
	private LocalDate scheduledDate;

	@Column(nullable = false, length = 200)
	private String address;

	@Column(nullable = false, length = 30)
	private String contactName;

	@Column(nullable = false, length = 20)
	private String contactPhone;

	@Column(columnDefinition = "TEXT")
	private String requestNote;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 20)
	private ReservationStatus status = ReservationStatus.PENDING;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	private Member member;

	protected Reservation() {
	}

	/** 생성 시에는 관리자가 아직 검토하지 않았으므로 모든 예약을 PENDING 상태로 시작합니다. */
	public Reservation(ServiceCategory serviceCategory, LocalDateTime scheduledAt, String address, String contactName,
		String contactPhone, String requestNote, Member member) {
		this.serviceCategory = serviceCategory;
		this.scheduledAt = scheduledAt;
		this.scheduledDate = scheduledAt.toLocalDate();
		this.address = address;
		this.contactName = contactName;
		this.contactPhone = contactPhone;
		this.requestNote = requestNote;
		this.member = member;
	}

	public Long getId() { return id; }
	public ServiceCategory getServiceCategory() { return serviceCategory; }
	public LocalDateTime getScheduledAt() { return scheduledAt; }
	public LocalDate getScheduledDate() { return scheduledDate; }
	public String getAddress() { return address; }
	public String getContactName() { return contactName; }
	public String getContactPhone() { return contactPhone; }
	public String getRequestNote() { return requestNote; }
	public ReservationStatus getStatus() { return status; }
	public Member getMember() { return member; }

	/** 관리자만 검증된 상태를 전달해 예약 진행 상태가 임의로 바뀌지 않도록 합니다. */
	public void updateStatus(ReservationStatus status) {
		this.status = status;
	}
}
```

## 3. 코드 한 줄씩

- 패키지 선언은 예약 도메인에 소속시킵니다.
- `LocalDate`와 `LocalDateTime`은 각각 날짜 단위 예약 제약 및 구체적인 예약 일정과 상위 클래스의 생성·수정 시각에 사용됩니다. `Member`, `BaseTimeEntity`는 예약자와 감사 시각을 재사용합니다.
- `jakarta.persistence` import들은 엔티티, 테이블, 기본키, 컬럼, enum 저장, 다대일 관계와 fetch 정책을 선언하는 JPA 표준 어노테이션/타입입니다.
- 클래스 주석은 예약 데이터를 보관하는 역할과 회원 연결을 설명합니다.
- `@Entity`는 JPA 관리 대상으로 표시하고, `@Table(name = "reservations")`는 테이블 이름을 지정합니다.
- `extends BaseTimeEntity`는 `createdAt`, `updatedAt` 필드와 getter를 상속합니다.
- `@Id`와 `@GeneratedValue(IDENTITY)`는 DB 생성 식별자 `id`를 선언합니다.
- `serviceCategory`는 문자열 enum이며 null을 허용하지 않고 길이를 30으로 제한합니다.
- `scheduledAt`은 null 불가 일정입니다. DB 컬럼 설정 외에 이 클래스가 미래 날짜인지 검사하지 않습니다.
- `scheduledDate`는 `scheduledAt`에서 파생한 날짜 전용 컬럼으로, 날짜 기준 중복 예약을 확인하고 달력 가용성을 조회하는 데 사용됩니다.
- `address`, `contactName`, `contactPhone`은 각각 방문 주소, 연락 이름, 전화번호 문자열 컬럼이며 null 불가와 길이 제한이 있습니다.
- `requestNote`는 선택 입력이고 DB `TEXT` 컬럼 정의를 사용합니다.
- `status`는 문자열 enum이며 기본값은 `PENDING`, null 불가입니다.
- `member`는 필수 `ManyToOne` 회원 관계입니다. `LAZY`는 실제 필요 시 연관 객체를 조회하도록 하는 fetch 설정입니다.
- `protected Reservation()`은 JPA가 엔티티를 복원할 때 사용할 생성자입니다.
- 공개 생성자는 모든 예약 필드를 인자로 받아 저장하며, 예약 날짜도 예약 시각에서 계산해 함께 보관하고 상태는 기본 초기값을 유지합니다.
- getter들은 ID, 예약 입력, 예약 날짜, 상태, 회원 연관을 반환합니다. 입력을 받지 않고 예외를 직접 던지지 않습니다.
- `updateStatus`는 새 상태를 받아 `status`에 대입하며 반환값은 없습니다. 상태 전이 제한 로직은 없습니다.

## 4. 메서드 분석

- `Reservation(...)`: 입력은 카테고리, 일정, 주소, 이름, 전화번호, 선택 메모, 회원입니다. `scheduledAt.toLocalDate()`로 날짜 컬럼 값을 파생해 저장하고 `PENDING` 기본값은 바꾸지 않습니다. `scheduledAt`이 null이면 날짜 추출 시 예외가 발생하므로 입력 DTO 검증이 선행되어야 합니다.
- 각 `get...()` 메서드: 대응 필드 하나를 반환합니다. 입력과 추가 호출, 명시적 예외가 없습니다.
- `updateStatus(status)`: 상태를 입력받아 필드에 대입하고 반환값은 `void`입니다. null 검사나 상태 전이 예외를 수행하지 않습니다.
- `protected Reservation()`: JPA용 기본 생성자이며 입력/반환/업무 처리가 없습니다.

## 5. 실제 실행 흐름

서비스 생성 흐름에서 [`ReservationService.createReservation`](../../src/main/java/com/cleaner/be/reservation/ReservationService.java)은 회원과 날짜 중복을 확인한 뒤 공개 생성자로 엔티티를 만들고 `saveAndFlush`에 전달합니다. 엔티티는 예약 시각에서 날짜를 파생하며, DB migration의 unique constraint가 동시 요청 중복도 차단합니다. 관리자 상태 변경은 서비스가 조회한 엔티티에서 `updateStatus`를 부르고 트랜잭션 커밋 때 변경을 반영합니다. 코드에는 응답 직전 `flush()`도 있습니다. 정확한 SQL 실행 시점은 JPA 구현과 설정의 영향을 받습니다.

## 6. 관계: 확인된 사실과 추론

- **코드로 확인됨:** `Member`와 필수 다대일 관계, `BaseTimeEntity` 상속, `ReservationRepository`의 JPA 엔티티 대상입니다.
- **코드로 확인됨:** 생성과 응답 변환은 [`ReservationService.java`](../../src/main/java/com/cleaner/be/reservation/ReservationService.java)가 담당합니다.
- **코드로 확인됨:** `scheduledDate`는 예약 시각에서 계산하며 달력 조회와 날짜 고유 제약에 사용됩니다.
- **코드로 확인됨:** 상위 클래스는 [BaseTimeEntity.java](../../src/main/java/com/cleaner/be/common/entity/BaseTimeEntity.java), 예약자 모델은 [Member.java](../../src/main/java/com/cleaner/be/auth/Member.java)입니다.
- **추론:** 실제 생성된 테이블의 모든 DDL 세부사항은 DB migration 및 JPA 설정을 보지 않고 확정할 수 없습니다.

## 7. 이 파일에만 적용되는 개념

엔티티는 DB 영속 상태와 연결된 객체입니다. 필드 변경은 트랜잭션의 변경 감지 대상이 됩니다. `LAZY` 연관은 응답 변환 시 회원 이름/ID를 읽을 때 로딩을 유발할 수 있어 repository의 entity graph와 서비스 트랜잭션이 중요합니다.

## 8. 예시

```java
Reservation reservation = new Reservation(
	ServiceCategory.MATTRESS, scheduledAt, "서울시 예시 주소", "김예시",
	"01012345678", null, member);
```

이 코드는 생성자의 매개변수 순서 예시입니다. `scheduledAt`과 `member`는 미리 유효한 값으로 준비되어야 하며, 이 파일 자체는 입력 검증을 하지 않습니다.

## 9. 요약

예약 데이터의 영속 엔티티이며 상태 기본값을 `PENDING`으로 두고 예약 시각과 날짜 키, 회원 및 생성·수정 시각을 연결합니다. API 검증과 권한 판단은 DTO·서비스 계층이 맡습니다.
