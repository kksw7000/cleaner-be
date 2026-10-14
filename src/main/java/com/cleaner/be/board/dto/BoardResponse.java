package com.cleaner.be.board.dto;

import java.time.LocalDateTime;
import java.util.List;

/** 게시글 상세 API 응답으로, 작성자·조회수·생성 및 수정 시각과 댓글 목록을 함께 제공합니다. */
public record BoardResponse(
	Long id,
	String title,
	String content,
	long viewCount,
	boolean notice,
	Long authorId,
	String authorName,
	LocalDateTime createdAt,
	LocalDateTime updatedAt,
	List<CommentResponse> comments
) {
}
