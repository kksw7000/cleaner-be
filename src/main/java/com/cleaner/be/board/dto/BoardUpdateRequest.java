package com.cleaner.be.board.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** 수정 시에도 작성자는 변경할 수 없으며 공지 지정은 서비스에서 ADMIN 여부를 추가 확인합니다. */
public record BoardUpdateRequest(
	@NotBlank @Size(max = 100) String title,
	@NotBlank String content,
	boolean notice
) {
}
