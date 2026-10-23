# AdminMemberResponse 학습 문서

## 1. 파일 역할·계층·필요한 이유·사용 시점

[`AdminMemberResponse.java`](../../../src/main/java/com/cleaner/be/admin/dto/AdminMemberResponse.java)는 관리자 회원 목록과 회원 정지 상태 변경 결과에 사용하는 응답 DTO입니다. 내부 [`Member`](../../../src/main/java/com/cleaner/be/auth/Member.java) 엔티티를 그대로 반환하지 않고 회원 관리에 필요한 일부 정보만 전달하기 위해 사용합니다. [`AdminService`](../../../src/main/java/com/cleaner/be/admin/AdminService.java)가 엔티티에서 필드를 골라 새 응답 객체를 만듭니다.

## 2. 전체 원본 코드

```java
package com.cleaner.be.admin.dto;

import com.cleaner.be.auth.Role;

/** 관리자 회원 조회와 정지 상태 변경 응답으로 사용하며, 인증 정보는 제외하고 회원 관리에 필요한 정보만 반환합니다. */
public record AdminMemberResponse(
	Long id,
	String email,
	String name,
	String phoneNumber,
	Role role,
	boolean suspended
) {
}
```

## 3. 줄 단위 구성과 Java 요소

- 1행은 관리자 DTO 패키지를 선언합니다.
- 3행은 회원의 역할 enum을 가져옵니다.
- 5행의 JavaDoc은 응답의 목적과 인증 정보를 반환하지 않는 의도를 설명합니다.
- 6행은 공개 record 타입을 선언합니다.
- 7행 `Long id`는 회원 식별자입니다. 객체형 `Long`은 null을 표현할 수 있지만 정상 회원 응답에서는 ID가 존재해야 합니다.
- 8~10행의 `String`은 이메일, 이름, 전화번호입니다.
- 11행의 `Role`은 USER 또는 ADMIN과 같은 회원 역할 타입입니다.
- 12행 `boolean suspended`는 계정 정지 상태입니다.
- 컴파일러는 필드에 해당하는 record component, canonical constructor, 접근자, `equals`/`hashCode`/`toString`을 생성합니다.

## 4. 생성·입력·접근 방법

- 이 파일에 직접 구현된 사용자 정의 메서드는 없습니다.
- 생성자는 `AdminMemberResponse(id, email, name, phoneNumber, role, suspended)` 형태로 여섯 값을 받습니다.
- 접근자는 `id()`, `email()`, `name()`, `phoneNumber()`, `role()`, `suspended()`입니다.
- 실제 생성 지점은 `AdminService.toMemberResponse`이며 Member getter의 결과를 순서대로 전달합니다.
- record 선언 자체는 예외를 던지지 않습니다. 전달된 값이 유효한지 별도 검증하는 코드도 이 파일에는 없습니다.

## 5. 실제 실행 흐름

1. 관리자 회원 목록에서는 AuthService가 `Page<Member>`를 조회합니다.
2. AdminService가 각 Member에 `toMemberResponse` 변환을 적용합니다.
3. 각 DTO는 페이지 content에 들어가거나 정지 변경 응답으로 반환됩니다.
4. Spring MVC/Jackson이 반환 객체의 값을 HTTP 응답으로 직렬화합니다. 구체적인 JSON 포맷은 설정을 따릅니다.

## 6. 확인된 관계와 추론

- **확인됨:** AdminService가 `Member`에서 ID, 이메일, 이름, 전화번호, 역할, 정지 여부를 추출해 이 DTO를 생성합니다.
- **확인됨:** Member에는 비밀번호와 OAuth 정보도 있지만 응답 DTO에 포함되지 않습니다. 따라서 이 변환 경로는 해당 값을 전달하지 않습니다.
- **확인됨:** `Role` enum에는 `USER`, `ADMIN`이 선언되어 있습니다.
- **추론:** 프론트엔드는 이 응답으로 관리자 회원 목록을 표시할 수 있습니다. 실제 화면이나 JSON 필드 casing은 UI/직렬화 설정을 더 확인해야 합니다.

## 7. 코드에 실제 쓰이는 핵심 개념

- **응답 DTO:** 엔티티 구조와 HTTP 계약을 분리하고 반환 필드를 제한합니다.
- **Record:** 데이터를 담는 타입을 짧게 선언하고 읽기 전용 형태의 구성요소 접근자를 제공합니다.
- **Enum:** 역할을 임의 문자열 대신 정의된 상수 중 하나로 표현합니다.
- **보안 최소화:** 비밀번호 같은 민감한 인증 정보가 이 응답 구조에 없습니다. 다만 이메일/전화번호도 개인정보이므로 관리자 권한으로만 접근해야 합니다.

## 8. 동작에 충실한 예시

생성 형태는 `new AdminMemberResponse(member.getId(), member.getEmail(), member.getName(), member.getPhoneNumber(), member.getRole(), member.isSuspended())`입니다. 이 예시는 실제 코드의 필드 매핑과 같습니다. 실제 ID, 개인 정보, 역할 값은 데이터베이스에 따라 달라지므로 임의의 회원 데이터로 단정하지 않습니다.

## 9. 요약

AdminMemberResponse는 관리자에게 보여줄 회원 정보를 여섯 필드로 제한한 응답 record입니다. AdminService가 Member에서 필요한 값만 복사하며, 비밀번호 등 내부 인증 정보는 이 DTO에 들어가지 않습니다.
