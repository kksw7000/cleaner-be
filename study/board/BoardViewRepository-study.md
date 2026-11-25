# BoardViewRepository.java 학습 노트

## 역할
Spring Data JPA가 구현하는 저장소로, 게시글과 방문자 키의 조합이 이미 기록되었는지 조회하고 방문 기록을 저장합니다.

## 핵심 메서드
- `existsByBoardIdAndViewerKey(boardId, viewerKey)`: Spring Data가 메서드 이름에서 존재 여부 쿼리를 생성합니다.
- `save(BoardView)`: 최초 방문 기록을 저장합니다.

## 실행 흐름
`BoardService.getBoard`가 잠금 상태로 게시글을 조회한 뒤 이 저장소로 중복 여부를 확인합니다. 방문 기록이 없을 때만 저장과 조회수 증가를 함께 처리합니다.

## 원본
[BoardViewRepository.java](../../src/main/java/com/cleaner/be/board/BoardViewRepository.java)
