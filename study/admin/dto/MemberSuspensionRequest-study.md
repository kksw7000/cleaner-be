# MemberSuspensionRequest 학습 문서

## 1. 파일 역할·계층·필요한 이유·사용 시점

[`MemberSuspensionRequest.java`](../../../src/main/java/com/cleaner/be/admin/dto/MemberSuspensionRequest.java)는 회원 계정 정지/해제 요청 본문 DTO입니다. 관리자 API의 `PATCH /admin/members/{id}/suspension` 요청을 [`AdminController`](../../../src/main/java/com/cleaner/be/admin/AdminController.java)가 받으면 이 객체로 변환합니다. 요청 JSON 구조와 내부 상태 변경 호출 사이의 전달 형식을 명확히 하는 데 사용합니다.

## 2. 전체 원본 코드

```java
package com.cleaner.be.admin.dto;

/** 계정 정지와 해제를 하나의 상태 변경 API로 처리하기 위한 요청입니다. */
public record MemberSuspensionRequest(boolean suspended) {
}
```

## 3. 줄 단위 구성과 Java 요소

- 1행은 관리자 DTO 패키지를 선언합니다.
- 3행의 JavaDoc은 이 요청이 정지와 해제를 동일한 API에서 처리함을 설명합니다.
- 4행은 공개 record 선언입니다. `boolean suspended`가 요청의 유일한 데이터입니다.
- Record는 생성자 `MemberSuspensionRequest(boolean suspended)`와 접근자 `suspended()` 등을 자동으로 제공합니다.
- `boolean`은 primitive 타입이므로 null 대신 true/false 상태를 나타냅니다.

## 4. 생성 및 접근 방식

- 별도 메서드 구현이나 생성자 구현은 없습니다. record가 생성하는 canonical constructor에 boolean 값을 전달합니다.
- `suspended()`는 정지 여부를 반환합니다. true는 정지, false는 해제를 요청합니다.
- `AdminController.updateMemberSuspension`이 이 접근자를 호출해 서비스로 전달합니다.

## 5. 실제 실행 흐름

1. 관리자 요청이 SecurityConfig의 `/admin/**` 권한 검사를 통과합니다.
2. Spring MVC가 본문을 `MemberSuspensionRequest`로 바인딩합니다.
3. Controller가 `request.suspended()`를 읽고 `AdminService.updateMemberSuspension(id, value)`를 호출합니다.
4. AdminService가 AuthService에 상태 변경을 위임하고 응답 DTO를 반환합니다.

## 6. 확인된 관계와 추론

- **확인됨:** 이 요청 타입을 AdminController가 사용하며, 값은 AdminService와 AuthService로 이어집니다.
- **확인됨:** AuthService는 대상 회원을 찾은 후 Member의 `updateSuspension`을 호출합니다. 대상이 없으면 `InvalidCredentialsException`을 던집니다.
- **확인됨:** 이 record에는 별도 검증 애너테이션이 없습니다. JSON 누락 처리나 변환 오류는 실제 Spring/Jackson 설정에 달려 있습니다.
- **추론:** 관리자 화면에 true/false 토글이 있을 수 있지만, 프론트엔드 구현은 여기서 확인하지 않았습니다.

## 7. 코드에 실제 쓰이는 핵심 개념

- **Record DTO:** 상태 값을 전달하기 위한 간결한 불변 데이터 구조입니다.
- **기본형 boolean:** null을 보유하지 않는 두 상태 값입니다.
- **상태 변경 요청과 도메인 행위의 분리:** DTO는 값만 운반하며, 회원을 정지시키는 정책은 AuthService/Member가 처리합니다.

## 8. 동작에 충실한 예시

요청 본문 `{"suspended": true}`는 정지를 요청하고 `request.suspended()`가 true를 반환합니다. `{"suspended": false}`는 해제를 요청합니다. 대상 회원이 존재하고 관리자 권한 검사가 통과한 경우에만 실제 변경이 완료됩니다.

## 9. 요약

MemberSuspensionRequest는 정지 상태 한 개를 전달하는 record입니다. 관리자 Controller가 이 값을 읽어 서비스에 넘기며, DTO 자체는 회원을 찾거나 변경하지 않습니다.
