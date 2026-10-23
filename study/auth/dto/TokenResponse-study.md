# TokenResponse 학습 가이드

> 원본: [TokenResponse.java](../../../src/main/java/com/cleaner/be/auth/dto/TokenResponse.java)

## 1. 역할·레이어·작성 이유·사용 시점

로그인/OAuth/갱신에서 클라이언트로 보낼 Access Token 및 메타데이터 DTO record입니다.

## 2. 전체 원본 코드

아래 코드는 현재 Java 원본을 생략 없이 그대로 수록했습니다.

```java
package com.cleaner.be.auth.dto;

/** 클라이언트 localStorage에 저장할 로그인 결과입니다. Refresh Token은 HTTP 전용 쿠키로 전달합니다. */
public record TokenResponse(
	String accessToken,
	String tokenType,
	long accessTokenExpiresIn
) {
}
```

## 3. 줄별 구성과 문법 읽기

`accessToken`, `tokenType`, `accessTokenExpiresIn`(초 단위 long) 3개 record component가 자동 constructor/accessors를 만듭니다. Refresh Token component는 없습니다.

## 4. 메서드별 목적·입력·처리 순서·호출·결과·예외

명시 메서드 없음. compiler-generated accessor만 사용하며 발급·만료 계산은 JwtTokenProvider가 수행합니다.

## 5. 실제 실행 흐름

JwtTokenProvider가 생성해 IssuedTokens에 넣고 AuthController가 body에 설정합니다. Refresh Token은 Set-Cookie로 따로 보냅니다.

## 6. 확인된 관계와 추론의 구분

확인: IssuedTokens component, JwtTokenProvider.issueTokens 생성 및 AuthController response body입니다. Refresh Token은 이 타입에 포함되지 않습니다.

## 7. 사용된 Java·Spring 핵심 개념

Java record, API 응답 DTO, Bearer type, 초 단위 만료 표현, body/cookie 분리를 사용합니다.

## 8. 코드에 충실한 예시

응답 body는 accessToken, `Bearer`, 만료 초를 가질 수 있고 Refresh Token은 Set-Cookie 헤더에 전달됩니다. 실제 값은 생략합니다.

## 9. 요약

클라이언트용 Access Token 정보만 담아 Refresh Token 노출을 분리합니다.
