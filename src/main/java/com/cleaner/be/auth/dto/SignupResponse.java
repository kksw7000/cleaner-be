package com.cleaner.be.auth.dto;

/** 회원가입 완료 후 비밀번호 등 인증 정보를 제외해 클라이언트에 반환합니다. */
public record SignupResponse(Long id, String email, String name, String phoneNumber) {
}
