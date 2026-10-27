package com.cleaner.be.reservation;

/** 대한민국 기준 과거 날짜의 예약 요청을 입력 오류로 처리하기 위한 도메인 예외입니다. */
public class ReservationPastDateException extends RuntimeException {

	/** 오늘 이전 날짜는 예약할 수 없다는 이유를 회원에게 전달합니다. */
	public ReservationPastDateException() {
		super("지난 날짜는 예약할 수 없습니다.");
	}
}
