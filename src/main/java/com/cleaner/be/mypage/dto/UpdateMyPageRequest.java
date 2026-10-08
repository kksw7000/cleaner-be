package com.cleaner.be.mypage.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * 마이페이지 프로필 수정 요청값입니다.
 * 가입 때와 동일한 형식을 적용해 수정 경로로 잘못된 회원 데이터가 저장되는 것을 막습니다.
 */
public record UpdateMyPageRequest(
	@NotBlank @Email @Size(max = 50) String email,
	@NotBlank @Size(max = 30) String name,
	@NotBlank @Pattern(regexp = "^01[016789]-?\\d{3,4}-?\\d{4}$") String phoneNumber
) {
}
