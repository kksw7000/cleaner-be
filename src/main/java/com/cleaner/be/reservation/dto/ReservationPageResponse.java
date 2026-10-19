package com.cleaner.be.reservation.dto;

import java.util.List;

/** Spring Data Page를 직접 노출하지 않고 관리자 예약 목록의 페이지 정보를 함께 전달합니다. */
public record ReservationPageResponse(
	List<ReservationResponse> content,
	int page,
	int size,
	long totalElements,
	int totalPages,
	boolean first,
	boolean last
) {
}
