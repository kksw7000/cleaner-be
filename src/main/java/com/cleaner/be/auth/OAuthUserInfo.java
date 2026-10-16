package com.cleaner.be.auth;

/** 제공자 토큰 검증 후 OAuthService가 회원 조회·생성에 사용하는 신뢰된 식별 정보입니다. */
public record OAuthUserInfo(String providerUserId, String email, String name) {
}
