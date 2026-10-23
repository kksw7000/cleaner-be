package com.cleaner.be.reservation.dto;

import java.time.LocalDate;

/** 예약 달력이 개인정보 없이 날짜별 선택 가능 여부만 표시하도록 전달하는 응답입니다. */
public record ReservationAvailabilityResponse(
	LocalDate date,
	boolean available
) {
}
