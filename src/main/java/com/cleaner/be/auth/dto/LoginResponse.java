package com.cleaner.be.auth.dto;

/** 로그인 성공 응답입니다. 보안을 위해 비밀번호는 담지 않습니다. */
public record LoginResponse(Long id, String email, String name, String phoneNumber) {
}
