package com.cleaner.be.notification;

import java.io.IOException;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.MediaType;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import com.cleaner.be.auth.InvalidCredentialsException;
import com.cleaner.be.notification.dto.NotificationHomeResponse;
import com.cleaner.be.notification.dto.NotificationPageResponse;
import com.cleaner.be.notification.dto.NotificationResponse;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;

/** 로그인 회원의 홈 알림·보관 목록·읽음 처리를 제공하며 회원 ID는 JWT 인증 정보에서만 가져옵니다. */
@RestController
@Validated
@RequestMapping("/api/notifications")
public class NotificationController {

	private final NotificationService notificationService;
	private final NotificationStreamService notificationStreamService;

	public NotificationController(NotificationService notificationService, NotificationStreamService notificationStreamService) {
		this.notificationService = notificationService;
		this.notificationStreamService = notificationStreamService;
	}

	/** 홈 화면에 최신 알림 세 건과 전체 미읽음 개수를 반환합니다. */
	@GetMapping("/home")
	public NotificationHomeResponse getHomeNotifications() {
		return notificationService.getHomeNotifications(currentMemberId());
	}

	/** 읽은 알림도 포함한 보관 내역을 최신순 페이지로 조회합니다. */
	@GetMapping
	public NotificationPageResponse getNotifications(@RequestParam(defaultValue = "0") @Min(0) int page,
		@RequestParam(defaultValue = "10") @Min(1) @Max(100) int size) {
		PageRequest pageable = PageRequest.of(page, size, Sort.by(Sort.Order.desc("createdAt"), Sort.Order.desc("id")));
		return notificationService.getNotifications(currentMemberId(), pageable);
	}

	/** 홈 배지에 표시할 전체 미읽음 개수를 조회합니다. */
	@GetMapping("/unread-count")
	public long getUnreadCount() {
		return notificationService.getUnreadCount(currentMemberId());
	}

	/** JWT 인증을 통과한 브라우저에 회원 본인의 신규 알림을 서버 전송 이벤트로 전달합니다. */
	@GetMapping(value = "/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
	public SseEmitter streamNotifications() throws IOException {
		return notificationStreamService.subscribe(currentMemberId());
	}

	/** 본인 알림을 읽음 처리하고 최초 읽음 시각을 반환합니다. */
	@PatchMapping("/{id}/read")
	public NotificationResponse markAsRead(@PathVariable Long id) {
		return notificationService.markAsRead(currentMemberId(), id);
	}

	/** 현재 회원의 미읽음 알림 전체를 읽음 처리하고 변경 개수를 반환합니다. */
	@PatchMapping("/read-all")
	public int markAllAsRead() {
		return notificationService.markAllAsRead(currentMemberId());
	}

	/** 인증 필터가 저장한 회원 ID를 읽고 숫자 principal이 없으면 인증 오류로 처리합니다. */
	private Long currentMemberId() {
		Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
		if (authentication == null || !(authentication.getPrincipal() instanceof Number memberId)) {
			throw new InvalidCredentialsException();
		}
		return memberId.longValue();
	}
}
