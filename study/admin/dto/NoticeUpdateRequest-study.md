# NoticeUpdateRequest 학습 문서

## 1. 파일 역할·계층·필요한 이유·사용 시점

[`NoticeUpdateRequest.java`](../../../src/main/java/com/cleaner/be/admin/dto/NoticeUpdateRequest.java)는 공지 상태를 바꾸는 요청 본문을 표현하는 DTO(Data Transfer Object)입니다. Java `record`로 구성된 단순 데이터 타입이며, [`AdminController`](../../../src/main/java/com/cleaner/be/admin/AdminController.java)의 `PATCH /admin/boards/{id}/notice`가 JSON을 이 타입으로 받아 `notice` 값을 Service로 전달합니다.

## 2. 전체 원본 코드

```java
package com.cleaner.be.admin.dto;

/** 관리자가 게시글을 목록 상단 공지로 지정하거나 해제할 때 사용하는 요청입니다. */
public record NoticeUpdateRequest(boolean notice) {
}
```

## 3. 줄 단위 구성과 Java 요소

- 1행은 `com.cleaner.be.admin.dto` 패키지 선언입니다.
- 3행은 이 타입의 목적을 설명하는 클래스 문서 주석입니다.
- 4행의 `public`은 다른 패키지의 Controller에서 사용할 수 있도록 공개한다는 뜻입니다.
- `record NoticeUpdateRequest`는 데이터를 담는 record 선언입니다. 매개변수 목록의 `boolean notice`가 컴포넌트이며, 컴파일러가 생성자와 `notice()` 접근 메서드 등을 제공합니다.
- 5행은 본문이 추가로 없음을 나타내는 닫는 중괄호입니다.
- `boolean`은 참/거짓 값이며, 이 타입에는 추가 validation annotation이 없습니다.

## 4. 생성 및 접근 방식

- record의 정식 생성자는 `NoticeUpdateRequest(boolean notice)` 형태입니다. 별도의 사용자 작성 생성자나 메서드는 없습니다.
- `notice()`는 저장된 공지 여부를 반환합니다. Java record는 `getNotice()`가 아니라 컴포넌트 이름과 같은 접근자를 생성합니다.
- Spring MVC의 JSON 변환기가 요청 본문의 `notice` 값을 record 생성자에 전달합니다. 이 변환/예외 동작은 해당 애플리케이션의 Jackson/Spring 설정에 의존합니다.

## 5. 실제 실행 흐름

1. 클라이언트가 `/admin/boards/{id}/notice`로 PATCH 요청을 보냅니다.
2. AdminController의 `@RequestBody`가 본문을 이 DTO로 바인딩하고 `@Valid`를 적용합니다.
3. Controller가 `request.notice()`를 읽어 관리자 ID와 함께 AdminService에 전달합니다.
4. BoardService가 실제 공지 상태 변경과 관리자 역할 확인을 수행합니다.

## 6. 확인된 관계와 추론

- **확인됨:** DTO를 AdminController가 사용하고, 결과 boolean을 AdminService 및 BoardService의 공지 변경 경로에 전달합니다.
- **확인됨:** 이 record 자체에는 `@NotNull`, `@AssertTrue` 같은 검증 규칙이 없습니다. 기본형 boolean은 참/거짓 두 값 중 하나입니다.
- **추론:** 클라이언트는 JSON 키를 `notice`로 보내도록 설계되었다고 볼 수 있습니다. 실제 직렬화/역직렬화 설정은 별도 확인 대상입니다.

## 7. 코드에 실제 쓰이는 핵심 개념

- **DTO:** HTTP 입력을 업무 객체와 분리해 전달합니다.
- **Record:** 값 중심 데이터를 간결하게 선언하며, 컴포넌트에 대한 생성자와 접근자 등을 자동 제공합니다.
- **Primitive boolean:** null을 표현할 수 없는 참/거짓 타입입니다. 따라서 이 DTO에서는 키가 생략될 때의 역직렬화 동작을 단순히 “필수 검증된다”고 단정할 수 없습니다.

## 8. 동작에 충실한 예시

본문 예시는 `{"notice": true}`입니다. 바인딩된 DTO에서 `request.notice()`는 `true`이고, 관리자는 게시글을 공지로 지정하도록 요청한 것입니다. `{"notice": false}`는 공지 해제 요청입니다. 실제 성공 여부는 게시글 존재와 관리자 권한 등 서비스 조건에 달려 있습니다.

## 9. 요약

NoticeUpdateRequest는 공지 여부 하나만 담는 요청 record입니다. 입력을 구조화할 뿐 공지 변경이나 권한 판단은 하지 않으며, Controller가 읽은 값을 서비스 계층에 전달합니다.
