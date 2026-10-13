package com.cleaner.be.board.dto;

import java.time.LocalDateTime;

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
