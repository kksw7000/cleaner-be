# OAuthUserInfo 학습 가이드

> 원본: [OAuthUserInfo.java](../../src/main/java/com/cleaner/be/auth/OAuthUserInfo.java)

## 1. 역할·레이어·작성 이유·사용 시점

토큰 검증 후 OAuthService로 전달되는 불변 사용자 식별 데이터 record입니다.

## 2. 전체 원본 코드

아래 코드는 현재 Java 원본을 생략 없이 그대로 수록했습니다.

```java
package com.cleaner.be.auth;

/** 제공자 토큰 검증 후 OAuthService가 회원 조회·생성에 사용하는 신뢰된 식별 정보입니다. */
public record OAuthUserInfo(String providerUserId, String email, String name) {
}
```

## 3. 줄별 구성과 문법 읽기

`OAuthUserInfo(providerUserId,email,name)` 세 record component가 생성자와 세 accessor를 자동 생성합니다. 필드·명시 메서드·검증 로직은 없습니다.

## 4. 메서드별 목적·입력·처리 순서·호출·결과·예외

명시 메서드 없음. 컴파일러가 `providerUserId()`, `email()`, `name()` 및 record 기본 메서드를 생성합니다.

## 5. 실제 실행 흐름

OAuth verifier가 인스턴스를 반환하고 OAuthService가 accessor로 필수값 검사 및 회원 연결에 사용합니다.

## 6. 확인된 관계와 추론의 구분

확인: OAuthTokenVerifier.verify의 반환형이고 OAuthService가 세 accessor를 호출합니다. OAuth profile 전체가 아닌 회원 연결에 필요한 정보만 전달합니다.

## 7. 사용된 Java·Spring 핵심 개념

Java record, 불변 데이터 전달 객체, 자동 accessor를 사용합니다.

## 8. 코드에 충실한 예시

`new OAuthUserInfo("provider-id", "hana@example.com", "하나")`와 같이 만들고 서비스가 `email()`을 읽습니다. 신뢰 여부는 verifier의 검증 성공에 달립니다.

## 9. 요약

OAuth 사용자 식별에 필요한 세 값을 명시적 타입으로 전달합니다.
