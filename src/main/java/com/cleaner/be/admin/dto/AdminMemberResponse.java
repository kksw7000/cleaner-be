package com.cleaner.be.admin.dto;

import com.cleaner.be.auth.Role;

/** 비밀번호·OAuth 식별자는 제외하고 회원 관리에 필요한 공개 정보만 반환합니다. */
public record AdminMemberResponse(
	Long id,
	String email,
	String name,
	String phoneNumber,
	Role role,
	boolean suspended
) {
}
