# ReservationService 학습 가이드

## 1. 역할·레이어·왜·언제

`ReservationService`는 예약 생성·회원 조회·관리자 페이지 조회·관리자 상태 변경의 비즈니스 계층입니다. Controller가 요청/인증 정보를 받고 이 서비스에 위임하며, 서비스는 회원·예약 조회, 권한 확인, 트랜잭션과 DTO 변환을 처리합니다.

## 2. 현재 원본 코드

```java
package com.cleaner.be.reservation;

import java.time.LocalDate;
import java.util.List;
import java.util.Set;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.dao.DataIntegrityViolationException;
import org.hibernate.exception.ConstraintViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.cleaner.be.auth.Member;
import com.cleaner.be.auth.MemberRepository;
import com.cleaner.be.auth.Role;
import com.cleaner.be.reservation.dto.ReservationCreateRequest;
import com.cleaner.be.reservation.dto.ReservationAvailabilityResponse;
import com.cleaner.be.reservation.dto.ReservationPageResponse;
import com.cleaner.be.reservation.dto.ReservationResponse;

/** 예약 생성·조회·상태 변경 규칙을 처리해 Controller가 인증 정보와 DB 처리 세부 사항을 갖지 않게 합니다. */
@Service
public class ReservationService {

	private static final String SCHEDULED_DATE_UNIQUE_CONSTRAINT = "uk_reservations_scheduled_date";

	private final ReservationRepository reservationRepository;
	private final MemberRepository memberRepository;

	public ReservationService(ReservationRepository reservationRepository, MemberRepository memberRepository) {
		this.reservationRepository = reservationRepository;
		this.memberRepository = memberRepository;
	}

	/** 로그인 회원을 예약자로 연결해 요청을 저장하고, 공백만 있는 문자열은 정리해 데이터 품질을 유지합니다. */
	@Transactional
	public ReservationResponse createReservation(Long memberId, ReservationCreateRequest request) {
		// 1. 예약자 조회 → 2. 입력 정리 및 예약 저장 → 3. 응답 변환 순서로 처리합니다.
		Member member = findMember(memberId);
		LocalDate scheduledDate = request.scheduledAt().toLocalDate();
		// 달력 조회 뒤 다른 회원이 먼저 예약했을 수 있으므로 저장 전에 같은 날짜의 예약을 다시 확인합니다.
		if (reservationRepository.existsByScheduledDate(scheduledDate)) {
			throw new ReservationDateUnavailableException();
		}
		try {
			Reservation reservation = reservationRepository.saveAndFlush(new Reservation(request.serviceCategory(), request.scheduledAt(),
				request.address().trim(), request.contactName().trim(), request.contactPhone().trim(), normalizeNote(request.requestNote()), member));
			return toResponse(reservation);
		} catch (DataIntegrityViolationException exception) {
			// 날짜 고유 제약 위반만 예약 충돌로 바꾸고, 회원 FK 등 다른 데이터 오류는 원래 원인을 유지합니다.
			if (isScheduledDateUniqueConstraintViolation(exception)) {
				throw new ReservationDateUnavailableException();
			}
			throw exception;
		}
	}

	/** 예외 원인 체인에서 날짜 고유 제약 위반만 찾아 동시 예약 충돌인지 구분합니다. */
	private boolean isScheduledDateUniqueConstraintViolation(DataIntegrityViolationException exception) {
		Throwable cause = exception;
		while (cause != null) {
			if (cause instanceof ConstraintViolationException constraintViolation
				&& SCHEDULED_DATE_UNIQUE_CONSTRAINT.equals(constraintViolation.getConstraintName())) {
				return true;
			}
			cause = cause.getCause();
		}
		return false;
	}

	/** 선택 가능한 날짜를 달력에 표시하도록 해당 월의 모든 날짜와 예약 여부를 함께 반환합니다. */
	@Transactional(readOnly = true)
	public List<ReservationAvailabilityResponse> getMonthlyAvailability(int year, int month) {
		LocalDate firstDate = LocalDate.of(year, month, 1);
		LocalDate lastDate = firstDate.withDayOfMonth(firstDate.lengthOfMonth());
		Set<LocalDate> occupiedDates = Set.copyOf(reservationRepository.findOccupiedDatesBetween(firstDate, lastDate));
		return firstDate.datesUntil(lastDate.plusDays(1))
			.map(date -> new ReservationAvailabilityResponse(date, !occupiedDates.contains(date)))
			.toList();
	}

	/** 로그인 회원의 예약만 반환해 회원 간 연락처와 주소가 노출되지 않도록 합니다. */
	@Transactional(readOnly = true)
	public List<ReservationResponse> getMyReservations(Long memberId) {
		return reservationRepository.findByMemberIdOrderByCreatedAtDesc(memberId).stream().map(this::toResponse).toList();
	}

	/** 관리자만 예약을 페이지 단위로 조회할 수 있게 역할을 다시 검증해 대량 예약을 한 번에 읽지 않도록 합니다. */
	@Transactional(readOnly = true)
	public ReservationPageResponse getReservationsByAdmin(Long adminId, int page, int size) {
		// 관리자 확인 → 최신순 페이지 조회 → API 페이지 응답 변환 순서로 처리합니다.
		validateAdmin(findMember(adminId));
		Page<Reservation> reservations = reservationRepository.findAllByOrderByCreatedAtDesc(
			PageRequest.of(page, size, Sort.by(Sort.Order.desc("createdAt"))));
		return new ReservationPageResponse(reservations.map(this::toResponse).getContent(), reservations.getNumber(),
			reservations.getSize(), reservations.getTotalElements(), reservations.getTotalPages(), reservations.isFirst(), reservations.isLast());
	}

	/** 관리자 권한을 확인한 뒤 예약 상태만 변경해 회원이 승인·완료 처리를 임의로 수행하지 못하게 합니다. */
	@Transactional
	public ReservationResponse updateStatusByAdmin(Long reservationId, Long adminId, ReservationStatus status) {
		// 관리자 확인 → 예약 조회 → 상태 변경 및 감사 시각 반영 → 응답 변환 순서로 처리합니다.
		validateAdmin(findMember(adminId));
		Reservation reservation = findReservation(reservationId);
		reservation.updateStatus(status);
		// 응답 전에 변경 감지를 실행해 AuditingEntityListener가 기록한 수정 시각을 포함합니다.
		reservationRepository.flush();
		return toResponse(reservation);
	}

	/** 잘못된 인증 ID는 예약 작업에 사용할 수 없으므로 권한 예외로 변환합니다. */
	private Member findMember(Long memberId) {
		return memberRepository.findById(memberId).orElseThrow(ReservationAccessDeniedException::new);
	}

	/** 존재하지 않는 예약 ID를 예약 전용 예외로 변환해 호출자가 원인을 구분하게 합니다. */
	private Reservation findReservation(Long reservationId) {
		return reservationRepository.findById(reservationId).orElseThrow(ReservationNotFoundException::new);
	}

	/** ADMIN 역할이 아닌 회원의 처리 요청을 거부해 URL 보안 설정 외에도 도메인 규칙을 유지합니다. */
	private void validateAdmin(Member member) {
		if (member.getRole() != Role.ADMIN) {
			throw new ReservationAccessDeniedException();
		}
	}

	/** 선택 입력인 요청 사항은 빈 문자열 대신 null로 정규화해 저장 의미를 일관되게 만듭니다. */
	private String normalizeNote(String requestNote) {
		if (requestNote == null || requestNote.isBlank()) {
			return null;
		}
		return requestNote.trim();
	}

	/** 엔티티와 회원 정보를 API 응답으로 옮겨 JPA 프록시와 내부 구조가 외부에 노출되지 않게 합니다. */
	private ReservationResponse toResponse(Reservation reservation) {
		return new ReservationResponse(reservation.getId(), reservation.getServiceCategory(), reservation.getScheduledAt(),
			reservation.getAddress(), reservation.getContactName(), reservation.getContactPhone(), reservation.getRequestNote(),
			reservation.getStatus(), reservation.getMember().getId(), reservation.getMember().getName(), reservation.getCreatedAt(),
			reservation.getUpdatedAt());
	}
}
```

