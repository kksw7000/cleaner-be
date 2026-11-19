package com.cleaner.be.auth.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/** 회원가입 요청값을 검증합니다. 비밀번호 규칙은 비밀번호 필드에만 적용됩니다. */
public record SignupRequest(
	@NotBlank(message = "이메일을 입력해 주세요.")
	@Email(message = "올바른 이메일 형식을 입력해 주세요.")
	@Size(max = 50, message = "이메일은 50자 이내로 입력해 주세요.")
	String email,
	@NotBlank(message = "비밀번호를 입력해 주세요.")
	@Size(min = 8, max = 30, message = "비밀번호는 8~30자로 입력해 주세요.")
	@Pattern(
		regexp = "^(?=.*[!@#$%^&*(),.?\":{}|<>]).*$",
		message = "비밀번호에는 특수문자를 최소 1개 이상 포함해야 합니다."
	)
	String password,
	@NotBlank(message = "닉네임을 입력해 주세요.")
	@Size(max = 30, message = "닉네임은 30자 이내로 입력해 주세요.")
	String name,
	@NotBlank(message = "전화번호를 입력해 주세요.")
	@Pattern(regexp = "^01[016789]-?\\d{3,4}-?\\d{4}$", message = "올바른 휴대전화 번호를 입력해 주세요.")
	String phoneNumber
) {
}
