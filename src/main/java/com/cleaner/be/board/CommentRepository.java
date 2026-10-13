package com.cleaner.be.board;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CommentRepository extends JpaRepository<Comment, Long> {

	@EntityGraph(attributePaths = "author")
	List<Comment> findByBoardIdOrderByCreatedAtAsc(Long boardId);

	@Override
	@EntityGraph(attributePaths = {"author", "board"})
	Page<Comment> findAll(Pageable pageable);
}
