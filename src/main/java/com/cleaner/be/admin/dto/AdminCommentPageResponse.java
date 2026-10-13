package com.cleaner.be.admin.dto;

import java.util.List;

public record AdminCommentPageResponse(
	List<AdminCommentResponse> content,
	int page,
	int size,
	long totalElements,
	int totalPages,
	boolean first,
	boolean last
) {
}
