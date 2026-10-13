package com.cleaner.be.board.dto;

import java.time.LocalDateTime;
import java.util.List;

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
