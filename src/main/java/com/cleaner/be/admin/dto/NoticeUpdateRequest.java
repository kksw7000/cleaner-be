package com.cleaner.be.admin.dto;

/** 관리자가 게시글을 목록 상단 공지로 지정하거나 해제할 때 사용하는 요청입니다. */
public record NoticeUpdateRequest(boolean notice) {
}