## 3. 코드 한 줄씩

- `LocalDate`와 `Set`은 날짜별 예약 여부를 계산하고, `List`와 Spring `Page`, `PageRequest`, `Sort`는 예약 목록과 관리자 페이지에 사용됩니다. Spring `DataIntegrityViolationException`과 Hibernate `ConstraintViolationException`은 DB 무결성 오류 및 제약 이름을 구분하며 `@Transactional`은 DB 작업 경계입니다.
- `Member`, `MemberRepository`, `Role`은 요청자를 조회하고 관리자 역할을 확인합니다. DTO 타입들은 생성 입력, 날짜 가용성 및 예약 응답 형태입니다.
- 클래스 주석과 `@Service`는 비즈니스 서비스 계층임을 표시합니다.
- 두 `final` 필드는 예약 repository와 회원 repository 의존성입니다. 생성자는 Spring이 주입한 값을 필드에 보관합니다.
- `createReservation`: 쓰기 트랜잭션에서 회원과 날짜 중복을 먼저 확인하고 입력을 정리해 저장합니다. DB의 날짜 unique 제약 위반만 예약 불가 예외로 바꾸며, 다른 무결성 오류는 원래 예외로 전파합니다.
- `isScheduledDateUniqueConstraintViolation`: 예외 원인 체인을 순회해 Hibernate 제약 이름이 `uk_reservations_scheduled_date`와 정확히 일치하는 경우에만 true를 반환합니다. 이름이 다르거나 확인할 수 없으면 false입니다.
- `getMonthlyAvailability`: 조회 전용 트랜잭션에서 해당 월의 예약 날짜만 가져와 달의 모든 날짜를 가용 여부 응답으로 변환합니다.
- `getMyReservations`: 읽기 전용 트랜잭션에서 해당 회원 예약을 최신순 조회, stream 변환해 DTO 목록을 반환합니다.
- `getReservationsByAdmin`: 관리자 검증 후 `PageRequest.of(page,size,정렬)`로 최신 생성순 페이지를 조회하고 DTO 내용 및 페이지 정보를 반환합니다.
- `updateStatusByAdmin`: 관리자와 예약을 확인하고 상태를 바꾸며, `flush` 후 응답 DTO를 반환합니다.
- `findMember`: `MemberRepository.findById`의 `Optional`이 비어 있으면 `ReservationAccessDeniedException`을 던집니다.
- `findReservation`: 예약 ID를 찾아 없으면 `ReservationNotFoundException`을 던집니다.
- `validateAdmin`: `member.getRole()`이 `Role.ADMIN`이 아니면 권한 예외를 던집니다.
- `normalizeNote`: null/공백-only는 null로, 나머지는 양끝 공백 제거 결과로 반환합니다.
- `toResponse`: 엔티티와 회원, BaseTimeEntity의 값을 API DTO 생성자 순서에 맞춰 복사합니다.

