# AdminReservationController 학습 문서

## 1. 파일 역할·계층·필요한 이유·사용 시점

[`AdminReservationController.java`](../../src/main/java/com/cleaner/be/admin/AdminReservationController.java)는 관리자 예약 관리 HTTP API를 담당하는 Spring MVC Controller입니다. 공통 경로 `/admin/reservations`에서 예약 목록 조회와 상태 변경 요청을 받고, 예약 도메인 규칙은 [`reservation/ReservationService.java`](../../src/main/java/com/cleaner/be/reservation/ReservationService.java)에 위임합니다. API 요청/응답과 예약 처리 정책을 분리하기 위해 사용하며, 관리자 화면에서 예약을 조회하거나 상태를 갱신할 때 실행됩니다.

## 2. 전체 원본 코드

아래는 현재 `AdminReservationController.java`의 전체 내용입니다.

```java
package com.cleaner.be.admin;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.cleaner.be.auth.InvalidCredentialsException;
import com.cleaner.be.reservation.ReservationService;
import com.cleaner.be.reservation.dto.ReservationPageResponse;
import com.cleaner.be.reservation.dto.ReservationResponse;
import com.cleaner.be.reservation.dto.ReservationStatusUpdateRequest;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;

/** 관리자 예약 HTTP 진입점으로, SecurityConfig의 ADMIN 인가 후 예약 서비스에 처리 요청을 전달합니다. */
@RestController
@Validated
@RequestMapping("/admin/reservations")
public class AdminReservationController {

	private final ReservationService reservationService;

	public AdminReservationController(ReservationService reservationService) {
		this.reservationService = reservationService;
	}

	/** 관리자에게 최신 요청순 예약을 페이지 단위로 제공해 예약이 많아도 응답 크기를 제한합니다. */
	@GetMapping
	public ReservationPageResponse getReservations(@RequestParam(defaultValue = "0") @Min(0) int page,
		@RequestParam(defaultValue = "10") @Min(1) @Max(100) int size) {
		return reservationService.getReservationsByAdmin(currentMemberId(), page, size);
	}

	/** 관리자 요청의 상태 값을 특정 예약에 반영하고, 변경 후 예약 정보를 반환합니다. */
	@PatchMapping("/{id}/status")
	public ReservationResponse updateStatus(@PathVariable Long id,
		@Valid @RequestBody ReservationStatusUpdateRequest request) {
		return reservationService.updateStatusByAdmin(id, currentMemberId(), request.status());
	}

	/** 인증 필터가 저장한 관리자 ID를 읽고, 유효한 숫자 ID가 없으면 인증 오류로 처리합니다. */
	private Long currentMemberId() {
		Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
		if (authentication == null || !(authentication.getPrincipal() instanceof Number memberId)) {
			throw new InvalidCredentialsException();
		}
		return memberId.longValue();
	}
}
```

## 3. 줄 단위 구성과 Java/Spring 요소

- 1행은 패키지 선언이고, 3~22행은 SecurityContext, Spring 매핑, Reservation Service/DTO, 인증 예외, Bean Validation 관련 import입니다.
- 24~26행은 Controller, 메서드 검증, 공통 URL을 선언합니다. 따라서 두 메서드의 최종 경로는 `/admin/reservations`를 기준으로 조합됩니다.
- 27~33행은 `ReservationService` 필드와 생성자 주입입니다. `final`은 생성자 이후 참조가 바뀌지 않게 합니다.
- 35~40행은 GET 목록 API입니다. 기본 페이지는 0, 기본 크기는 10, 허용 크기는 1~100입니다. 호출 시 현재 인증 ID를 Service에 전달합니다.
- 42~47행은 예약 상태 변경 API입니다. URL의 `{id}`와 검증된 JSON 요청에서 상태를 꺼내 Service에 전달합니다.
- 49~56행은 SecurityContext에서 인증 principal을 가져오는 private 메서드입니다. null/숫자가 아닌 principal을 예외로 거부합니다.
- `@Valid`는 요청 DTO에 선언된 제약 조건을 적용합니다. 연결된 [`ReservationStatusUpdateRequest`](../../src/main/java/com/cleaner/be/reservation/dto/ReservationStatusUpdateRequest.java)는 상태가 null이 아닌지 검사합니다.
- `instanceof Number memberId`는 타입 검사 성공 시 해당 객체를 `memberId` 변수로 바인딩하는 Java 패턴 매칭 문법입니다.

## 4. 메서드별 목적·입력·처리 순서·호출·반환·예외

