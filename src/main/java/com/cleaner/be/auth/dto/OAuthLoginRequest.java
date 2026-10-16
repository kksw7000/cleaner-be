package com.cleaner.be.auth.dto;

import jakarta.validation.constraints.NotBlank;

/** 외부 OAuth 제공자에서 발급한 비어 있지 않은 Access Token을 받습니다. */
public record OAuthLoginRequest(@NotBlank String accessToken) {
}
