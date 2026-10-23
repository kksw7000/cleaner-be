# LoginResponse 학습 가이드

> 원본: [LoginResponse.java](../../../src/main/java/com/cleaner/be/auth/dto/LoginResponse.java)

## 1. 역할·레이어·작성 이유·사용 시점

로그인 성공 회원을 식별하는 응답형 record이며 비밀번호는 포함하지 않습니다.

## 2. 전체 원본 코드

아래 코드는 현재 Java 원본을 생략 없이 그대로 수록했습니다.

```java
package com.cleaner.be.auth.dto;

/** 인증 서비스 내부에서 로그인 회원을 식별하며, 보안을 위해 비밀번호는 담지 않습니다. */
public record LoginResponse(Long id, String email, String name, String phoneNumber) {
}
```

## 3. 줄별 구성과 문법 읽기

구성요소는 `Long id`와 세 String(email/name/phoneNumber)입니다. import, annotation, 추가 필드/메서드는 없습니다. record가 생성자와 accessor를 만듭니다.

## 4. 메서드별 목적·입력·처리 순서·호출·결과·예외

명시 메서드 없음. 자동 accessor가 필드를 반환합니다. validation이나 예외 동작은 없습니다.

## 5. 실제 실행 흐름

AuthService.login이 인증 후 생성하고 AuthController는 id만 읽어 회원 재조회 후 토큰 발급에 사용합니다. 이 DTO 자체는 현재 Controller의 HTTP body가 아닙니다.

## 6. 확인된 관계와 추론의 구분

확인: AuthService.login의 반환형이며 Controller가 `id()`를 사용합니다. SignupResponse와 같은 공개 필드 모양입니다.

## 7. 사용된 Java·Spring 핵심 개념

Java record, 불변 DTO, 자동 accessor, 민감 데이터 최소화를 사용합니다.

## 8. 코드에 충실한 예시

`new LoginResponse(12L,"hana@example.com","하나","01012345678")`에는 password 필드가 없습니다.

## 9. 요약

로그인 회원 정보를 비밀번호 없이 전달하는 내부 결과 DTO입니다.
