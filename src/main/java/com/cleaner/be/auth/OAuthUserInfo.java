package com.cleaner.be.auth;

public record OAuthUserInfo(String providerUserId, String email, String name) {
}
