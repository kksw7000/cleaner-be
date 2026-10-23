# ChangePasswordRequest 학습 가이드

## 1. 역할·레이어·왜 필요한가·언제 동작하는가

`ChangePasswordRequest`는 비밀번호 변경 API의 입력을 표현하는 **요청 DTO(record)**입니다. 클라이언트가 보내는 현재 비밀번호와 새 비밀번호를 전달하고, 새 비밀번호의 형식을 선언적으로 검증합니다. [`MyPageController`](../../../src/main/java/com/cleaner/be/mypage/MyPageController.java)의 `PATCH /api/mypage/password`에서 JSON을 이 타입으로 변환하며 `@Valid`가 검증을 요청합니다.

요청 DTO를 엔티티와 분리하면 API 입력값과 DB 모델을 독립적으로 관리할 수 있습니다. 특히 비밀번호 입력이 어떤 형식인지 명확하게 표현하고, 잘못된 요청이 서비스 작업에 도달하지 않도록 돕습니다.

## 2. 현재 전체 소스

아래는 [ChangePasswordRequest.java](../../../src/main/java/com/cleaner/be/mypage/dto/ChangePasswordRequest.java)의 전체 내용입니다.

```java
package com.cleaner.be.mypage.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * 비밀번호 변경 시 현재 비밀번호 확인과 새 비밀번호 규칙 검증에 사용하는 요청값입니다.
 * 새 비밀번호 규칙은 회원가입과 동일하게 유지해 어느 경로로 가입해도 보안 수준이 달라지지 않게 합니다.
 */
public record ChangePasswordRequest(
	@NotBlank String currentPassword,
	@NotBlank
	@Size(min = 8, max = 30)
	@Pattern(
		regexp = "^(?=.*[!@#$%^&*(),.?\":{}|<>]).*$",
		message = "비밀번호에는 특수문자를 최소 1개 이상 포함해야 합니다."
	)
	String newPassword
) {
}
```

## 3. import·어노테이션·필드·메서드·키워드 설명

- `package com.cleaner.be.mypage.dto`는 DTO 패키지에 속함을 나타냅니다.
- `NotBlank`, `Pattern`, `Size`는 Jakarta Bean Validation 제약 어노테이션입니다.
- `public`은 다른 패키지에서도 사용할 수 있게 합니다. `record`는 데이터 전달에 적합한 Java 레코드 문법으로, 컴포넌트에 대한 접근자와 생성자를 자동으로 제공합니다.
- `currentPassword`와 `newPassword`는 record 컴포넌트이며 DTO의 데이터 구성 요소입니다. 각각 `currentPassword()`와 `newPassword()` 형태의 접근자를 자동으로 생성합니다.
- `@NotBlank`는 값이 `null`, 빈 문자열 또는 공백만 있는 문자열이면 유효하지 않도록 합니다.
- 새 비밀번호의 `@Size(min = 8, max = 30)`은 길이를 8~30자로 제한합니다. `@Pattern`은 정규식과 일치하는지 검사하며, 정규식의 positive lookahead `(?=...)`가 특수문자 집합의 문자가 적어도 하나 존재하는지 검사합니다. `.*`는 앞뒤의 임의 문자열을 허용합니다.
- `message`는 패턴 제약 실패 시 제공할 한국어 안내 문구입니다. 이 문구가 실제 HTTP 응답에 노출되는 방식은 예외 처리 구성에 따릅니다.
- `)`로 컴포넌트 목록을 닫고 `{}`는 record 본문입니다. 본문에는 사용자 정의 생성자나 메서드가 없습니다.

## 4. 각 메서드의 목적·입력·순서·호출·결과·예외

이 record에는 직접 작성된 메서드가 없습니다. Java가 생성한 canonical constructor는 두 문자열을 받아 객체를 만들고, 접근자는 각각 입력값을 돌려줍니다. 호출 쪽은 [`MyPageService.changePassword`](../../../src/main/java/com/cleaner/be/mypage/MyPageService.java)의 `request.currentPassword()`와 `request.newPassword()`입니다.

검증은 Controller의 `@Valid`가 활성화된 경계에서 요청 DTO에 적용됩니다. `currentPassword`가 비어 있거나 새 비밀번호가 비어 있으면 `@NotBlank` 위반입니다. 새 비밀번호 길이가 8~30이 아니면 `@Size` 위반, 특수문자가 없으면 `@Pattern` 위반입니다. 여기에는 별도 checked exception 선언이 없으며, 검증 실패는 프레임워크 검증 예외 흐름으로 처리됩니다.

## 5. 실제 실행 흐름

클라이언트의 JSON이 `{"currentPassword":"Old!pass123","newPassword":"New!pass123"}`라면 Spring MVC가 이를 record 인스턴스로 바꿉니다. `@Valid` 검증이 제약을 확인한 후 Controller가 현재 회원 ID와 DTO를 서비스에 전달합니다. 서비스는 현재 비밀번호를 BCrypt 해시와 비교하고, 맞으면 새 비밀번호를 해시해 저장합니다. DTO 자체는 비밀번호를 저장하거나 암호화하지 않습니다.

## 6. 확인된 관계와 추론을 구분하기

**소스에서 확인됨:** 이 DTO를 Controller의 비밀번호 변경 메서드가 `@Valid @RequestBody`로 받습니다. 서비스가 currentPassword/newPassword 접근자를 호출합니다. 서비스는 현재 비밀번호 오류를 `InvalidCurrentPasswordException`으로 나타내며 [`GlobalExceptionHandler`](../../../src/main/java/com/cleaner/be/auth/GlobalExceptionHandler.java)는 이를 HTTP 400으로 변환합니다.

**합리적인 해석/추론:** 클래스 설명은 회원가입과 같은 새 비밀번호 규칙이라고 밝힙니다. 이 DTO만으로 두 경로의 제약이 실제 동일한지 증명할 수는 없습니다. 실제 규칙 일치는 회원가입 요청 DTO의 제약도 비교해야 확인할 수 있습니다.

## 7. 사용된 개념

- **DTO:** HTTP 입력 데이터를 별도 타입으로 표현합니다.
- **Java record:** 불변 데이터 전달 타입을 간결하게 선언합니다.
- **Bean Validation:** 입력값의 길이·공백·패턴 조건을 선언합니다.
- **정규식 lookahead:** 문자열 안에 특수문자가 하나 이상 포함되는지 검사합니다.
- **현재 비밀번호 재확인:** 이미 로그인한 사용자의 민감한 계정 변경을 추가 확인합니다. 실제 비교와 저장은 서비스가 맡습니다.

## 8. 동작에 맞는 예시

다음 예시에서 `currentPassword`는 비어 있지 않고, `newPassword`는 8자 이상이며 `!`가 포함되어 제약을 만족합니다(실제 현재 비밀번호 일치 여부는 서비스가 별도로 확인합니다).

```json
{"currentPassword":"Current!123","newPassword":"Next!pass123"}
```

다음 요청은 새 비밀번호에 특수문자가 없어 패턴 검증에 실패합니다.

```json
{"currentPassword":"Current!123","newPassword":"Nextpass123"}
```

## 9. 요약

`ChangePasswordRequest`는 현재 비밀번호와 새 비밀번호를 담는 요청 record입니다. 공백 금지, 새 비밀번호 8~30자, 지정 특수문자 포함 규칙을 표현합니다. 입력 검증만 담당하며 현재 비밀번호 비교와 해시 저장은 서비스가 수행합니다.
