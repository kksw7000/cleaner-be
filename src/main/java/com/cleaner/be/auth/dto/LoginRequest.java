package com.cleaner.be.auth.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** 로그인 요청에서 받을 이메일(아이디)과 비밀번호입니다. */
public record LoginRequest(
	@NotBlank @Email @Size(max = 50) String email,
	@NotBlank @Size(min = 8, max = 30) String password
) {
}
