package com.cleaner.be.mypage.dto;

/**
 * 로그인한 회원이 마이페이지에서 표시할 수 있는 공개 프로필 정보입니다.
 * 비밀번호 해시나 소셜 로그인 식별자처럼 계정 보안에 영향을 주는 값은 포함하지 않습니다.
 */
public record MyPageResponse(Long id, String email, String name, String phoneNumber) {
}
