package com.cleaner.be.reservation;

import java.time.LocalDate;
import java.time.Clock;
import java.util.List;
import java.util.Set;

import org.hibernate.exception.ConstraintViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.cleaner.be.auth.Member;
import com.cleaner.be.auth.MemberRepository;
import com.cleaner.be.auth.Role;
import com.cleaner.be.notification.NotificationService;
import com.cleaner.be.notification.NotificationType;
import com.cleaner.be.reservation.dto.ReservationCreateRequest;
import com.cleaner.be.reservation.dto.ReservationAvailabilityResponse;
import com.cleaner.be.reservation.dto.ReservationPageResponse;
import com.cleaner.be.reservation.dto.ReservationResponse;

/** 예약 생성·조회·상태 변경 규칙을 처리해 Controller가 인증 정보와 DB 처리 세부 사항을 갖지 않게 합니다. */
@Service
public class ReservationService {

	private static final String SCHEDULED_DATE_UNIQUE_CONSTRAINT = "uk_reservations_active_scheduled_date";
	private static final List<ReservationStatus> ACTIVE_STATUSES = List.of(ReservationStatus.PENDING, ReservationStatus.APPROVED);

	private final ReservationRepository reservationRepository;
	private final MemberRepository memberRepository;
	private final NotificationService notificationService;
	private final Clock koreaClock;

	public ReservationService(ReservationRepository reservationRepository, MemberRepository memberRepository,
		NotificationService notificationService, Clock koreaClock) {
		this.reservationRepository = reservationRepository;
		this.memberRepository = memberRepository;
		this.notificationService = notificationService;
		this.koreaClock = koreaClock;
	}

	/** 로그인 회원을 예약자로 연결해 요청을 저장하고, 공백만 있는 문자열은 정리해 데이터 품질을 유지합니다. */
	@Transactional
	public ReservationResponse createReservation(Long memberId, ReservationCreateRequest request) {
		// 1. 예약자·대한민국 날짜 확인 → 2. 활성 날짜 중복 확인 → 3. 저장 및 응답 변환 순서로 처리합니다.
		Member member = findMember(memberId);
		LocalDate scheduledDate = request.scheduledAt().toLocalDate();
		validateNotPastDate(scheduledDate);
		// 달력 확인 후 먼저 저장된 활성 예약을 다시 검사하고, DB 인덱스가 동시 요청을 최종 직렬화합니다.
		if (reservationRepository.existsByScheduledDateAndStatusIn(scheduledDate, ACTIVE_STATUSES)) {
			throw new ReservationDateUnavailableException();
		}
		try {
			Reservation reservation = reservationRepository.saveAndFlush(new Reservation(request.serviceCategory(), request.scheduledAt(),
				request.address().trim(), request.contactName().trim(), request.contactPhone().trim(), normalizeNote(request.requestNote()), member));
			notificationService.create(member, reservation, NotificationType.RESERVATION_RECEIVED, "예약 요청이 접수되었습니다.");
			return toResponse(reservation);
		} catch (DataIntegrityViolationException exception) {
			// 날짜 고유 제약 위반만 예약 충돌로 바꾸고, 회원 FK 등 다른 데이터 오류는 원래 원인을 유지합니다.
			if (isScheduledDateUniqueConstraintViolation(exception)) {
				throw new ReservationDateUnavailableException();
			}
			throw exception;
		}
	}

	/** 대한민국 날짜를 기준으로 오늘은 허용하고 과거 날짜만 거부합니다. */
	private void validateNotPastDate(LocalDate scheduledDate) {
		if (scheduledDate.isBefore(LocalDate.now(koreaClock))) {
			throw new ReservationPastDateException();
		}
	}

	/** 예외 원인 체인에서 날짜 고유 제약 위반만 찾아 동시 예약 충돌인지 구분합니다. */
	private boolean isScheduledDateUniqueConstraintViolation(DataIntegrityViolationException exception) {
		Throwable cause = exception;
		while (cause != null) {
			if (cause instanceof ConstraintViolationException constraintViolation
				&& SCHEDULED_DATE_UNIQUE_CONSTRAINT.equals(constraintViolation.getConstraintName())) {
				return true;
			}
			cause = cause.getCause();
		}
		return false;
	}

	/** 선택 가능한 날짜를 달력에 표시하도록 대한민국 기준 해당 월의 날짜와 활성 예약 여부를 반환합니다. */
	@Transactional(readOnly = true)
	public List<ReservationAvailabilityResponse> getMonthlyAvailability(int year, int month) {
		LocalDate firstDate = LocalDate.of(year, month, 1);
		LocalDate lastDate = firstDate.withDayOfMonth(firstDate.lengthOfMonth());
		Set<LocalDate> occupiedDates = Set.copyOf(reservationRepository.findOccupiedDatesBetween(firstDate, lastDate, ACTIVE_STATUSES));
		return firstDate.datesUntil(lastDate.plusDays(1))
			.map(date -> new ReservationAvailabilityResponse(date, !occupiedDates.contains(date)))
			.toList();
	}

	/** 로그인 회원의 예약만 반환해 회원 간 연락처와 주소가 노출되지 않도록 합니다. */
	@Transactional(readOnly = true)
	public List<ReservationResponse> getMyReservations(Long memberId) {
		return reservationRepository.findByMemberIdOrderByCreatedAtDesc(memberId).stream().map(this::toResponse).toList();
	}

