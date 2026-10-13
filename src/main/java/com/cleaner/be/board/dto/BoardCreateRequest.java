package com.cleaner.be.board.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** 작성자 정보는 받지 않고 제목·본문과 관리자의 공지 지정 여부만 받는 게시글 생성 요청입니다. */
public record BoardCreateRequest(
	@NotBlank @Size(max = 100) String title,
	@NotBlank String content,
	boolean notice
) {
}
