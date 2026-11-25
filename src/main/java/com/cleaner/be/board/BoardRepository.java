package com.cleaner.be.board;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import jakarta.persistence.LockModeType;

/** 게시글의 기본 CRUD와 제목 검색을 담당하며, 응답 변환에 필요한 작성자를 함께 조회합니다. */
public interface BoardRepository extends JpaRepository<Board, Long> {

	/** 제목에 검색어가 포함된 게시글만 대소문자 구분 없이 페이지 단위로 찾습니다. */
	@EntityGraph(attributePaths = "author")
	Page<Board> findByTitleContainingIgnoreCase(String titleKeyword, Pageable pageable);

	/** 작성자 정보도 함께 읽어 목록 응답 변환 중 추가 조회가 발생하지 않게 합니다. */
	@Override
	@EntityGraph(attributePaths = "author")
	Page<Board> findAll(Pageable pageable);

	/** 같은 게시글을 동시에 조회하는 요청이 방문 기록을 중복 생성하지 않도록 게시글 행을 잠급니다. */
	@Lock(LockModeType.PESSIMISTIC_WRITE)
	@Query("select board from Board board where board.id = :id")
	java.util.Optional<Board> findByIdForViewUpdate(@Param("id") Long id);
}
