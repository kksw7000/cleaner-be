-- 정지 여부는 기본값 false로 추가해 기존 회원이 즉시 로그인 불가 상태가 되지 않도록 합니다.
ALTER TABLE members ADD COLUMN suspended boolean NOT NULL DEFAULT false;
