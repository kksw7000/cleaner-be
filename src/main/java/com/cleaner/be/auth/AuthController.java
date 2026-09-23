package com.cleaner.be.auth;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.cleaner.be.auth.dto.SignupRequest;
import com.cleaner.be.auth.dto.SignupResponse;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

	private final AuthService authService;

	public AuthController(AuthService authService) {
		this.authService = authService;
	}

	@PostMapping("/signup")
	@ResponseStatus(HttpStatus.CREATED)
	/**
	 * 회원가입 요청의 진입점입니다.
	 * {@code @Valid}가 이메일 형식, 비밀번호 길이, 이름의 필수 입력 여부를 먼저 검사한 뒤
	 * 검증을 통과한 요청만 서비스에 전달합니다.
	 */
	public SignupResponse signup(@Valid @RequestBody SignupRequest request) {
		return authService.signup(request);
	}

	
}