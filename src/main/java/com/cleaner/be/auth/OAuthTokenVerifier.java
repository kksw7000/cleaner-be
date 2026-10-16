package com.cleaner.be.auth;

/** 제공자별 OAuth 검증 구현이 공통 방식으로 사용자 정보를 반환하도록 하는 계약입니다. */
public interface OAuthTokenVerifier {
	/** 정규화된 제공자 이름을 해당 구현이 처리할 수 있는지 확인합니다. */
	boolean supports(String provider);
	/** 외부 Access Token을 검증하고 회원 연결에 필요한 신뢰된 정보를 반환합니다. */
	OAuthUserInfo verify(String accessToken);
}
