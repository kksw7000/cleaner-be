package com.cleaner.be.notification.dto;

import java.util.List;

/** 홈 화면에 표시할 최신 알림 세 건과 전체 미읽음 개수를 함께 반환합니다. */
public record NotificationHomeResponse(List<NotificationResponse> notifications, long unreadCount) {
}
