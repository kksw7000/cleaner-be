package com.cleaner.be.board.dto;

import java.time.LocalDateTime;

public record CommentResponse(
	Long id,
	String content,
	Long authorId,
	String authorName,
	LocalDateTime createdAt,
	LocalDateTime updatedAt
) {
}
