# LoginRequest 학습 가이드

> 원본: [LoginRequest.java](../../../src/main/java/com/cleaner/be/auth/dto/LoginRequest.java)

## 1. 역할·레이어·작성 이유·사용 시점

로그인 API 입력 DTO record입니다. JSON 이메일과 비밀번호를 받고 Controller의 `@Valid` 검사를 적용합니다.

## 2. 전체 원본 코드

아래 코드는 현재 Java 원본을 생략 없이 그대로 수록했습니다.

```java
package com.cleaner.be.auth.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** 로그인 요청에서 받을 이메일(아이디)과 비밀번호입니다. */
public record LoginRequest(
	@NotBlank @Email @Size(max = 50) String email,
	@NotBlank @Size(min = 8, max = 30) String password
) {
}
```

## 3. 줄별 구성과 문법 읽기

`@Email`, `@NotBlank`, `@Size`는 Jakarta Validation 제약입니다. email은 공백 불가·이메일 형식·최대 50자, password는 공백 불가·8~30자입니다. record가 constructor와 `email()`, `password()` accessor를 생성합니다.

## 4. 메서드별 목적·입력·처리 순서·호출·결과·예외

명시 메서드 없음. record가 생성자/accessors를 제공하고, validation은 Controller의 `@Valid` 경계에서 실행됩니다. 제약 위반은 MVC validation 오류이지 DTO의 직접 예외 처리가 아닙니다.

## 5. 실제 실행 흐름

JSON 역직렬화→DTO 검증→성공하면 AuthService.login에 전달됩니다.

## 6. 확인된 관계와 추론의 구분

확인: AuthController가 `@RequestBody @Valid`로 받고 AuthService가 두 accessor를 읽습니다. JSON 속성명은 record component와 같습니다.

## 7. 사용된 Java·Spring 핵심 개념

Java record, DTO, Bean Validation, Spring MVC JSON binding을 사용합니다.

## 8. 코드에 충실한 예시

`{"email":"hana@example.com","password":"study!234"}`는 형식/길이 요건을 만족할 수 있습니다. 빈 이메일이나 짧은 비밀번호는 서비스 호출 전에 거부됩니다.

## 9. 요약

입력 필드와 기본 로그인 요청 형식만 선언하며 자격 증명 진위는 서비스가 판단합니다.
