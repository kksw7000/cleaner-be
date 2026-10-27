package com.cleaner.be.reservation;

import java.time.LocalDate;
import java.util.Collection;
import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/** 예약의 저장과 회원별·관리자용 조회를 제공하며, 응답에 필요한 회원 정보를 함께 읽습니다. */
public interface ReservationRepository extends JpaRepository<Reservation, Long> {

	/** 로그인 회원의 예약만 최신 생성순으로 찾아 다른 회원 예약 노출을 막습니다. */
	@EntityGraph(attributePaths = "member")
	List<Reservation> findByMemberIdOrderByCreatedAtDesc(Long memberId);

	/** 관리자가 처리 대상을 페이지 단위로 확인하도록 예약자 정보를 포함해 조회합니다. */
	@EntityGraph(attributePaths = "member")
	Page<Reservation> findAllByOrderByCreatedAtDesc(Pageable pageable);

	/** 달력에 표시할 수 있도록 활성 예약 날짜만 월 범위로 조회하고 개인정보를 반환하지 않습니다. */
	@Query("select distinct r.scheduledDate from Reservation r where r.scheduledDate between :startDate and :endDate and r.status in :statuses")
	List<LocalDate> findOccupiedDatesBetween(@Param("startDate") LocalDate startDate, @Param("endDate") LocalDate endDate,
		@Param("statuses") Collection<ReservationStatus> statuses);

	/** 날짜와 활성 상태를 함께 검사해 거절·취소된 예약은 달력 마감에서 제외합니다. */
	boolean existsByScheduledDateAndStatusIn(LocalDate scheduledDate, Collection<ReservationStatus> statuses);
}
