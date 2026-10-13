package com.cleaner.be.admin.dto;

/** 계정 정지와 해제를 하나의 상태 변경 API로 처리하기 위한 요청입니다. */
public record MemberSuspensionRequest(boolean suspended) {
}
