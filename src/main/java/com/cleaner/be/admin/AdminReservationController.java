package com.cleaner.be.admin;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.validation.annotation.Validated;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
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

	/** 관리자가 예약을 삭제하면 알림을 남겨 예약자의 다음 페이지 방문 때 삭제 사실을 표시합니다. */
	@DeleteMapping("/{id}")
	@ResponseStatus(HttpStatus.NO_CONTENT)
	public void deleteReservation(@PathVariable Long id) {
		reservationService.deleteReservationByAdmin(id, currentMemberId());
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
