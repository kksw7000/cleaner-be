package com.cleaner.be.auth.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/** 회원가입 요청값을 검증합니다. 비밀번호 규칙은 비밀번호 필드에만 적용됩니다. */
public record SignupRequest(
	@NotBlank @Email @Size(max = 50) String email,
	@NotBlank
	@Size(min = 8, max = 30)
	@Pattern(
		regexp = "^(?=.*[!@#$%^&*(),.?\":{}|<>]).*$",
		message = "비밀번호에는 특수문자를 최소 1개 이상 포함해야 합니다."
	)
	String password,
	@NotBlank @Size(max = 30) String name,
	@NotBlank @Pattern(regexp = "^01[016789]-?\\d{3,4}-?\\d{4}$") String phoneNumber
) {
}
