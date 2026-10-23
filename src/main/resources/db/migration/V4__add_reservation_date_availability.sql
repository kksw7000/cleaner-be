-- 날짜 단위로 한 건의 예약만 허용하기 위해 기존 예약 시각에서 예약 날짜를 분리하고 고유 제약을 추가합니다.
ALTER TABLE reservations ADD COLUMN scheduled_date date;

-- 기존 데이터를 보존하면서 예약 시각의 날짜를 채워 새 제약을 적용할 수 있게 합니다.
UPDATE reservations SET scheduled_date = CAST(scheduled_at AS date);

ALTER TABLE reservations ALTER COLUMN scheduled_date SET NOT NULL;
-- 같은 날짜의 중복 예약은 동시 요청에서도 허용되지 않아야 하므로 DB 고유 제약으로 최종 차단합니다.
ALTER TABLE reservations ADD CONSTRAINT uk_reservations_scheduled_date UNIQUE (scheduled_date);

-- 달력 월 조회가 예약 전체를 조회하지 않도록 날짜와 상태 기준 인덱스를 추가합니다.
CREATE INDEX idx_reservations_scheduled_date_status ON reservations (scheduled_date, status);
