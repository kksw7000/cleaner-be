package com.cleaner.be.admin.dto;

import java.util.List;

/** 관리자 회원 목록과 페이지 이동에 필요한 결과 건수 정보를 함께 반환합니다. */
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
