-- 개발·테스트 프로필에서만 실행하며, 운영 기본 실행에는 포함하지 않습니다.
INSERT INTO members (email, password, name, phone_number, role, suspended)
VALUES
	('admin.seed@example.com', '$2a$10$ekgdRInHoE0dFk4Tiplb5.dLvp1iDFvy8ac.5h/tVb3o5joPYKaWe', '테스트 관리자', '01000000001', 'ADMIN', false),
	('user.seed@example.com', '$2a$10$ekgdRInHoE0dFk4Tiplb5.dLvp1iDFvy8ac.5h/tVb3o5joPYKaWe', '테스트 회원', '01000000002', 'USER', false)
ON CONFLICT DO NOTHING;

INSERT INTO boards (title, content, view_count, notice, author_id, created_at, updated_at)
SELECT '[테스트] 관리자 공지', '관리자 권한과 공지 목록을 확인하기 위한 시드 게시글입니다.', 0, true, member.id, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
FROM members member
WHERE member.email = 'admin.seed@example.com'
	AND NOT EXISTS (SELECT 1 FROM boards WHERE title = '[테스트] 관리자 공지');

INSERT INTO boards (title, content, view_count, notice, author_id, created_at, updated_at)
SELECT '[테스트] 일반 회원 게시글', '게시글 조회·수정·삭제 흐름을 확인하기 위한 시드 게시글입니다.', 0, false, member.id, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
FROM members member
WHERE member.email = 'user.seed@example.com'
	AND NOT EXISTS (SELECT 1 FROM boards WHERE title = '[테스트] 일반 회원 게시글');

INSERT INTO comments (content, board_id, author_id, created_at, updated_at)
SELECT '게시글 상세의 댓글 조회를 확인하기 위한 시드 댓글입니다.', board.id, member.id, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
FROM boards board
JOIN members member ON member.email = 'user.seed@example.com'
WHERE board.title = '[테스트] 관리자 공지'
	AND NOT EXISTS (
		SELECT 1 FROM comments
		WHERE board_id = board.id AND content = '게시글 상세의 댓글 조회를 확인하기 위한 시드 댓글입니다.'
	);