## 4. 메서드 분석

- 생성자: 두 repository를 입력받아 저장합니다. 반환값은 객체이며 별도 호출/예외 로직은 없습니다.
- `createReservation(memberId, request)`: ① 회원 확인 ② 예약 시각에서 날짜를 추출해 기존 예약 확인 ③ 입력 문자열 정리 ④ `saveAndFlush` 및 DTO 변환. 날짜 제약 위반만 `ReservationDateUnavailableException`으로 바꿉니다. 제약 이름이 다른 `DataIntegrityViolationException`은 같은 인스턴스 그대로 다시 던져 회원 외래 키 등 다른 실패 원인을 보존합니다. 회원 ID가 없으면 권한 예외가 발생하며, `saveAndFlush`로 제약 위반을 이 메서드 안에서 확인합니다.
- `isScheduledDateUniqueConstraintViolation(exception)`: 원인 객체를 차례로 확인해 Hibernate `ConstraintViolationException`의 제약 이름이 예약 날짜 고유 제약과 같은지 boolean으로 반환합니다. 제약 이름이 없거나 일치하지 않으면 false입니다.
- `getMonthlyAvailability(year, month)`: `LocalDate.of`로 해당 월의 첫날을 만들고 말일을 계산한 뒤, 월 범위의 예약 날짜 집합을 조회합니다. 해당 월의 날짜를 빠짐없이 순회해 예약이 없는 날은 `available=true`, 예약된 날은 `false`인 DTO 목록을 반환합니다. 잘못된 연월은 `LocalDate`의 날짜 검증 예외가 발생하며, Controller 경로의 파라미터 범위 제약은 별도로 확인해야 합니다.
- `getMyReservations(memberId)`: repository 회원별 조회 → stream으로 각 엔티티를 `toResponse` 처리 → 불변 결과 목록 형태로 반환합니다. 반환은 DTO 목록이며 명시적 예외 처리는 없습니다.
- `getReservationsByAdmin(adminId,page,size)`: 회원 조회 → `validateAdmin` → 페이지 쿼리 → DTO 내용과 총 개수/페이지/첫·마지막 값을 반환합니다. 비관리자 또는 회원 미존재는 권한 예외입니다. 유효 범위는 Controller의 `@Min`/`@Max`에서 검증되며, 서비스 단독 호출은 페이지 입력을 검증하지 않습니다.
- `updateStatusByAdmin(reservationId,adminId,status)`: 관리자 확인 → 예약 조회 → 상태 필드 변경 → flush → 응답 변환. 관리자 오류, 예약 미존재 예외가 있고, 이 메서드 자체는 상태 전이/널을 추가 검증하지 않습니다.
- `findMember`, `findReservation`, `validateAdmin`, `normalizeNote`, `toResponse`: 각 흐름은 위 코드의 상세와 같습니다. private helper로 메서드 인자/실행 결과를 명확히 나누며, 예외는 두 조회·권한 검사에서 발생합니다.

