package com.cleaner.be.auth;

public interface OAuthTokenVerifier {
	boolean supports(String provider);
	OAuthUserInfo verify(String accessToken);
}
