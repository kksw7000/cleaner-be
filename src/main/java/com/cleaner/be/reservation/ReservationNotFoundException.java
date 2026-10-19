package com.cleaner.be.reservation;

/** 요청한 예약 식별자가 없을 때 API 경계에서 404로 변환할 도메인 예외입니다. */
public class ReservationNotFoundException extends RuntimeException {
	public ReservationNotFoundException() { super("예약을 찾을 수 없습니다."); }
}
