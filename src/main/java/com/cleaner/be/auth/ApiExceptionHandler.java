package com.cleaner.be.auth;

import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class ApiExceptionHandler {

	// 서비스의 중복 이메일 예외를 API 응답 규격인 409 Conflict로 변환합니다.
	@ExceptionHandler(DuplicateEmailException.class)
	@ResponseStatus(HttpStatus.CONFLICT)
	public Map<String, String> handleDuplicateEmail() {
		return Map.of("message", "이미 가입된 이메일입니다.");
	}

	@ExceptionHandler(DuplicatePhoneNumberException.class)
	@ResponseStatus(HttpStatus.CONFLICT)
	public Map<String, String> handleDuplicatePhoneNumber() {
		return Map.of("message", "이미 가입된 휴대폰번호입니다.");
	}

	/** 이메일 또는 비밀번호가 틀렸을 때 401과 공통 메시지를 반환합니다. */
	@ExceptionHandler(InvalidCredentialsException.class)
	@ResponseStatus(HttpStatus.UNAUTHORIZED)
	public Map<String, String> handleInvalidCredentials() {
		return Map.of("message", "이메일 또는 비밀번호가 올바르지 않습니다.");
	}
}
