# SignupResponse 학습 가이드

> 원본: [SignupResponse.java](../../../src/main/java/com/cleaner/be/auth/dto/SignupResponse.java)

## 1. 역할·레이어·작성 이유·사용 시점

회원가입 성공 후 클라이언트에 반환하는 공개 정보 record입니다. 인증 비밀정보를 제외합니다.

## 2. 전체 원본 코드

아래 코드는 현재 Java 원본을 생략 없이 그대로 수록했습니다.

```java
package com.cleaner.be.auth.dto;

/** 회원가입 완료 후 비밀번호 등 인증 정보를 제외해 클라이언트에 반환합니다. */
public record SignupResponse(Long id, String email, String name, String phoneNumber) {
}
```

## 3. 줄별 구성과 문법 읽기

구성요소는 id/email/name/phoneNumber입니다. import·annotation·직접 메서드가 없고 record 자동 생성자를 사용합니다. password, OAuth token, role은 없습니다.

## 4. 메서드별 목적·입력·처리 순서·호출·결과·예외

명시 메서드 없음. record constructor/accessors가 값을 제공합니다.

## 5. 실제 실행 흐름

AuthService.signup이 저장된 Member에서 공개 필드를 꺼내 생성하고 AuthController가 201 body로 반환합니다.

## 6. 확인된 관계와 추론의 구분

확인: AuthService.signup 반환형과 AuthController.signup 결과입니다. Member 값을 사용하며 비밀번호 필드는 정의되어 있지 않습니다.

## 7. 사용된 Java·Spring 핵심 개념

Java record, immutable API DTO, 정보 최소화를 사용합니다.

## 8. 코드에 충실한 예시

성공 응답에는 id/email/name/phoneNumber가 담기고 password는 포함되지 않습니다. id는 저장 결과에 따라 결정됩니다.

## 9. 요약

회원가입 완료 정보를 필요한 공개 필드로 제한합니다.
