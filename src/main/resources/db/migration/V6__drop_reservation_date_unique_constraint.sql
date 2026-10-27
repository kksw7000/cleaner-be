-- V5에서 활성 예약 인덱스 생성이 완료된 뒤 기존 전체 날짜 고유 제약을 제거합니다.
ALTER TABLE reservations DROP CONSTRAINT uk_reservations_scheduled_date;
