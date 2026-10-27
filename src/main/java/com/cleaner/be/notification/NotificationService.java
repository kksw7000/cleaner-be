package com.cleaner.be.notification;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.cleaner.be.auth.Member;
import com.cleaner.be.notification.dto.NotificationHomeResponse;
import com.cleaner.be.notification.dto.NotificationPageResponse;
import com.cleaner.be.notification.dto.NotificationResponse;
import com.cleaner.be.reservation.Reservation;

/** 알림을 저장하고 회원 본인에게만 최신 미리보기·페이지·읽음 처리를 제공합니다. */
@Service
public class NotificationService {

	private final NotificationRepository notificationRepository;
	private final ApplicationEventPublisher eventPublisher;
	private final Clock koreaClock;

	public NotificationService(NotificationRepository notificationRepository, ApplicationEventPublisher eventPublisher, Clock koreaClock) {
		this.notificationRepository = notificationRepository;
		this.eventPublisher = eventPublisher;
		this.koreaClock = koreaClock;
	}

	/** 예약 이벤트 알림을 같은 업무 트랜잭션에 저장해 상태 변경과 알림 누락을 방지합니다. */
	@Transactional
	public Notification create(Member member, Reservation reservation, NotificationType type, String message) {
		Notification notification = notificationRepository.saveAndFlush(
			new Notification(member, reservation, type, message, LocalDateTime.now(koreaClock)));
		eventPublisher.publishEvent(new NotificationCreatedEvent(member.getId(), toResponse(notification)));
		return notification;
	}

	/** 홈 화면에는 최신 세 건만 표시하고, 배지에는 전체 미읽음 개수를 반환합니다. */
	@Transactional(readOnly = true)
	public NotificationHomeResponse getHomeNotifications(Long memberId) {
		List<NotificationResponse> notifications = notificationRepository.findTop3ByMemberIdOrderByCreatedAtDescIdDesc(memberId)
			.stream().map(this::toResponse).toList();
		return new NotificationHomeResponse(notifications, notificationRepository.countByMemberIdAndReadAtIsNull(memberId));
	}

	/** 알림 보관 페이지에서 로그인 회원의 전체 알림만 페이지 단위로 반환합니다. */
	@Transactional(readOnly = true)
	public NotificationPageResponse getNotifications(Long memberId, Pageable pageable) {
		Page<NotificationResponse> page = notificationRepository.findByMemberIdOrderByCreatedAtDescIdDesc(memberId, pageable)
			.map(this::toResponse);
		return new NotificationPageResponse(page.getContent(), page.getNumber(), page.getSize(), page.getTotalElements(),
			page.getTotalPages(), page.isFirst(), page.isLast());
	}

	/** 홈 배지 표시를 위해 로그인 회원의 전체 미읽음 개수를 반환합니다. */
	@Transactional(readOnly = true)
	public long getUnreadCount(Long memberId) {
		return notificationRepository.countByMemberIdAndReadAtIsNull(memberId);
	}

	/** 본인 알림만 읽음 처리하고 이미 읽은 알림의 최초 읽음 시각은 유지합니다. */
	@Transactional
	public NotificationResponse markAsRead(Long memberId, Long notificationId) {
		Notification notification = notificationRepository.findByIdAndMemberId(notificationId, memberId)
			.orElseThrow(NotificationNotFoundException::new);
		notification.markAsRead(LocalDateTime.now(koreaClock));
		return toResponse(notification);
	}

	/** 현재 회원의 모든 미읽음 알림에 같은 확인 시각을 기록하고 변경된 개수를 반환합니다. */
	@Transactional
	public int markAllAsRead(Long memberId) {
		return notificationRepository.markAllAsRead(memberId, LocalDateTime.now(koreaClock));
	}

	/** 삭제된 예약 참조는 null로 응답하면서 보존된 알림 문구와 시각을 제공합니다. */
	private NotificationResponse toResponse(Notification notification) {
		Long reservationId = notification.getReservation() == null ? null : notification.getReservation().getId();
		return new NotificationResponse(notification.getId(), notification.getType(), notification.getMessage(), reservationId,
			notification.getCreatedAt(), notification.getReadAt());
	}
}
