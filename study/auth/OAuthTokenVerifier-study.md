# OAuthTokenVerifier 학습 가이드

> 원본: [OAuthTokenVerifier.java](../../src/main/java/com/cleaner/be/auth/OAuthTokenVerifier.java)

## 1. 역할·레이어·작성 이유·사용 시점

제공자별 OAuth 검증기가 구현할 interface 계약입니다. 외부 Access Token 처리 방식을 OAuthService와 분리합니다.

## 2. 전체 원본 코드

아래 코드는 현재 Java 원본을 생략 없이 그대로 수록했습니다.

```java
package com.cleaner.be.auth;

/** 제공자별 OAuth 검증 구현이 공통 방식으로 사용자 정보를 반환하도록 하는 계약입니다. */
public interface OAuthTokenVerifier {
	/** 정규화된 제공자 이름을 해당 구현이 처리할 수 있는지 확인합니다. */
	boolean supports(String provider);
	/** 외부 Access Token을 검증하고 회원 연결에 필요한 신뢰된 정보를 반환합니다. */
	OAuthUserInfo verify(String accessToken);
}
```

## 3. 줄별 구성과 문법 읽기

import 없이 interface를 선언합니다. `supports(String)`는 제공자 처리 가능 여부를 boolean으로 표현합니다. `verify(String)`은 검증 토큰을 입력받아 `OAuthUserInfo`를 반환하도록 요구합니다. 본문이 없는 것은 의도한 추상 메서드입니다.

## 4. 메서드별 목적·입력·처리 순서·호출·결과·예외

`supports(provider)`: 구현체가 provider를 지원하는지 반환. `verify(accessToken)`: 토큰을 검증해 사용자 정보를 반환. 두 메서드는 여기서 구현/예외 정책을 제공하지 않으며 각 구현체 책임입니다.

## 5. 실제 실행 흐름

OAuthService가 verifier 목록에서 supports가 true인 객체를 찾고 verify를 호출한 뒤 결과를 회원 처리에 사용합니다.

## 6. 확인된 관계와 추론의 구분

확인: OAuthService가 interface를 주입받아 두 메서드를 호출하며 반환형은 OAuthUserInfo입니다. 이 auth Java 트리에서는 구현체가 검색되지 않았습니다. 구현체 Bean이 외부에 제공되는지는 별도 확인 대상입니다.

## 7. 사용된 Java·Spring 핵심 개념

Java interface, 추상 메서드, 다형성, 계약과 구현 분리를 사용합니다.

## 8. 코드에 충실한 예시

구현체는 `supports("google")`을 true로 하고 검증 성공 시 OAuthUserInfo를 반환할 수 있습니다. 실제 제공자 연결 구현은 이 파일에 없습니다.

## 9. 요약

제공자별 OAuth 처리의 공통 계약만 정의합니다.
