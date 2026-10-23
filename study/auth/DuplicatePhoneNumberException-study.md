# DuplicatePhoneNumberException 학습 가이드

> 원본: [DuplicatePhoneNumberException.java](../../src/main/java/com/cleaner/be/auth/DuplicatePhoneNumberException.java)

## 1. 역할·레이어·작성 이유·사용 시점

전화번호 중복으로 가입 또는 수정하려는 상황을 거부하는 도메인 예외입니다.

## 2. 전체 원본 코드

아래 코드는 현재 Java 원본을 생략 없이 그대로 수록했습니다.

```java
package com.cleaner.be.auth;

/** 이미 사용 중인 전화번호의 가입 또는 프로필 변경을 거부하기 위한 도메인 예외입니다. */
public class DuplicatePhoneNumberException extends RuntimeException {
}
```

## 3. 줄별 구성과 문법 읽기

import나 별도 필드 없이 `RuntimeException`을 상속한 최소 클래스입니다. 생성자와 메서드가 없으므로 기본 생성자가 암시적으로 제공되며 예외 메시지는 직접 설정하지 않습니다.

## 4. 메서드별 목적·입력·처리 순서·호출·결과·예외

명시 메서드 없음. AuthService가 표준화한 전화번호가 이미 등록된 것을 확인하면 기본 생성자로 예외를 던집니다. GlobalExceptionHandler가 409와 전화 중복 메시지로 변환합니다.

## 5. 실제 실행 흐름

서비스가 전화번호 조회→기존 계정 발견→예외 발생→전역 handler가 충돌 상태 반환 순입니다.

## 6. 확인된 관계와 추론의 구분

확인: `AuthService.signup`의 `findByPhoneNumber` 결과와 `GlobalExceptionHandler`가 연결됩니다. 주석의 프로필 변경 용례는 주석 수준이며 다른 경로 사용은 별도 확인이 필요합니다.

## 7. 사용된 Java·Spring 핵심 개념

unchecked 도메인 예외와 전역 HTTP 오류 변환을 사용합니다.

## 8. 코드에 충실한 예시

전화가 이미 등록되어 있으면 서비스가 이 예외를 던지고 클라이언트는 409 응답을 받습니다.

## 9. 요약

전화번호 중복 상황을 식별하는 타입이며 응답 문구는 handler 책임입니다.
