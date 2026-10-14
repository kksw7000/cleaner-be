package com.cleaner.be.board.dto;

import java.time.LocalDateTime;

/** 댓글 API 응답으로, 작성자 표시 정보와 생성·수정 시각을 포함합니다. */
public record CommentResponse(
	Long id,
	String content,
	Long authorId,
	String authorName,
	LocalDateTime createdAt,
	LocalDateTime updatedAt
) {
}
