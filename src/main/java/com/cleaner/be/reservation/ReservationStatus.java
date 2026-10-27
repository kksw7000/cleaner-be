package com.cleaner.be.reservation;

/** 예약의 처리 단계를 고정된 값으로 제한해 회원과 관리자가 서로 다른 상태를 해석하지 않게 합니다. */
public enum ReservationStatus {
	PENDING,
	APPROVED,
	REJECTED,
	CANCELLED,
	COMPLETED,
	/** 기존 API 값과 DB 데이터를 호환하기 위해 유지하는 취소 상태입니다. */
	CANCELED
}
