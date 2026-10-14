package com.cleaner.be.board;

import java.util.List;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

/** 댓글 CRUD와 게시글별 댓글 목록 조회를 담당합니다. */
public interface CommentRepository extends JpaRepository<Comment, Long> {

	/** 상세 응답에서 작성자 정보가 필요하므로 작성자와 함께 생성 시간 오름차순으로 조회합니다. */
	@EntityGraph(attributePaths = "author")
	List<Comment> findByBoardIdOrderByCreatedAtAsc(Long boardId);

}