	/** 관리자만 예약을 페이지 단위로 조회할 수 있게 역할을 다시 검증해 대량 예약을 한 번에 읽지 않도록 합니다. */
	@Transactional(readOnly = true)
	public ReservationPageResponse getReservationsByAdmin(Long adminId, int page, int size) {
		// 관리자 확인 → 최신순 페이지 조회 → API 페이지 응답 변환 순서로 처리합니다.
		validateAdmin(findMember(adminId));
		Page<Reservation> reservations = reservationRepository.findAllByOrderByCreatedAtDesc(
			PageRequest.of(page, size, Sort.by(Sort.Order.desc("createdAt"))));
		return new ReservationPageResponse(reservations.map(this::toResponse).getContent(), reservations.getNumber(),
			reservations.getSize(), reservations.getTotalElements(), reservations.getTotalPages(), reservations.isFirst(), reservations.isLast());
	}

	/** 관리자 권한을 확인한 뒤 예약 상태만 변경해 회원이 승인·완료 처리를 임의로 수행하지 못하게 합니다. */
	@Transactional
	public ReservationResponse updateStatusByAdmin(Long reservationId, Long adminId, ReservationStatus status) {
		// 관리자 확인 → 예약 조회 → 상태 변경 및 감사 시각 반영 → 알림 저장 → 응답 변환 순서로 처리합니다.
		validateAdmin(findMember(adminId));
		Reservation reservation = findReservation(reservationId);
		ReservationStatus previousStatus = reservation.getStatus();
		// 비활성 예약을 다시 활성화할 때 기존 활성 예약과 날짜가 겹치는지 먼저 검사합니다.
		if (ACTIVE_STATUSES.contains(status) && !ACTIVE_STATUSES.contains(reservation.getStatus())
			&& reservationRepository.existsByScheduledDateAndStatusIn(reservation.getScheduledDate(), ACTIVE_STATUSES)) {
			throw new ReservationDateUnavailableException();
		}
		reservation.updateStatus(status);
		// 응답 전에 변경 감지를 실행해 AuditingEntityListener가 기록한 수정 시각을 포함합니다.
		try {
			reservationRepository.flush();
		} catch (DataIntegrityViolationException exception) {
			if (isScheduledDateUniqueConstraintViolation(exception)) {
				throw new ReservationDateUnavailableException();
			}
			throw exception;
		}
		if (previousStatus != status) {
			NotificationType notificationType = notificationTypeFor(status);
			notificationService.create(reservation.getMember(), reservation, notificationType, messageFor(status));
		}
		return toResponse(reservation);
	}

	/** 관리자 예약 삭제 시 먼저 알림을 영속화하고 예약 FK는 마이그레이션의 SET NULL로 보존합니다. */
	@Transactional
	public void deleteReservationByAdmin(Long reservationId, Long adminId) {
		validateAdmin(findMember(adminId));
		Reservation reservation = findReservation(reservationId);
		notificationService.create(reservation.getMember(), null, NotificationType.RESERVATION_DELETED, "예약이 삭제되었습니다.");
		reservationRepository.delete(reservation);
		reservationRepository.flush();
	}

	/** 예약 상태를 회원 알림 유형으로 변환해 상태가 바뀔 때마다 알맞은 메시지를 선택합니다. */
	private NotificationType notificationTypeFor(ReservationStatus status) {
		return switch (status) {
			case PENDING -> NotificationType.RESERVATION_RECEIVED;
			case APPROVED -> NotificationType.RESERVATION_APPROVED;
			case REJECTED -> NotificationType.RESERVATION_REJECTED;
			case CANCELLED, CANCELED -> NotificationType.RESERVATION_CANCELLED;
			case COMPLETED -> NotificationType.RESERVATION_COMPLETED;
		};
	}

	/** 현재 상태의 의미를 짧은 웹 알림 문구로 변환해 프론트엔드가 바로 표시하게 합니다. */
	private String messageFor(ReservationStatus status) {
		return switch (status) {
			case PENDING -> "예약 요청이 접수되었습니다.";
			case APPROVED -> "예약이 확정되었습니다.";
			case REJECTED -> "예약이 거절되었습니다.";
			case CANCELLED, CANCELED -> "예약이 취소되었습니다.";
			case COMPLETED -> "예약이 완료되었습니다.";
		};
	}

	/** 잘못된 인증 ID는 예약 작업에 사용할 수 없으므로 권한 예외로 변환합니다. */
	private Member findMember(Long memberId) {
		return memberRepository.findById(memberId).orElseThrow(ReservationAccessDeniedException::new);
	}

	/** 존재하지 않는 예약 ID를 예약 전용 예외로 변환해 호출자가 원인을 구분하게 합니다. */
	private Reservation findReservation(Long reservationId) {
		return reservationRepository.findById(reservationId).orElseThrow(ReservationNotFoundException::new);
	}

	/** ADMIN 역할이 아닌 회원의 처리 요청을 거부해 URL 보안 설정 외에도 도메인 규칙을 유지합니다. */
	private void validateAdmin(Member member) {
		if (member.getRole() != Role.ADMIN) {
			throw new ReservationAccessDeniedException();
		}
	}

	/** 선택 입력인 요청 사항은 빈 문자열 대신 null로 정규화해 저장 의미를 일관되게 만듭니다. */
	private String normalizeNote(String requestNote) {
		if (requestNote == null || requestNote.isBlank()) {
			return null;
		}
		return requestNote.trim();
	}

	/** 엔티티와 회원 정보를 API 응답으로 옮겨 JPA 프록시와 내부 구조가 외부에 노출되지 않게 합니다. */
	private ReservationResponse toResponse(Reservation reservation) {
		return new ReservationResponse(reservation.getId(), reservation.getServiceCategory(), reservation.getScheduledAt(),
			reservation.getAddress(), reservation.getContactName(), reservation.getContactPhone(), reservation.getRequestNote(),
			reservation.getStatus(), reservation.getMember().getId(), reservation.getMember().getName(), reservation.getCreatedAt(),
			reservation.getUpdatedAt());
	}
}
