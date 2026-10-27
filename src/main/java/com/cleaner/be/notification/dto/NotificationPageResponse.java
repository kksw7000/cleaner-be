package com.cleaner.be.notification.dto;

import java.util.List;

/** 알림 보관 페이지의 현재 항목과 페이지 이동 정보를 반환합니다. */
public record NotificationPageResponse(
	List<NotificationResponse> notifications,
	int page,
	int size,
	long totalElements,
	int totalPages,
	boolean first,
	boolean last
) {
}
