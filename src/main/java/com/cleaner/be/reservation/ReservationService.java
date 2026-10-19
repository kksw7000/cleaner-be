package com.cleaner.be.reservation;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.cleaner.be.auth.Member;
import com.cleaner.be.auth.MemberRepository;
import com.cleaner.be.auth.Role;
import com.cleaner.be.reservation.dto.ReservationCreateRequest;
import com.cleaner.be.reservation.dto.ReservationPageResponse;
import com.cleaner.be.reservation.dto.ReservationResponse;

/** 예약 생성·조회·상태 변경 규칙을 처리해 Controller가 인증 정보와 DB 처리 세부 사항을 갖지 않게 합니다. */
@Service
public class ReservationService {

	private final ReservationRepository reservationRepository;
	private final MemberRepository memberRepository;

	public ReservationService(ReservationRepository reservationRepository, MemberRepository memberRepository) {
		this.reservationRepository = reservationRepository;
		this.memberRepository = memberRepository;
	}

	/** 로그인 회원을 예약자로 연결해 요청을 저장하고, 공백만 있는 문자열은 정리해 데이터 품질을 유지합니다. */
	@Transactional
	public ReservationResponse createReservation(Long memberId, ReservationCreateRequest request) {
		// 1. 예약자 조회 → 2. 입력 정리 및 예약 저장 → 3. 응답 변환 순서로 처리합니다.
		Member member = findMember(memberId);
		Reservation reservation = reservationRepository.save(new Reservation(request.serviceCategory(), request.scheduledAt(),
			request.address().trim(), request.contactName().trim(), request.contactPhone().trim(), normalizeNote(request.requestNote()), member));
		return toResponse(reservation);
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
		// 관리자 확인 → 예약 조회 → 상태 변경 및 감사 시각 반영 → 응답 변환 순서로 처리합니다.
		validateAdmin(findMember(adminId));
		Reservation reservation = findReservation(reservationId);
		reservation.updateStatus(status);
		// 응답 전에 변경 감지를 실행해 AuditingEntityListener가 기록한 수정 시각을 포함합니다.
		reservationRepository.flush();
		return toResponse(reservation);
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
