package com.cleaner.be.reservation;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.cleaner.be.auth.Member;
import com.cleaner.be.auth.MemberRepository;
import com.cleaner.be.notification.NotificationService;
import com.cleaner.be.notification.NotificationType;
import com.cleaner.be.reservation.dto.ReservationCreateRequest;

/** 예약 날짜 정책이 한국 날짜와 활성 상태 조회를 기준으로 동작하는지 확인합니다. */
class ReservationDatePolicyTest {

	private static final Clock FIXED_CLOCK = Clock.fixed(Instant.parse("2026-10-26T00:00:00Z"), ZoneId.of("Asia/Seoul"));

	private ReservationRepository reservationRepository;
	private MemberRepository memberRepository;
	private NotificationService notificationService;
	private ReservationService reservationService;

	/** 예약 저장 경계와 회원 조회를 대역으로 만들어 날짜 규칙만 검증합니다. */
	@BeforeEach
	void setUp() {
		reservationRepository = mock(ReservationRepository.class);
		memberRepository = mock(MemberRepository.class);
		notificationService = mock(NotificationService.class);
		reservationService = new ReservationService(reservationRepository, memberRepository, notificationService, FIXED_CLOCK);
		when(memberRepository.findById(1L)).thenReturn(Optional.of(mock(Member.class)));
		when(reservationRepository.existsByScheduledDateAndStatusIn(any(), anyCollection())).thenReturn(false);
		when(reservationRepository.saveAndFlush(any(Reservation.class))).thenAnswer(invocation -> invocation.getArgument(0));
	}

	/** 대한민국 오늘 날짜는 선택한 시각이 지나갔더라도 예약 요청을 허용합니다. */
	@Test
	void todayIsAllowed() {
		LocalDate today = LocalDate.now(FIXED_CLOCK);

		reservationService.createReservation(1L, request(today.atStartOfDay()));

		verify(reservationRepository).existsByScheduledDateAndStatusIn(today,
			java.util.List.of(ReservationStatus.PENDING, ReservationStatus.APPROVED));
		verify(notificationService).create(any(Member.class), any(Reservation.class), eq(NotificationType.RESERVATION_RECEIVED),
			eq("예약 요청이 접수되었습니다."));
	}

	/** 대한민국 어제 날짜는 데이터베이스 조회나 저장 전에 예약 요청을 거부합니다. */
	@Test
	void pastDateIsRejected() {
		LocalDate yesterday = LocalDate.now(FIXED_CLOCK).minusDays(1);

		assertThatThrownBy(() -> reservationService.createReservation(1L, request(yesterday.atStartOfDay())))
			.isInstanceOf(ReservationPastDateException.class);
	}

	/** 회원 입력을 구성해 예약 서비스의 정책 경로를 직접 호출합니다. */
	private ReservationCreateRequest request(LocalDateTime scheduledAt) {
		return new ReservationCreateRequest(ServiceCategory.MATTRESS, scheduledAt, "주소", "예약자", "01012345678", null);
	}
}
