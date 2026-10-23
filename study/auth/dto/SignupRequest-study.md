# SignupRequest 학습 가이드

> 원본: [SignupRequest.java](../../../src/main/java/com/cleaner/be/auth/dto/SignupRequest.java)

## 1. 역할·레이어·작성 이유·사용 시점

회원가입 입력 DTO record입니다. 이메일·비밀번호·이름·전화번호 형식 제약을 선언합니다.

## 2. 전체 원본 코드

아래 코드는 현재 Java 원본을 생략 없이 그대로 수록했습니다.

```java
package com.cleaner.be.auth.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/** 회원가입 요청값을 검증합니다. 비밀번호 규칙은 비밀번호 필드에만 적용됩니다. */
public record SignupRequest(
	@NotBlank @Email @Size(max = 50) String email,
	@NotBlank
	@Size(min = 8, max = 30)
	@Pattern(
		regexp = "^(?=.*[!@#$%^&*(),.?\":{}|<>]).*$",
		message = "비밀번호에는 특수문자를 최소 1개 이상 포함해야 합니다."
	)
	String password,
	@NotBlank @Size(max = 30) String name,
	@NotBlank @Pattern(regexp = "^01[016789]-?\\d{3,4}-?\\d{4}$") String phoneNumber
) {
}
```

## 3. 줄별 구성과 문법 읽기

email: NotBlank/Email/최대50. password: NotBlank/8~30자와 지정 특수문자 최소 하나를 요구하는 Pattern 및 오류 문구. name: NotBlank/최대30. phoneNumber: NotBlank 및 `01[016789]` 휴대폰 형태, 하이픈은 선택입니다. record components가 생성자/accessor를 만듭니다. 정규화나 DB 중복은 여기서 하지 않습니다.

## 4. 메서드별 목적·입력·처리 순서·호출·결과·예외

명시 메서드 없음. Controller의 `@Valid`가 annotation 검증을 실행합니다. AuthService가 email/phone 정규화, 중복 검사, 해시를 담당합니다.

## 5. 실제 실행 흐름

JSON body→형식 검증→AuthService.signup→중복 검사/정규화/해시/저장 순입니다.

## 6. 확인된 관계와 추론의 구분

확인: AuthController.signup이 받고 AuthService.signup이 네 accessor를 읽습니다. SignupResponse가 성공 결과입니다. 하이픈 없는 번호도 정규식상 허용되고 서비스는 숫자만 저장합니다.

## 7. 사용된 Java·Spring 핵심 개념

Java record, Bean Validation, 정규식, request DTO와 서비스 규칙 분리를 사용합니다.

## 8. 코드에 충실한 예시

`{"email":"hana@example.com","password":"Study!234","name":"하나","phoneNumber":"010-1234-5678"}`는 형식 검사를 통과할 수 있지만 중복 여부는 서비스/DB 결과에 달립니다.

## 9. 요약

요청의 형식을 검증하고 유일성·정규화·암호화는 서비스와 DB에 맡깁니다.
