package com.cleaner.be.reservation;

import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InOrder;

import com.cleaner.be.auth.Member;
import com.cleaner.be.auth.MemberRepository;
import com.cleaner.be.auth.Role;
import com.cleaner.be.notification.NotificationService;
import com.cleaner.be.notification.NotificationType;

/** 예약 이벤트가 실제 상태 변경·삭제와 같은 트랜잭션 흐름에서 회원 알림을 만드는지 확인합니다. */
class ReservationServiceNotificationTest {

	private static final Clock FIXED_CLOCK = Clock.fixed(Instant.parse("2026-10-27T03:00:00Z"), ZoneId.of("Asia/Seoul"));

	private ReservationRepository reservationRepository;
	private MemberRepository memberRepository;
	private NotificationService notificationService;
	private ReservationService reservationService;
	private Member admin;
	private Member owner;
	private Reservation reservation;

	/** 관리자, 예약, 알림 저장 경계를 대역으로 준비해 이벤트 연결을 검증합니다. */
	@BeforeEach
	void setUp() {
		reservationRepository = mock(ReservationRepository.class);
		memberRepository = mock(MemberRepository.class);
		notificationService = mock(NotificationService.class);
		reservationService = new ReservationService(reservationRepository, memberRepository, notificationService, FIXED_CLOCK);
		admin = mock(Member.class);
		owner = mock(Member.class);
		reservation = mock(Reservation.class);
		when(admin.getRole()).thenReturn(Role.ADMIN);
		when(memberRepository.findById(1L)).thenReturn(Optional.of(admin));
		when(reservationRepository.findById(2L)).thenReturn(Optional.of(reservation));
		when(reservation.getMember()).thenReturn(owner);
		when(reservation.getStatus()).thenReturn(ReservationStatus.PENDING);
	}

	/** 예약을 승인하면 예약자에게 승인 알림을 만듭니다. */
	@Test
	void approvalCreatesNotificationForReservationOwner() {
		reservationService.updateStatusByAdmin(2L, 1L, ReservationStatus.APPROVED);

		verify(notificationService).create(owner, reservation, NotificationType.RESERVATION_APPROVED, "예약이 확정되었습니다.");
	}

	/** 취소 상태의 구버전 값도 동일한 취소 알림으로 정규화합니다. */
	@Test
	void legacyCancelledStatusCreatesCancellationNotification() {
		reservationService.updateStatusByAdmin(2L, 1L, ReservationStatus.CANCELED);

		verify(notificationService).create(owner, reservation, NotificationType.RESERVATION_CANCELLED, "예약이 취소되었습니다.");
	}

	/** 실제 상태가 바뀌지 않은 요청은 같은 알림을 중복 생성하지 않습니다. */
	@Test
	void unchangedStatusDoesNotCreateDuplicateNotification() {
		reservationService.updateStatusByAdmin(2L, 1L, ReservationStatus.PENDING);

		verify(notificationService, never()).create(owner, reservation, NotificationType.RESERVATION_RECEIVED, "예약 요청이 접수되었습니다.");
	}

	/** 관리자 예약 삭제는 알림을 먼저 영속화한 다음 예약을 제거합니다. */
	@Test
	void deletionPersistsNotificationBeforeRemovingReservation() {
		reservationService.deleteReservationByAdmin(2L, 1L);

		InOrder order = inOrder(notificationService, reservationRepository);
		order.verify(notificationService).create(owner, null, NotificationType.RESERVATION_DELETED, "예약이 삭제되었습니다.");
		order.verify(reservationRepository).delete(reservation);
		order.verify(reservationRepository).flush();
	}
}
