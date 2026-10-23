package com.cleaner.be.reservation;

/** 예약 달력에서 현재 연도 기준 허용 범위를 벗어난 연도를 요청했을 때 발생합니다. */
public class ReservationYearOutOfRangeException extends RuntimeException {

	/** 요청한 연도가 벗어난 최소·최대 연도를 응답 메시지에 포함해 올바른 범위를 안내합니다. */
	public ReservationYearOutOfRangeException(int minimumYear, int maximumYear) {
		super("예약 조회 연도는 " + minimumYear + "년부터 " + maximumYear + "년까지 선택할 수 있습니다.");
	}
}
