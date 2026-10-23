# OAuthLoginRequest 학습 가이드

> 원본: [OAuthLoginRequest.java](../../../src/main/java/com/cleaner/be/auth/dto/OAuthLoginRequest.java)

## 1. 역할·레이어·작성 이유·사용 시점

OAuth 로그인 요청 본문에서 외부 제공자 Access Token을 받는 DTO record입니다.

## 2. 전체 원본 코드

아래 코드는 현재 Java 원본을 생략 없이 그대로 수록했습니다.

```java
package com.cleaner.be.auth.dto;

import jakarta.validation.constraints.NotBlank;

/** 외부 OAuth 제공자에서 발급한 비어 있지 않은 Access Token을 받습니다. */
public record OAuthLoginRequest(@NotBlank String accessToken) {
}
```

## 3. 줄별 구성과 문법 읽기

단일 record component `accessToken`에 `@NotBlank`를 적용합니다. record가 생성자와 accessor를 생성하고 실제 서명/토큰 검증은 verifier가 수행합니다. Controller의 `@Valid`가 검증을 활성화합니다.

## 4. 메서드별 목적·입력·처리 순서·호출·결과·예외

명시 메서드 없음. 자동 `accessToken()` accessor로 값을 읽습니다. 공백 검사 외 토큰 유효성/제공자 검증은 하지 않습니다.

## 5. 실제 실행 흐름

JSON 변환→NotBlank 검증→AuthController가 URL provider와 accessToken을 OAuthService에 넘깁니다.

## 6. 확인된 관계와 추론의 구분

확인: AuthController.oauthLogin이 body를 받고 accessToken accessor를 호출합니다. provider는 URL 경로 변수에 있습니다.

## 7. 사용된 Java·Spring 핵심 개념

Java record, Spring `@RequestBody`/`@Valid`, Jakarta `@NotBlank`, OAuth 토큰 전달을 사용합니다.

## 8. 코드에 충실한 예시

`/api/auth/oauth/google` 본문에 `{"accessToken":"<provider-token>"}`이 들어오면 두 값이 서비스에 전달됩니다. 실제 토큰은 노출하지 않습니다.

## 9. 요약

외부 토큰 하나를 입력 타입으로 표현하고 빈 값만 DTO 단계에서 막습니다.
