package com.cleaner.be.board.dto;

import jakarta.validation.constraints.NotBlank;

/** 댓글 생성 요청 본문이며 공백만 있는 댓글은 검증 단계에서 거부합니다. */
public record CommentCreateRequest(@NotBlank String content) {
}
