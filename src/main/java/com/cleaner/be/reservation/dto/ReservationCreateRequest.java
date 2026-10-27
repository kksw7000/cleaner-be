package com.cleaner.be.reservation.dto;

import java.time.LocalDateTime;

import com.cleaner.be.reservation.ServiceCategory;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/** 회원이 예약할 서비스·일정·방문 정보를 전달하며, 과거 일정과 빈 필수 입력을 요청 단계에서 차단합니다. */
public record ReservationCreateRequest(
	@NotNull ServiceCategory serviceCategory,
	@NotNull LocalDateTime scheduledAt,
	@NotBlank @Size(max = 200) String address,
	@NotBlank @Size(max = 30) String contactName,
	@NotBlank @Size(max = 20) String contactPhone,
	@Size(max = 2000) String requestNote
) {
}
