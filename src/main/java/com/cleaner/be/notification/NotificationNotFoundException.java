package com.cleaner.be.notification;

/** 존재하지 않거나 현재 회원 소유가 아닌 알림의 접근을 숨기기 위해 사용하는 예외입니다. */
public class NotificationNotFoundException extends RuntimeException {

	/** 알림이 없거나 회원 소유가 다르면 동일한 메시지로 응답해 다른 회원 데이터 노출을 막습니다. */
	public NotificationNotFoundException() {
		super("알림을 찾을 수 없습니다.");
	}
}
