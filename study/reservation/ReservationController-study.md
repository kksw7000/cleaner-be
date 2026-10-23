# ReservationController 학습 가이드

## 1. 역할·레이어·왜·언제

`ReservationController`는 회원 예약 HTTP API의 프레젠테이션 계층입니다. JSON 요청을 DTO로 받고 서비스에 위임하며, SecurityContext에서 로그인 회원 ID를 읽어 서비스에 전달합니다.

## 2. 현재 원본 코드

```java
package com.cleaner.be.reservation;

import java.time.Year;
import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.cleaner.be.auth.InvalidCredentialsException;
import com.cleaner.be.reservation.ReservationYearOutOfRangeException;
import com.cleaner.be.reservation.dto.ReservationCreateRequest;
import com.cleaner.be.reservation.dto.ReservationAvailabilityResponse;
import com.cleaner.be.reservation.dto.ReservationResponse;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;

/** 회원 예약 HTTP 진입점으로, 연월 범위를 검증하고 JWT에서 읽은 회원 ID를 ReservationService에 전달합니다. */
@RestController
@Validated
@RequestMapping("/api/reservations")
public class ReservationController {

	private final ReservationService reservationService;

	public ReservationController(ReservationService reservationService) {
		this.reservationService = reservationService;
	}

	/** 검증된 예약 요청을 로그인 회원의 예약으로 생성하고, 관리자가 검토할 PENDING 상태를 반환합니다. */
	@PostMapping
	@ResponseStatus(HttpStatus.CREATED)
	public ReservationResponse createReservation(@Valid @RequestBody ReservationCreateRequest request) {
		return reservationService.createReservation(currentMemberId(), request);
	}

	/** 로그인 회원의 예약 내역만 조회해 다른 회원의 개인정보가 포함된 예약을 노출하지 않습니다. */
	@GetMapping
	public List<ReservationResponse> getMyReservations() {
		return reservationService.getMyReservations(currentMemberId());
	}

	/** 현재 연도 기준 ±3년과 월 1~12 범위의 달력 가용성을 반환해 잘못된 연월 조회를 HTTP 경계에서 차단합니다. */
	@GetMapping("/availability")
	public List<ReservationAvailabilityResponse> getMonthlyAvailability(
		@RequestParam int year,
		@RequestParam @Min(1) @Max(12) int month) {
		int currentYear = Year.now().getValue();
		int minimumYear = currentYear - 3;
		int maximumYear = currentYear + 3;
		if (year < minimumYear || year > maximumYear) {
			throw new ReservationYearOutOfRangeException(minimumYear, maximumYear);
		}
		return reservationService.getMonthlyAvailability(year, month);
	}

	/** 인증 필터가 저장한 회원 ID를 읽고, 숫자 principal이 없으면 인증 오류로 처리합니다. */
	private Long currentMemberId() {
		Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
		if (authentication == null || !(authentication.getPrincipal() instanceof Number memberId)) {
			throw new InvalidCredentialsException();
		}
		return memberId.longValue();
	}
}
```

## 3. 코드 한 줄씩

- `List`는 회원 예약 응답 목록입니다. Spring HTTP/security import들은 상태 코드, 인증 principal, 라우트 및 요청 처리에 사용됩니다.
- `InvalidCredentialsException`은 유효한 숫자 principal이 없을 때 사용합니다. DTO 타입들은 생성 요청과 예약 응답이며 `ReservationYearOutOfRangeException`은 동적 연도 범위 위반을 나타냅니다. `@Valid`는 요청 본문 Bean Validation에, `@Min`/`@Max`는 월 범위에 사용합니다.
- `@RestController`는 반환 객체를 HTTP 응답 본문으로 직렬화하고, `@Validated`는 메서드 파라미터 제약을 활성화하며 `@RequestMapping`은 기본 URL을 지정합니다.
- `final reservationService`는 서비스 의존성이고 생성자가 주입합니다.
- `createReservation`은 POST 경로이며 요청 본문을 `ReservationCreateRequest`로 변환 후 검증합니다. 성공 응답 상태는 201입니다. 현재 로그인 ID와 요청을 서비스에 넘기고 `ReservationResponse`를 반환합니다.
- `getMyReservations`는 GET 경로에서 로그인 ID의 예약 목록을 호출해 반환합니다.
- `getMonthlyAvailability`는 `/availability` 경로에서 연도를 현재 연도 기준 ±3년으로, 월을 1~12로 검증한 뒤 월의 날짜별 공개 가능 여부를 호출합니다. 인증한 회원 ID를 서비스에 넘기지는 않으며 개인정보 없이 날짜와 boolean 상태만 반환합니다. 범위 위반은 전역 예외 처리기가 400으로 변환합니다.
- `currentMemberId`는 SecurityContext에서 Authentication을 가져옵니다. `instanceof Number memberId` 패턴은 principal 타입 확인과 변수 바인딩을 동시에 합니다. 조건이 맞지 않으면 인증 예외, 맞으면 `longValue()`를 반환합니다.

