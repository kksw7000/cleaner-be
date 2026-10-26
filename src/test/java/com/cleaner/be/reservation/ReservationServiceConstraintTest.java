package com.cleaner.be.reservation;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.Optional;

import org.hibernate.exception.ConstraintViolationException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DataIntegrityViolationException;

import com.cleaner.be.auth.Member;
import com.cleaner.be.auth.MemberRepository;
import com.cleaner.be.reservation.dto.ReservationCreateRequest;

/** 예약 생성은 날짜 고유 제약만 도메인 충돌로 변환하고 다른 DB 오류는 보존하는지 확인합니다. */
class ReservationServiceConstraintTest {

	private ReservationRepository reservationRepository;
	private MemberRepository memberRepository;
	private ReservationService reservationService;

	/** 저장 경계의 예외 처리만 독립적으로 검증하도록 repository 의존성을 대역으로 준비합니다. */
	@BeforeEach
	void setUp() {
		reservationRepository = mock(ReservationRepository.class);
		memberRepository = mock(MemberRepository.class);
		reservationService = new ReservationService(reservationRepository, memberRepository);
		when(memberRepository.findById(1L)).thenReturn(Optional.of(mock(Member.class)));
		when(reservationRepository.existsByScheduledDateAndStatusIn(any(), any())).thenReturn(false);
	}

	/** 동시 예약으로 날짜 unique constraint가 위반되면 예약 불가 도메인 예외로 바꿉니다. */
	@Test
	void scheduledDateUniqueConstraintBecomesReservationUnavailable() {
		DataIntegrityViolationException databaseException = integrityViolation("uk_reservations_active_scheduled_date");
		when(reservationRepository.saveAndFlush(any(Reservation.class))).thenThrow(databaseException);

		assertThatThrownBy(this::createReservation)
			.isInstanceOf(ReservationDateUnavailableException.class);
	}

	/** 회원 외래 키 등 날짜 외 제약 위반은 변환하지 않고 원래 데이터 접근 예외를 유지합니다. */
	@Test
	void unrelatedConstraintViolationIsRethrownUnchanged() {
		DataIntegrityViolationException databaseException = integrityViolation("fk_reservations_member_id");
		when(reservationRepository.saveAndFlush(any(Reservation.class))).thenThrow(databaseException);

		assertThatThrownBy(this::createReservation)
			.isSameAs(databaseException);
	}

	/** 유효한 요청으로 실제 예약 생성 경로의 저장 예외 변환을 실행합니다. */
	private void createReservation() {
		reservationService.createReservation(1L, new ReservationCreateRequest(
			ServiceCategory.MATTRESS,
			LocalDateTime.now(java.time.Clock.system(java.time.ZoneId.of("Asia/Seoul"))).plusDays(1),
			"주소",
			"예약자",
			"01012345678",
			null));
	}

	/** Hibernate 제약 위반 원인을 Spring 데이터 예외 아래에 감싸 테스트합니다. */
	private DataIntegrityViolationException integrityViolation(String constraintName) {
		ConstraintViolationException cause = new ConstraintViolationException(
			"constraint violation", new SQLException("constraint violation"), constraintName);
		return new DataIntegrityViolationException("could not execute statement", cause);
	}
}
