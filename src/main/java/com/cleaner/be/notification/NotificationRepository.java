package com.cleaner.be.notification;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

/** 회원 알림을 최신순으로 조회하고 현재 회원의 읽음 상태만 변경합니다. */
public interface NotificationRepository extends JpaRepository<Notification, Long> {

	/** 홈 화면의 알림 미리보기에 필요한 최신 세 건만 조회합니다. */
	List<Notification> findTop3ByMemberIdOrderByCreatedAtDescIdDesc(Long memberId);

	/** 보관 알림 페이지에서 회원 알림만 최신순으로 나눠 조회합니다. */
	Page<Notification> findByMemberIdOrderByCreatedAtDescIdDesc(Long memberId, Pageable pageable);

	/** 읽지 않은 알림 전체 개수를 계산해 홈 화면의 배지에 표시합니다. */
	long countByMemberIdAndReadAtIsNull(Long memberId);

	/** 다른 회원의 알림을 읽음 처리하지 않도록 알림 ID와 소유 회원 ID를 함께 확인합니다. */
	Optional<Notification> findByIdAndMemberId(Long id, Long memberId);

	/** 현재 회원의 미읽음 알림만 한 번에 읽음 처리하고 대상 행 수를 반환합니다. */
	@Modifying
	@Query("update Notification n set n.readAt = :readAt where n.member.id = :memberId and n.readAt is null")
	int markAllAsRead(@Param("memberId") Long memberId, @Param("readAt") LocalDateTime readAt);

	/** 회원 탈퇴와 테스트 정리에서 해당 회원 소유의 알림만 삭제합니다. */
	@Transactional
	void deleteByMemberId(Long memberId);
}
