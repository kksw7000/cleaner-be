package com.cleaner.be.auth.dto;

/** 클라이언트 localStorage에 저장할 로그인 결과입니다. Refresh Token은 HTTP 전용 쿠키로 전달합니다. */
public record TokenResponse(
	String accessToken,
	String tokenType,
	long accessTokenExpiresIn
) {
}
