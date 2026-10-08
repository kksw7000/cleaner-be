package com.cleaner.be.mypage.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * 비밀번호 변경 시 현재 비밀번호 확인과 새 비밀번호 규칙 검증에 사용하는 요청값입니다.
 * 새 비밀번호 규칙은 회원가입과 동일하게 유지해 어느 경로로 가입해도 보안 수준이 달라지지 않게 합니다.
 */
public record ChangePasswordRequest(
	@NotBlank String currentPassword,
	@NotBlank
	@Size(min = 8, max = 30)
	@Pattern(
		regexp = "^(?=.*[!@#$%^&*(),.?\":{}|<>]).*$",
		message = "비밀번호에는 특수문자를 최소 1개 이상 포함해야 합니다."
	)
	String newPassword
) {
}
