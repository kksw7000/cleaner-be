package com.cleaner.be.admin.dto;

import com.cleaner.be.auth.Role;

/** 관리자 회원 조회와 정지 상태 변경 응답으로 사용하며, 인증 정보는 제외하고 회원 관리에 필요한 정보만 반환합니다. */
public record AdminMemberResponse(
	Long id,
	String email,
	String name,
	String phoneNumber,
	Role role,
	boolean suspended
) {
}
