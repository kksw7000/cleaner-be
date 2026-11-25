# BoardView.java 학습 노트

## 역할
게시글별 방문자 키를 DB에 한 번 저장해 상세 페이지 새로고침이 조회수를 반복해서 올리지 않도록 하는 JPA 엔티티입니다.

## 저장 규칙
- `board_id`와 `viewer_key` 조합은 `uk_board_views_board_viewer` 고유 제약으로 중복을 막습니다.
- 로그인 사용자는 `member:<회원 ID>`, 비로그인 사용자는 `visitor:<UUID>` 형식으로 저장합니다.
- 게시글 삭제 시 방문 기록도 DB 외래 키의 `ON DELETE CASCADE`로 정리합니다.
- 테이블은 `V9__track_unique_board_views.sql` 마이그레이션에서 생성됩니다.

## 원본
[BoardView.java](../../src/main/java/com/cleaner/be/board/BoardView.java)

