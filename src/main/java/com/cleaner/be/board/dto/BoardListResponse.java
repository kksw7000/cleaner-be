package com.cleaner.be.board.dto;

import java.time.LocalDateTime;

/** 목록 화면에 필요한 게시글 요약 정보이며 본문과 댓글은 상세 응답에서 제공합니다. */
public record BoardListResponse(
	Long id,
	String title,
	boolean notice,
	Long authorId,
	String authorName,
	long viewCount,
	LocalDateTime createdAt
) {
}
