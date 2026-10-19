package com.cleaner.be.reservation;

/** 예약 소유자가 아닌 회원이 조회하거나 관리자가 아닌 사용자가 처리할 때 발생하는 권한 예외입니다. */
public class ReservationAccessDeniedException extends RuntimeException {
	public ReservationAccessDeniedException() { super("예약에 접근할 권한이 없습니다."); }
}
