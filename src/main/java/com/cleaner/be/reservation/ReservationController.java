package com.cleaner.be.reservation;

import java.time.Year;
import java.time.Clock;
import java.time.ZoneId;
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
import com.cleaner.be.reservation.dto.ReservationCreateRequest;
import com.cleaner.be.reservation.dto.ReservationAvailabilityResponse;
import com.cleaner.be.reservation.dto.ReservationResponse;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;

/** 회원 예약 HTTP 진입점으로, 요청 파라미터 범위를 검증하고 JWT에서 읽은 회원 ID를 ReservationService에 전달합니다. */
@RestController
@Validated
@RequestMapping("/api/reservations")
public class ReservationController {

	private static final Clock KOREA_CLOCK = Clock.system(ZoneId.of("Asia/Seoul"));

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
		int currentYear = Year.now(KOREA_CLOCK).getValue();
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
