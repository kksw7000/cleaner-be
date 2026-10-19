package com.cleaner.be.admin;

import java.util.List;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.cleaner.be.auth.InvalidCredentialsException;
import com.cleaner.be.reservation.ReservationService;
import com.cleaner.be.reservation.dto.ReservationResponse;
import com.cleaner.be.reservation.dto.ReservationStatusUpdateRequest;

import jakarta.validation.Valid;

/** 관리자 예약 HTTP 진입점으로, SecurityConfig의 ADMIN 인가 후 예약 서비스에 처리 요청을 전달합니다. */
@RestController
@RequestMapping("/admin/reservations")
public class AdminReservationController {

	private final ReservationService reservationService;

	public AdminReservationController(ReservationService reservationService) {
		this.reservationService = reservationService;
	}

	/** 관리자에게 전체 예약 목록을 최신 요청순으로 제공해 승인과 처리 대상을 확인하게 합니다. */
	@GetMapping
	public List<ReservationResponse> getReservations() {
		return reservationService.getReservationsByAdmin(currentMemberId());
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
