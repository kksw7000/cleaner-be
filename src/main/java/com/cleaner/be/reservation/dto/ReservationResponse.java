package com.cleaner.be.reservation.dto;

import java.time.LocalDateTime;

import com.cleaner.be.reservation.ReservationStatus;
import com.cleaner.be.reservation.ServiceCategory;

/** 회원 예약 내역과 관리자 처리 화면이 공통으로 사용하는 예약 상세 응답입니다. */
public record ReservationResponse(
	Long id,
	ServiceCategory serviceCategory,
	LocalDateTime scheduledAt,
	String address,
	String contactName,
	String contactPhone,
	String requestNote,
	ReservationStatus status,
	Long memberId,
	String memberName,
	LocalDateTime createdAt,
	LocalDateTime updatedAt
) {
}
