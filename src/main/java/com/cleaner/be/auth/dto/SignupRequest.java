package com.cleaner.be.auth.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record SignupRequest(
	// 요청 본문을 받는 DTO이며, 각 애너테이션이 잘못된 입력을 400으로 차단합니다.
	@NotBlank @Email @Size(max = 50) String email,
	@NotBlank @Size(min = 8, max = 30) String password,
	@Pattern(
		regexp = "^(?=.*[!@#$%^&*(),.?\":{}|<>]).*$",
		message = "비밀번호는 특수문자를 최소 1개 이상 포함해야 합니다."
	)
	@NotBlank @Size(max = 30) String name,
	// 010-1234-5678 또는 01012345678 형식을 받습니다. DB에는 숫자만 저장합니다.
	@NotBlank @Pattern(regexp = "^01[016789]-?\\d{3,4}-?\\d{4}$") String phoneNumber
) {
}