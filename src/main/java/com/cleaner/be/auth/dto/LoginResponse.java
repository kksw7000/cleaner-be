package com.cleaner.be.auth.dto;

/** 인증 서비스 내부에서 로그인 회원을 식별하며, 보안을 위해 비밀번호는 담지 않습니다. */
public record LoginResponse(Long id, String email, String name, String phoneNumber) {
}