- 생성자(30~32행): 주입받은 예약 서비스를 필드에 보관합니다.
- `getReservations`(36~40행): 페이지와 크기를 입력받아 `currentMemberId()`를 먼저 평가하고, `getReservationsByAdmin(id, page, size)`를 호출합니다. 페이지 응답 DTO를 반환합니다. 입력 범위 밖이면 검증 오류, 유효한 관리자 ID가 없으면 `InvalidCredentialsException`, Service의 도메인 권한 검증 실패 시 `ReservationAccessDeniedException`이 발생할 수 있습니다.
- `updateStatus`(44~47행): 경로 예약 ID와 검증된 요청 DTO를 받습니다. 현재 ID를 추출하고 `request.status()`와 함께 `updateStatusByAdmin`을 호출해 `ReservationResponse`를 반환합니다. 요청 상태가 null이면 Bean Validation 단계에서 거부되고, 예약 미존재 또는 권한 오류는 도메인 예외가 발생할 수 있습니다.
- `currentMemberId`(51~57행): SecurityContext에서 인증 객체를 얻고 숫자 principal을 `Long`으로 바꿉니다. 인증 정보가 없거나 ID 타입이 맞지 않으면 `InvalidCredentialsException`을 던집니다.

## 5. 실제 실행 흐름

1. `/admin/reservations` 요청은 `SecurityConfig`의 `/admin/**` ADMIN 인가를 먼저 거칩니다.
2. Spring MVC는 쿼리 매개변수 또는 경로와 JSON 본문을 메서드 인수로 변환하고 제약 조건을 검사합니다.
3. Controller는 인증 principal의 숫자 ID를 읽습니다.
4. 예약 조회는 `ReservationService.getReservationsByAdmin`을, 상태 변경은 `updateStatusByAdmin`을 호출합니다.
5. 확인된 서비스 코드에서는 관리자 역할 재검증, 예약 조회/상태 변경, 페이지 또는 응답 DTO 변환을 수행합니다.

## 6. 확인된 관계와 추론

- **확인됨:** `/admin/**` 접근 역할은 `SecurityConfig`에서 `ADMIN`으로 제한됩니다.
- **확인됨:** JWT 필터는 SecurityContext에 Authentication을 설정하고, Controller는 principal의 숫자값을 회원 ID로 사용합니다.
- **확인됨:** `ReservationService`는 전달된 ID로 회원을 조회해 `Role.ADMIN`인지 다시 확인하고, 관리자 목록은 `createdAt` 내림차순으로 페이지 조회합니다.
- **확인됨:** 상태 변경 서비스는 관리자 검증 후 예약을 찾아 상태를 변경하고 flush한 다음 응답 DTO를 만듭니다.
- **추론:** 관리자 예약 화면이 이 API를 호출할 가능성이 높지만, 실제 프론트엔드 연결 여부는 이 파일만으로 확정할 수 없습니다.

## 7. 코드에서 실제 쓰이는 핵심 개념

- **HTTP 라우팅:** `@GetMapping`은 조회, `@PatchMapping`은 일부 상태 변경 요청을 메서드에 연결합니다.
- **입력 바인딩:** `@RequestParam`, `@PathVariable`, `@RequestBody`가 세 종류의 HTTP 입력 위치를 연결합니다.
- **의존성 주입:** 예약 로직을 직접 구현하지 않고 Service를 생성자로 주입받습니다.
- **검증:** `@Min`, `@Max`는 페이지 인수의 범위를, `@Valid`는 요청 DTO의 상태 필수성을 검사합니다.
- **SecurityContext:** 인증 필터가 현재 요청에 설정한 인증 정보를 읽는 저장소입니다.
- **서비스 계층의 재검증:** URL 보안 설정 이외에도 서비스가 관리자 역할을 확인하므로 내부 도메인 진입점에서도 권한 규칙이 유지됩니다.

## 8. 동작에 충실한 예시

예약 목록을 첫 페이지 기본 크기로 요청하면 `GET /admin/reservations?page=0&size=10`이며, 두 쿼리 매개변수를 생략해도 기본값은 같습니다. 이 Controller는 인증 ID와 함께 페이지 값을 Service로 전달합니다. 실제 정렬·페이지 총계는 서비스와 `ReservationPageResponse`가 결정합니다.

상태 변경은 `PATCH /admin/reservations/17/status`와 `{"status":"..."}` 형태의 JSON을 받습니다. `...`는 실제 `ReservationStatus` enum 값이어야 하며 이 문서에서는 enum에 있는 값 목록을 임의로 가정하지 않습니다. 검증 후 해당 예약 ID, 관리자 ID, 상태를 Service에 전달합니다.

## 9. 요약

이 Controller는 관리자 예약 요청의 라우팅, 입력 검증, 인증 ID 전달을 담당합니다. 실제 예약 권한·상태 변경 규칙은 ReservationService에서 처리되며, URL 접근도 SecurityConfig에서 ADMIN으로 제한됩니다.
