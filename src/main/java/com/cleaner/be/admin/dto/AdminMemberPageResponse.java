package com.cleaner.be.admin.dto;

import java.util.List;

public record AdminMemberPageResponse(
	List<AdminMemberResponse> content,
	int page,
	int size,
	long totalElements,
	int totalPages,
	boolean first,
	boolean last
) {
}
