package com.cleaner.be.admin.dto;

import java.time.LocalDateTime;

/** 관리자 목록에서 원문 게시글과 작성자를 함께 식별할 수 있는 댓글 응답입니다. */
public record AdminCommentResponse(
	Long id,
	Long boardId,
	String content,
	Long authorId,
	String authorName,
	LocalDateTime createdAt,
	LocalDateTime updatedAt
) {
}
