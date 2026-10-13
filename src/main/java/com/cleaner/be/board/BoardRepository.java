package com.cleaner.be.board;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface BoardRepository extends JpaRepository<Board, Long> {

	@EntityGraph(attributePaths = "author")
	Page<Board> findByTitleContainingIgnoreCaseOrContentContainingIgnoreCase(String titleKeyword, String contentKeyword,
		Pageable pageable);

	@Override
	@EntityGraph(attributePaths = "author")
	Page<Board> findAll(Pageable pageable);
}
