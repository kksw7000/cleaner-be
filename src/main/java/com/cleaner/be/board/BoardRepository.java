package com.cleaner.be.board;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

/** 게시글의 기본 CRUD와 제목·본문 검색을 담당하며, 응답 변환에 필요한 작성자를 함께 조회합니다. */
public interface BoardRepository extends JpaRepository<Board, Long> {

	/** 제목 또는 본문에 검색어가 포함된 게시글을 대소문자 구분 없이 페이지 단위로 찾습니다. */
	@EntityGraph(attributePaths = "author")
	Page<Board> findByTitleContainingIgnoreCaseOrContentContainingIgnoreCase(String titleKeyword, String contentKeyword,
		Pageable pageable);

	/** 작성자 정보도 함께 읽어 목록 응답 변환 중 추가 조회가 발생하지 않게 합니다. */
	@Override
	@EntityGraph(attributePaths = "author")
	Page<Board> findAll(Pageable pageable);
}
