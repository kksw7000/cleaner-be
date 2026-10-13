package com.cleaner.be.auth;

/** 정지된 계정의 로그인·토큰 갱신을 중단하기 위한 도메인 예외입니다. */
public class SuspendedMemberException extends RuntimeException {
	public SuspendedMemberException() {
		super("정지된 계정입니다.");
	}
}
