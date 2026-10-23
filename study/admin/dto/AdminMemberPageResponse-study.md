# AdminMemberPageResponse 학습 문서

## 1. 파일 역할·계층·필요한 이유·사용 시점

[`AdminMemberPageResponse.java`](../../../src/main/java/com/cleaner/be/admin/dto/AdminMemberPageResponse.java)는 회원 목록 데이터와 페이지 이동에 필요한 메타데이터를 함께 담는 응답 DTO입니다. [`AdminService.getMembers`](../../../src/main/java/com/cleaner/be/admin/AdminService.java)가 AuthService의 `Page<Member>`를 변환할 때 생성합니다. 페이지별 목록을 반환해 대량 회원을 한 응답으로 보내지 않도록 합니다.

## 2. 전체 원본 코드

```java
package com.cleaner.be.admin.dto;

import java.util.List;

/** 관리자 회원 목록과 페이지 이동에 필요한 결과 건수 정보를 함께 반환합니다. */
public record AdminMemberPageResponse(
	List<AdminMemberResponse> content,
	int page,
	int size,
	long totalElements,
	int totalPages,
	boolean first,
	boolean last
) {
}
```

## 3. 줄 단위 구성과 Java 요소

- 1행은 관리자 DTO 패키지입니다.
- 3행에서 `List`를 import해 회원 응답 여러 개를 담습니다.
- 5행의 JavaDoc은 목록과 페이지 정보를 한 결과로 제공한다고 설명합니다.
- 6행은 공개 record를 선언합니다.
- 7행 `List<AdminMemberResponse> content`는 현재 페이지의 회원 응답 목록입니다. 제네릭 `<...>`은 목록 요소의 타입을 지정합니다.
- 8행 `int page`와 9행 `int size`는 현재 페이지 번호와 페이지 크기입니다.
- 10행 `long totalElements`는 전체 결과 개수입니다. `long`은 int보다 큰 범위의 정수를 표현합니다.
- 11행 `int totalPages`는 전체 페이지 수입니다.
- 12~13행 `boolean first`, `boolean last`는 현재 페이지가 첫/마지막인지 나타냅니다.
- record는 선언된 구성 요소에 대한 생성자와 `content()`, `page()` 등의 접근자 메서드를 생성합니다.

## 4. 생성·입력·접근 방법

- 사용자 정의 메서드는 없습니다. record 생성자가 목록과 여섯 종류의 페이지 정보를 받습니다.
- 실제 AdminService는 `Page<Member>`의 각 항목을 `AdminMemberResponse`로 변환한 content와 `getNumber()`, `getSize()`, `getTotalElements()`, `getTotalPages()`, `isFirst()`, `isLast()` 결과를 전달합니다.
- 조회 실패나 잘못된 페이지 입력의 예외는 이 DTO가 아니라 Controller 검증, Spring Data, 서비스/저장소 호출에서 발생할 수 있습니다.
- DTO 자체에는 content null 방지나 숫자 범위 검증이 없습니다.

## 5. 실제 실행 흐름

1. `GET /admin/members`가 AdminController에 도달합니다.
2. AdminService가 ID 내림차순 `PageRequest`로 회원 페이지를 조회합니다.
3. 각 엔티티를 `AdminMemberResponse`로 변환하고 Spring Data `Page`의 메타데이터를 복사합니다.
4. 이 record를 Controller 응답으로 돌려주고 프레임워크가 HTTP 응답으로 직렬화합니다.

## 6. 확인된 관계와 추론

- **확인됨:** `content`의 원소 타입은 `AdminMemberResponse`이며, AdminService가 이 DTO의 모든 record component를 구성해 반환합니다.
- **확인됨:** 기본 페이지 번호는 Controller가 0으로, 기본 크기는 10으로 설정되어 있습니다. Controller는 크기를 1~100으로 제한합니다.
- **확인됨:** AdminService는 회원을 `id` 내림차순으로 요청합니다.
- **추론:** UI가 `first`/`last`와 페이지 총계를 이용해 이전/다음 버튼을 활성화할 수 있습니다. 해당 사용 방식은 프론트엔드 코드에서 확인해야 합니다.

## 7. 코드에 실제 쓰이는 핵심 개념

- **페이지네이션:** 목록 전체 대신 작은 조각을 조회합니다.
- **제네릭 컬렉션:** `List<AdminMemberResponse>`는 지정한 응답 타입의 여러 항목을 담습니다.
- **Record DTO:** 데이터 구조를 간결하게 표현하고 명시적 getter 코드를 줄입니다.
- **페이지 메타데이터:** content 외에 전체 건수·페이지 수·첫/마지막 여부를 알려 UI가 이동 상태를 알 수 있습니다.

## 8. 동작에 충실한 예시

구조를 설명하는 예시는 `content`에 `AdminMemberResponse` 목록을 두고, `page=0`, `size=10`, `totalElements`, `totalPages`, `first`, `last`를 함께 전달하는 형태입니다. 실제 값은 조회 시점의 데이터베이스 결과에 따라 정해집니다. 따라서 임의의 총 회원 수나 응답 레코드를 고정 사실처럼 제시하지 않습니다.

## 9. 요약

AdminMemberPageResponse는 현재 페이지 회원 목록과 페이지 이동 정보를 묶는 응답 record입니다. AdminService가 회원 엔티티를 안전한 회원 DTO로 바꾼 뒤 Spring Data 페이지 정보를 복사해 만듭니다.
