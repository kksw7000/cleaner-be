package com.cleaner.be.notification;

import com.cleaner.be.notification.dto.NotificationResponse;

/** 커밋된 알림을 회원별 SSE 구독자에게 전달하기 위해 사용하는 애플리케이션 이벤트입니다. */
public record NotificationCreatedEvent(Long memberId, NotificationResponse notification) {
}