## 4. 메서드 분석

- 생성자: `ReservationService`를 받아 필드에 할당합니다. 객체를 반환하며 추가 처리는 없습니다.
- `createReservation(request)`: HTTP 요청 본문을 인자로 받습니다. Spring이 역직렬화하고 `@Valid` 검증 후 실행합니다. `currentMemberId()`를 호출하고 서비스의 create를 호출하여 응답 DTO를 반환합니다. 성공 상태는 201이며 명시적 예외는 인증 ID 오류입니다. DTO 위반의 HTTP 처리 상세는 전역/프레임워크 설정에 좌우됩니다.
- `getMyReservations()`: 별도 인자는 없습니다. `currentMemberId` → service 조회 순서로 호출하고 DTO 목록을 반환합니다. 인증 ID가 없으면 `InvalidCredentialsException`입니다.
- `getMonthlyAvailability()`: `Year.now()`에서 현재 연도를 구해 허용 범위를 3년 전부터 3년 후까지 계산합니다. 연도가 그 범위를 벗어나면 `ReservationYearOutOfRangeException`을 던지고, 월은 `@Min`/`@Max`로 1~12를 검증합니다. 범위 내 입력은 기존 service 월별 조회에 전달해 응답 목록을 반환하고, 범위 밖 입력은 HTTP 400으로 처리합니다.
- `currentMemberId()`: Authentication을 조회합니다. null이거나 principal이 `Number`가 아니면 예외, 숫자면 long ID를 반환합니다. JWT 검증/ principal 생성 자체는 이 메서드에 구현되어 있지 않습니다.

## 5. 실제 실행 흐름

`POST /api/reservations` 요청에서 JSON을 DTO로 역직렬화하고 `@Valid`를 적용한 뒤, 메서드가 principal ID를 가져와 `ReservationService.createReservation`에 전달합니다. `GET /api/reservations`는 같은 ID로 회원 본인의 목록만 요청합니다. `GET /api/reservations/availability?year=2026&month=5`는 로그인 상태에서 현재 연도 기준 ±3년 이내 월의 날짜별 가용성을 반환하고, 범위 밖 연도 또는 1~12 밖 월은 HTTP 400을 반환합니다. 실제 인증 필터가 principal을 채우는 세부 단계는 이 파일에 없으며, 파일 주석과 SecurityContext 사용으로 인증 컨텍스트를 읽는 점이 확인됩니다.

## 6. 관계: 확인된 사실과 추론

- **코드로 확인됨:** 서비스에 의존하며 DTO를 주고받습니다.
- **코드로 확인됨:** POST/GET 모두 같은 `/api/reservations` 경로를 사용합니다.
- **코드로 확인됨:** 인증 principal은 숫자 타입이어야 하며 그렇지 않으면 `InvalidCredentialsException`입니다.
- **코드로 확인됨:** 달력 조회는 현재 연도 기준 과거 3년부터 미래 3년 및 월 1~12를 허용하고, 범위 밖 입력은 400으로 반환합니다.
- **관련 관리자 경로:** [`AdminReservationController.java`](../../src/main/java/com/cleaner/be/admin/AdminReservationController.java)는 `/admin/reservations`에서 관리자 목록과 상태변경을 다룹니다.
- **추론:** JWT 토큰의 claim 이름 및 인증 필터 구현은 이 파일만으로 확정할 수 없습니다.

## 7. 이 파일에만 적용되는 개념

Controller는 HTTP 요청·응답의 경계입니다. `@RequestBody`는 본문 역직렬화, `@Valid`는 DTO 제약조건 평가, `@Validated`는 메서드 매개변수 제약 검증을 활성화하고 `@Min`/`@Max`는 월 수치 범위를 선언합니다. 현재 연도에 따라 달라지는 연도 범위는 `Year.now()`로 계산해 도메인 예외로 검증합니다. 월 제약 위반과 연도 예외는 전역 처리기에서 400 응답으로 변환됩니다. Java 패턴 매칭 `instanceof`는 타입 검사와 지역 변수 선언을 결합합니다.

## 8. 예시

```http
POST /api/reservations
Content-Type: application/json

{"serviceCategory":"MATTRESS","scheduledAt":"2030-05-20T10:00:00","address":"예시 주소","contactName":"홍길동","contactPhone":"01012345678","requestNote":""}
```

DTO 제약조건을 만족하는 형식을 보여주는 예시이며, 서버가 동작하는 날짜와 실제 enum/API 설정에 따라 값은 달라질 수 있습니다. 로그인 인증이 있어야 정상적인 회원 ID를 얻습니다.

## 9. 요약

회원 예약 생성·목록 및 월별 가용성 API를 제공하며 인증된 회원 ID를 필요한 서비스 호출에 전달합니다. Controller가 예약 데이터 접근과 저장을 직접 하지는 않습니다.
