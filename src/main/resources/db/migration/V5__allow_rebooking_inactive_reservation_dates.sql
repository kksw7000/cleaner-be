-- 기존 전체 날짜 고유 제약은 거절·취소된 예약도 재예약을 막으므로 활성 상태에만 적용합니다.
ALTER TABLE reservations DROP CONSTRAINT uk_reservations_scheduled_date;

-- PENDING 또는 APPROVED 예약만 날짜당 하나로 제한하며 상태 전환과 동시 생성에도 DB가 보장합니다.
CREATE UNIQUE INDEX uk_reservations_active_scheduled_date
	ON reservations (scheduled_date)
	WHERE status IN ('PENDING', 'APPROVED');
