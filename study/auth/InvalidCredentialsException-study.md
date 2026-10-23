# InvalidCredentialsException 학습 가이드

> 원본: [InvalidCredentialsException.java](../../src/main/java/com/cleaner/be/auth/InvalidCredentialsException.java)

## 1. 역할·레이어·작성 이유·사용 시점

로그인 정보 또는 토큰으로 회원 인증을 할 수 없는 상황을 공통 표현하는 도메인 예외입니다.

## 2. 전체 원본 코드

아래 코드는 현재 Java 원본을 생략 없이 그대로 수록했습니다.

```java
package com.cleaner.be.auth;

/** 로그인용 이메일 또는 비밀번호가 일치하지 않을 때 발생합니다. */
public class InvalidCredentialsException extends RuntimeException {
}
```

## 3. 줄별 구성과 문법 읽기

`RuntimeException`을 상속하고 추가 import·필드·생성자·메서드는 없습니다. 구체적인 원인 메시지를 저장하지 않는 기본 예외 타입입니다.

## 4. 메서드별 목적·입력·처리 순서·호출·결과·예외

명시 메서드 없음. 서비스/컨트롤러의 기본 생성자로 발생하며 GlobalExceptionHandler가 인증 오류를 401과 공통 메시지로 변환합니다. 필터의 잘못된 JWT는 이 예외 대신 `sendError`를 직접 호출합니다.

## 5. 실제 실행 흐름

이메일 없음·비밀번호 불일치·회원 ID 없음·잘못된 Refresh Token 등에서 예외→MVC handler가 401 처리 순입니다.

## 6. 확인된 관계와 추론의 구분

확인: AuthService, AuthController가 사용하고 handler가 처리합니다. JwtAuthenticationFilter는 직접 401을 기록합니다. 같은 로그인 메시지는 계정 존재 여부를 구분하지 않습니다.

## 7. 사용된 Java·Spring 핵심 개념

unchecked exception, 인증 오류와 인가 오류 구분, Spring 예외 처리, 오류 정보 최소화를 사용합니다.

## 8. 코드에 충실한 예시

없는 이메일과 틀린 비밀번호는 모두 같은 인증 실패 응답이므로 응답만으로 계정 존재 여부를 구분할 수 없습니다.

## 9. 요약

다양한 자격 증명 실패를 공통 예외 타입으로 나타냅니다.
