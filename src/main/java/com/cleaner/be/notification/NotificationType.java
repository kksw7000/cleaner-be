package com.cleaner.be.notification;

/** 알림 유형을 고정된 값으로 관리해 프론트엔드가 이벤트별 표시와 이동 동작을 구분하게 합니다. */
public enum NotificationType {
	RESERVATION_RECEIVED,
	RESERVATION_APPROVED,
	RESERVATION_REJECTED,
	RESERVATION_CANCELLED,
	RESERVATION_COMPLETED,
	RESERVATION_DELETED
}
