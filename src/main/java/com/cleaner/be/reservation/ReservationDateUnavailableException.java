package com.cleaner.be.reservation;

/** 이미 활성 예약이 있는 날짜에 중복 예약을 생성하려 할 때 409 응답으로 변환하기 위한 도메인 예외입니다. */
public class ReservationDateUnavailableException extends RuntimeException {

	/** 사용자에게 다른 날짜를 선택해야 하는 이유를 일관되게 전달합니다. */
	public ReservationDateUnavailableException() {
		super("이미 예약된 날짜입니다. 다른 날짜를 선택해주세요.");
	}
}
