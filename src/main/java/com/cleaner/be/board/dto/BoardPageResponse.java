package com.cleaner.be.board.dto;

import java.util.List;

/** Spring Data 내부 Page 객체를 API 계약으로 노출하지 않기 위한 목록 페이징 응답입니다. */
public record BoardPageResponse(
	List<BoardListResponse> content,
	int page,
	int size,
	long totalElements,
	int totalPages,
	boolean first,
	boolean last
) {
}
