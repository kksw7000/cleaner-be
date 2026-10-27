package com.cleaner.be.notification.dto;

import java.time.LocalDateTime;

import com.cleaner.be.notification.NotificationType;

/** 알림 목록과 읽음 처리 응답에 필요한 공개 필드만 전달합니다. */
public record NotificationResponse(
	Long id,
	NotificationType type,
	String message,
	Long reservationId,
	LocalDateTime createdAt,
	LocalDateTime readAt
) {
}
