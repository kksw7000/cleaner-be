# IssuedTokens 학습 가이드

> 원본: [IssuedTokens.java](../../src/main/java/com/cleaner/be/auth/IssuedTokens.java)

## 1. 역할·레이어·작성 이유·사용 시점

Access Token 응답 DTO와 Refresh Token 문자열을 함께 전달하는 서버 내부 record입니다. Controller가 둘을 본문과 쿠키로 각각 보낼 때 사용합니다.

## 2. 전체 원본 코드

아래 코드는 현재 Java 원본을 생략 없이 그대로 수록했습니다.

```java
package com.cleaner.be.auth;

import com.cleaner.be.auth.dto.TokenResponse;

/** 내부에서만 사용하는 토큰 쌍이며, API 응답에는 accessTokenResponse만 직렬화합니다. */
public record IssuedTokens(TokenResponse accessTokenResponse, String refreshToken) {
}
```

## 3. 줄별 구성과 문법 읽기

`TokenResponse` import는 Access Token 응답의 타입을 지정합니다. record 구성요소 두 개는 자동 생성 생성자와 `accessTokenResponse()`, `refreshToken()` 접근자를 제공합니다. 별도 메서드·필드는 없습니다.

## 4. 메서드별 목적·입력·처리 순서·호출·결과·예외

명시 메서드 없음. compiler-generated record constructor/accessors가 두 값을 전달합니다. 토큰 생성·검증은 JwtTokenProvider 책임입니다.

## 5. 실제 실행 흐름

JwtTokenProvider가 인스턴스를 만들고 AuthController가 두 accessor로 꺼내 각각 HTTP body와 Set-Cookie에 넣습니다.

## 6. 확인된 관계와 추론의 구분

확인: JwtTokenProvider.issueTokens가 생성하고 AuthController.tokenResponse가 읽으며 `TokenResponse`를 포함합니다. Refresh Token을 body에 노출하지 않는 분리는 코드 주석과 컨트롤러 사용에서 확인됩니다.

## 7. 사용된 Java·Spring 핵심 개념

Java record, 불변 데이터 전달, record accessor, 내부 결과와 외부 응답 분리를 사용합니다.

## 8. 코드에 충실한 예시

`IssuedTokens(accessDto, refreshValue)`를 받은 컨트롤러는 `accessTokenResponse()`를 body에, `refreshToken()`을 쿠키에 넣습니다. 예시 값은 자리표시자입니다.

## 9. 요약

두 토큰 값을 묶어 전달하되 최종 응답 위치는 서로 분리합니다.
