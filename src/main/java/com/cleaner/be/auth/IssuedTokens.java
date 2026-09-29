package com.cleaner.be.auth;

import com.cleaner.be.auth.dto.TokenResponse;

/** 내부에서만 사용하는 토큰 쌍이며, API 응답에는 accessTokenResponse만 직렬화합니다. */
public record IssuedTokens(TokenResponse accessTokenResponse, String refreshToken) {
}
