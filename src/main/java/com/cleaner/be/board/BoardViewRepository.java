package com.cleaner.be.board;

import org.springframework.data.jpa.repository.JpaRepository;

/** 게시글별 방문자 기록의 존재 여부와 저장을 담당합니다. */
public interface BoardViewRepository extends JpaRepository<BoardView, Long> {

	/** 방문자가 해당 게시글을 이미 조회했는지 확인합니다. */
	boolean existsByBoardIdAndViewerKey(Long boardId, String viewerKey);
}
