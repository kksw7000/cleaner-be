-- 기존 예약의 서비스 분류 문자열을 새 enum 코드와 일치시킵니다.
UPDATE reservations
SET service_category = 'FABRIC'
WHERE service_category = 'SOFA_CARPET';
