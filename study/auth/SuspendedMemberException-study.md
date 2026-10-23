# SuspendedMemberException 학습 가이드

> 원본: [SuspendedMemberException.java](../../src/main/java/com/cleaner/be/auth/SuspendedMemberException.java)

## 1. 역할·레이어·작성 이유·사용 시점

정지 계정의 로그인·토큰 갱신을 거부하는 도메인 예외입니다.

## 2. 전체 원본 코드

아래 코드는 현재 Java 원본을 생략 없이 그대로 수록했습니다.

```java
package com.cleaner.be.auth;

/** 정지된 계정의 로그인·토큰 갱신을 중단하기 위한 도메인 예외입니다. */
public class SuspendedMemberException extends RuntimeException {
	public SuspendedMemberException() {
		super("정지된 계정입니다.");
	}
}
```

## 3. 줄별 구성과 문법 읽기

RuntimeException을 상속합니다. 명시한 기본 생성자가 `super`를 호출하여 `정지된 계정입니다.` 메시지를 부모 예외에 저장합니다. 별도 필드/메서드는 없습니다.

## 4. 메서드별 목적·입력·처리 순서·호출·결과·예외

`SuspendedMemberException()`: 입력 없이 부모 생성자에 메시지를 전달합니다. 반환값은 없으며 `getMessage()`가 이 문구를 제공합니다. AuthService/OAuthService가 던지고 전역 handler가 403을 응답합니다. JWT 필터는 직접 403을 기록합니다.

## 5. 실제 실행 흐름

서비스에서 회원 정지 확인→예외 발생→GlobalExceptionHandler가 메시지와 403으로 변환합니다. 필터에서는 sendError 경로입니다.

## 6. 확인된 관계와 추론의 구분

확인: AuthService/OAuthService 및 GlobalExceptionHandler와 연결되며 JwtAuthenticationFilter도 DB의 정지 상태를 확인합니다. 기존 토큰도 다음 요청에 정지 여부를 확인하는 흐름입니다.

## 7. 사용된 Java·Spring 핵심 개념

Java 상속, `super` 생성자 호출, unchecked exception, HTTP 403 Forbidden을 사용합니다.

## 8. 코드에 충실한 예시

정지 계정의 인증 시도는 `{ "message": "정지된 계정입니다." }`와 403 상태로 거절됩니다.

## 9. 요약

정지 상태의 거부 사유를 일관된 예외와 메시지로 전달합니다.