## 5. 실제 실행 흐름

회원 POST는 [`ReservationController`](../../src/main/java/com/cleaner/be/reservation/ReservationController.java)가 principal ID와 검증된 요청 DTO를 전달합니다. 서비스는 회원 조회→날짜 중복 사전 확인→입력 정리 및 저장→응답 변환을 수행합니다. 동시 중복 예약은 DB 고유 제약으로 막고 [`GlobalExceptionHandler`](../../src/main/java/com/cleaner/be/auth/GlobalExceptionHandler.java)에서 409로 응답합니다. 달력 조회 API는 연월을 받아 예약된 날짜만 조회하고 월의 모든 날짜 상태를 반환합니다. 회원 GET은 principal ID로 조회 범위를 제한합니다. 관리자 GET/PATCH는 [`AdminReservationController`](../../src/main/java/com/cleaner/be/admin/AdminReservationController.java)에서 서비스 메서드를 호출하고, 서비스 안에서도 DB 회원 역할을 다시 확인합니다. 미존재/권한 예외는 전역 처리기에서 각각 404/403으로 응답됩니다.

## 6. 관계: 확인된 사실과 추론

- **코드로 확인됨:** `ReservationRepository`와 `MemberRepository`를 생성자로 주입받습니다.
- **코드로 확인됨:** 예약 생성은 저장 전 조회와 DB 고유 제약을 함께 사용하고, 월 가용성 조회는 예약 날짜만 읽습니다.
- **코드로 확인됨:** 관리자 권한은 DB의 `Member.getRole()`을 확인하며 `Role.ADMIN`만 통과합니다.
- **코드로 확인됨:** 쓰기 세 메서드는 `@Transactional`, 목록 조회 둘은 `readOnly = true`입니다.
- **코드로 확인됨:** 상태 변경 응답 전에 repository `flush()`를 호출합니다.
- **추론:** Auditing listener가 `flush` 시점에 수정 시각을 언제 기록하는지 구체적 동작은 Spring Data JPA 설정 및 트랜잭션에 의존합니다. 코드 주석은 응답 반영을 의도한다고 밝힙니다.

## 7. 이 파일에만 적용되는 개념

트랜잭션은 여러 DB 작업의 실행 경계를 묶고, 읽기 전용 표시는 조회 의도를 전달합니다. `Optional.orElseThrow`는 조회 결과가 비었을 때 도메인 예외를 발생시킵니다. Java stream `map`은 엔티티 각각을 DTO로 변환합니다. `flush()`는 영속성 컨텍스트 변경사항을 DB로 동기화하도록 요청하지만 트랜잭션 커밋과 동일한 의미는 아닙니다.

## 8. 예시

```java
ReservationResponse response =
	reservationService.createReservation(memberId, request);
```

Controller가 인증된 ID와 `@Valid` 검증 요청을 전달하는 형태입니다. 서비스가 성공하면 만들어진 예약의 응답 DTO가 반환됩니다.

## 9. 요약

예약의 핵심 업무 규칙을 수행합니다. 요청자 연결, 회원별 접근 범위, 관리자 역할 확인, 페이지 처리, 상태 변경, DTO 변환과 트랜잭션 경계가 한 곳에 모여 있습니다.

## 10. 하루 한 팀 정책 보완

현재 구현은 `Asia/Seoul`의 오늘 날짜보다 앞선 예약을 거부하며 오늘은 시각과 관계없이 허용합니다. 생성 사전 조회와 달력 월 조회는 `PENDING`, `APPROVED` 상태만 활성으로 계산합니다. PostgreSQL 부분 고유 인덱스 `uk_reservations_active_scheduled_date`가 같은 날짜의 동시 요청을 최종 차단합니다.

관리자 상태 변경도 비활성 예약을 `PENDING` 또는 `APPROVED`로 다시 바꾸기 전에 날짜 충돌을 검사합니다. 검사 이후 경쟁 요청이 먼저 활성화되는 경우에도 같은 인덱스 위반을 날짜 마감 HTTP 409로 변환합니다. `REJECTED`, `CANCELLED`를 새 상태로 지원하고 기존 API 호환을 위해 `CANCELED` 값은 유지합니다.
