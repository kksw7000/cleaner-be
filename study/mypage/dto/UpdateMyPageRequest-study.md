# UpdateMyPageRequest 학습 가이드

## 1. 역할·레이어·왜 필요한가·언제 동작하는가

`UpdateMyPageRequest`는 마이페이지의 이메일·이름·전화번호 수정을 위한 **요청 DTO(record)**입니다. 클라이언트 JSON을 Java 객체로 표현하고 입력값의 기본 형식을 검증합니다. [`MyPageController.updateMyPage`](../../../src/main/java/com/cleaner/be/mypage/MyPageController.java)가 PATCH `/api/mypage` 요청 본문으로 받으며, `@Valid`로 Bean Validation 검사를 활성화합니다.

요청 DTO를 사용하면 잘못된 형식의 값이 서비스에 도달하는 것을 줄이고, 수정 API가 받는 항목을 분명히 합니다. 회원 ID는 이 DTO에 없으므로 변경 대상은 요청 본문이 아니라 인증된 회원 ID로 정해집니다.

## 2. 현재 전체 소스

아래는 [UpdateMyPageRequest.java](../../../src/main/java/com/cleaner/be/mypage/dto/UpdateMyPageRequest.java)의 전체 내용입니다.

```java
package com.cleaner.be.mypage.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * 마이페이지 프로필 수정 요청값입니다.
 * 가입 때와 동일한 형식을 적용해 수정 경로로 잘못된 회원 데이터가 저장되는 것을 막습니다.
 */
public record UpdateMyPageRequest(
	@NotBlank @Email @Size(max = 50) String email,
	@NotBlank @Size(max = 30) String name,
	@NotBlank @Pattern(regexp = "^01[016789]-?\\d{3,4}-?\\d{4}$") String phoneNumber
) {
}
```

## 3. import·어노테이션·필드·메서드·키워드 설명

- `Email`, `NotBlank`, `Pattern`, `Size`는 Jakarta Bean Validation의 제약 어노테이션입니다.
- `public record`는 여러 패키지에서 사용할 수 있는 데이터 전달 타입을 선언합니다. 세 컴포넌트는 `email`, `name`, `phoneNumber`입니다. Java가 생성자와 접근자를 자동 생성합니다.
- 이메일에는 `@NotBlank`(공백/빈 값 금지), `@Email`(이메일 모양), `@Size(max = 50)`(최대 길이 50)을 적용합니다.
- 이름에는 공백/빈 값을 금지하고 최대 30자로 제한합니다.
- 전화번호에는 공백/빈 값을 금지하며 정규식 `^01[016789]-?\\d{3,4}-?\\d{4}$`를 적용합니다. Java 문자열의 `\\d`는 정규식에 `\d`를 전달해 숫자 한 자리를 뜻하게 합니다. `-?`는 하이픈이 없거나 하나 있을 수 있음을 뜻합니다. 번호 구간은 앞에서 `01`과 허용된 세 번째 숫자, 중간 3~4자리, 마지막 4자리 순서입니다.
- `^`와 `$`는 문자열 전체가 패턴에 맞는지 확인하는 시작/끝 경계입니다. 이 패턴은 한국 휴대전화 형식의 제한적인 모양을 확인할 뿐, 실제 번호의 소유자나 가입 가능 여부는 확인하지 않습니다.
- 이 클래스에 직접 작성된 메서드·필드는 없습니다. DTO 데이터 접근은 자동 생성된 `email()`, `name()`, `phoneNumber()`를 사용합니다.

## 4. 각 메서드의 목적·입력·순서·호출·결과·예외

직접 정의된 메서드는 없습니다. 자동 생성된 생성자는 세 문자열을 받아 요청 DTO를 만들고 각 접근자는 해당 값을 반환합니다.

Controller에서 요청 본문을 DTO로 변환한 후 `@Valid`가 어노테이션 규칙을 확인합니다. 이메일·이름·전화번호가 각각 제약을 어기면 서비스 호출 전에 검증 실패가 발생합니다. [`MyPageService.updateMyPage`](../../../src/main/java/com/cleaner/be/mypage/MyPageService.java)는 접근자를 호출해 값을 읽고, 이메일을 소문자로 바꾸고 공백을 제거하며 전화번호는 숫자만 남깁니다. 중복 검사는 서비스와 저장소가 담당합니다.

## 5. 실제 실행 흐름

1. 클라이언트가 PATCH `/api/mypage`에 JSON 본문을 보냅니다.
2. Spring MVC가 JSON을 `UpdateMyPageRequest` record로 만듭니다.
3. `@Valid`가 `NotBlank`, `Email`, `Size`, `Pattern` 조건을 검사합니다.
4. 검증에 통과하면 Controller가 인증된 회원 ID와 DTO를 서비스에 넘깁니다.
5. 서비스가 값을 정규화하고 이메일/전화번호 중복을 확인한 다음 엔티티를 수정합니다.
6. 성공하면 공개 프로필 응답 DTO가 반환됩니다.

## 6. 확인된 관계와 추론을 구분하기

**소스에서 확인됨:** Controller가 이 타입을 `@Valid @RequestBody`로 받습니다. 서비스가 `email()`, `name()`, `phoneNumber()`를 사용합니다. DTO에는 ID가 없고 세 프로필 항목만 있습니다. `Member`의 유일 제약은 DB에도 존재하지만, 요청 DTO 검증 그 자체는 DB 중복을 검사하지 않습니다.

**합리적인 해석/추론:** Java 문자열에 적용된 길이 제한의 실제 문자 단위는 Bean Validation 구현의 규칙을 따릅니다. 입력 형식 검증은 가능한 입력 모양을 제한하지만, DB 저장 가능 여부와 동시 요청 시의 충돌 문제까지 완전히 보장하지 않습니다. DB 고유 제약과 서비스의 중복 검사도 함께 관련됩니다.

## 7. 사용된 개념

- **요청 DTO:** 클라이언트가 보낼 수 있는 항목과 형식을 타입으로 정의합니다.
- **record:** 데이터 중심의 불변 객체를 간결하게 만듭니다.
- **Bean Validation:** 형식 제약을 어노테이션으로 선언하고 `@Valid`로 실행합니다.
- **정규식:** 전화번호 문자열의 허용 모양을 정의합니다.
- **입력 검증과 업무 규칙의 구분:** DTO는 모양을, 서비스는 회원 존재·중복 여부와 저장을 담당합니다.

## 8. 동작에 맞는 예시

다음은 형식 제약을 만족하는 예시 요청입니다. 이메일과 전화번호가 다른 회원과 중복되지 않는지는 서비스가 별도로 확인합니다.

```http
PATCH /api/mypage
Authorization: Bearer eyJ...유효한-Access-Token
Content-Type: application/json

{"email":"member@example.com","name":"김하늘","phoneNumber":"010-1234-5678"}
```

전화번호에 문자가 들어간 `010-12AB-5678`은 패턴에 맞지 않습니다. `name`이 공백뿐이거나 이메일 모양이 올바르지 않은 경우도 각각 해당 제약에 실패합니다.

## 9. 요약

`UpdateMyPageRequest`는 프로필 수정에 필요한 세 입력 필드와 형식 규칙을 담는 요청 record입니다. Controller에서 `@Valid`로 검사한 후 서비스가 정규화·중복 검사·저장을 수행합니다. 수정 대상 회원은 DTO가 아니라 인증 정보로 결정됩니다.
