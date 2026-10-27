package com.cleaner.be.notification;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import com.cleaner.be.auth.Member;
import com.cleaner.be.reservation.Reservation;

/** 알림 조회가 회원 소유 범위와 최신 3건·페이지·읽음 규칙을 지키는지 확인합니다. */
class NotificationServiceTest {

	private static final Clock FIXED_CLOCK = Clock.fixed(Instant.parse("2026-10-27T03:00:00Z"), ZoneId.of("Asia/Seoul"));

	private NotificationRepository notificationRepository;
	private NotificationService notificationService;
	private Member member;
	private Reservation reservation;

	/** DB 응답을 대역으로 만들어 알림 서비스의 필터·상태 처리만 검증합니다. */
	@BeforeEach
	void setUp() {
		notificationRepository = mock(NotificationRepository.class);
		notificationService = new NotificationService(notificationRepository, mock(ApplicationEventPublisher.class), FIXED_CLOCK);
		member = mock(Member.class);
		reservation = mock(Reservation.class);
		when(reservation.getId()).thenReturn(44L);
	}

	/** 홈 응답은 저장된 최신 세 건과 전체 미읽음 개수를 함께 사용합니다. */
	@Test
	void homeReturnsThreeNewestAndAllUnreadCount() {
		List<Notification> newest = List.of(
			notification(NotificationType.RESERVATION_APPROVED, "예약이 확정되었습니다.", null),
			notification(NotificationType.RESERVATION_CANCELLED, "예약이 취소되었습니다.", FIXED_CLOCK.instant().atZone(FIXED_CLOCK.getZone()).toLocalDateTime()),
			notification(NotificationType.RESERVATION_RECEIVED, "예약 요청이 접수되었습니다.", null));
		when(notificationRepository.findTop3ByMemberIdOrderByCreatedAtDescIdDesc(7L)).thenReturn(newest);
		when(notificationRepository.countByMemberIdAndReadAtIsNull(7L)).thenReturn(5L);

		var response = notificationService.getHomeNotifications(7L);

		assertThat(response.notifications()).hasSize(3);
		assertThat(response.notifications()).extracting(item -> item.type())
			.containsExactly(NotificationType.RESERVATION_APPROVED, NotificationType.RESERVATION_CANCELLED, NotificationType.RESERVATION_RECEIVED);
		assertThat(response.unreadCount()).isEqualTo(5L);
	}

	/** 알림 페이지는 읽음 여부와 상관없이 전달된 페이지 범위 그대로 응답합니다. */
	@Test
	void pageResponsePreservesPageMetadataAndReadTime() {
		Notification readNotification = notification(NotificationType.RESERVATION_DELETED, "예약이 삭제되었습니다.", FIXED_CLOCK.instant()
			.atZone(FIXED_CLOCK.getZone()).toLocalDateTime());
		PageRequest pageable = PageRequest.of(1, 10);
		when(notificationRepository.findByMemberIdOrderByCreatedAtDescIdDesc(7L, pageable))
			.thenReturn(new PageImpl<>(List.of(readNotification), pageable, 11));

		var response = notificationService.getNotifications(7L, pageable);

		assertThat(response.page()).isEqualTo(1);
		assertThat(response.totalElements()).isEqualTo(11);
		assertThat(response.notifications()).singleElement().satisfies(item -> {
			assertThat(item.type()).isEqualTo(NotificationType.RESERVATION_DELETED);
			assertThat(item.reservationId()).isNull();
			assertThat(item.readAt()).isNotNull();
		});
	}

	/** 소유 회원 ID까지 일치할 때에만 해당 알림을 읽음 처리합니다. */
	@Test
	void markAsReadUsesMemberOwnedNotificationAndKeepsFirstReadTime() {
		Notification notification = notification(NotificationType.RESERVATION_APPROVED, "예약이 확정되었습니다.", null);
		when(notificationRepository.findByIdAndMemberId(9L, 7L)).thenReturn(Optional.of(notification));

		var response = notificationService.markAsRead(7L, 9L);

		assertThat(response.readAt()).isEqualTo(LocalDateTime.now(FIXED_CLOCK));
		notification.markAsRead(LocalDateTime.now(FIXED_CLOCK).plusDays(1));
		assertThat(notification.getReadAt()).isEqualTo(LocalDateTime.now(FIXED_CLOCK));
	}

	/** 다른 회원의 알림 ID는 소유자 조회에서 걸러져 존재하지 않는 것처럼 처리합니다. */
	@Test
	void markAsReadRejectsNotificationOwnedByAnotherMember() {
		when(notificationRepository.findByIdAndMemberId(9L, 7L)).thenReturn(Optional.empty());

		assertThatThrownBy(() -> notificationService.markAsRead(7L, 9L))
			.isInstanceOf(NotificationNotFoundException.class);
	}

	/** 전체 읽음 처리는 현재 회원 ID와 서버 시각만 저장소에 전달합니다. */
	@Test
	void markAllAsReadReturnsUpdatedCount() {
		when(notificationRepository.markAllAsRead(7L, LocalDateTime.now(FIXED_CLOCK))).thenReturn(3);

		assertThat(notificationService.markAllAsRead(7L)).isEqualTo(3);
		verify(notificationRepository).markAllAsRead(7L, LocalDateTime.now(FIXED_CLOCK));
	}

	/** 지정한 상태의 응답을 반환하도록 알림 엔티티를 구성합니다. */
	private Notification notification(NotificationType type, String message, LocalDateTime readAt) {
		Notification notification = new Notification(member, null, type, message, LocalDateTime.now(FIXED_CLOCK));
		if (readAt != null) {
			notification.markAsRead(readAt);
		}
		return notification;
	}
}
