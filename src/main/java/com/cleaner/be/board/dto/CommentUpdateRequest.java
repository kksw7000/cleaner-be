package com.cleaner.be.board.dto;

import jakarta.validation.constraints.NotBlank;

/** 댓글 수정 요청 본문이며 작성자와 대상 게시글은 URL 및 인증 정보로 결정합니다. */
public record CommentUpdateRequest(@NotBlank String content) {
}
