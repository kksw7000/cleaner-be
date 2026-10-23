# MyPageResponse 학습 가이드

## 1. 역할·레이어·왜 필요한가·언제 동작하는가

`MyPageResponse`는 마이페이지 API가 클라이언트에 돌려줄 프로필 데이터 모양을 나타내는 **응답 DTO(record)**입니다. 서비스가 엔티티에서 공개해도 되는 회원 식별자와 프로필 값을 골라 이 DTO를 만들며, Spring MVC가 이를 JSON 응답으로 직렬화합니다.

회원 엔티티에는 비밀번호 해시, OAuth 연결 정보 등 API 응답에 보이면 안 되는 정보가 더 있습니다. 응답 DTO를 따로 두면 API에 공개할 필드를 명시적으로 제한할 수 있습니다. 마이페이지 조회와 프로필 수정이 성공했을 때 사용됩니다.

## 2. 현재 전체 소스

아래는 [MyPageResponse.java](../../../src/main/java/com/cleaner/be/mypage/dto/MyPageResponse.java)의 전체 내용입니다.

```java
package com.cleaner.be.mypage.dto;

/**
 * 로그인한 회원이 마이페이지에서 표시할 수 있는 공개 프로필 정보입니다.
 * 비밀번호 해시나 소셜 로그인 식별자처럼 계정 보안에 영향을 주는 값은 포함하지 않습니다.
 */
public record MyPageResponse(Long id, String email, String name, String phoneNumber) {
}
```

## 3. import·어노테이션·필드·메서드·키워드 설명

- `package`는 DTO 패키지에 소속시킵니다. 외부 타입이나 라이브러리 import는 필요하지 않습니다.
- JavaDoc은 클래스가 API 응답용 공개 프로필 DTO임을 설명합니다.
- `public`은 서비스 계층을 포함한 다른 패키지에서 타입을 사용할 수 있게 합니다.
- `record`는 데이터를 전달하기 위한 간결한 Java 타입입니다. 괄호의 `id`, `email`, `name`, `phoneNumber`가 record 컴포넌트입니다.
- `Long`은 회원 ID를 나타내며 `null` 가능 객체형입니다. `String` 세 개는 이메일·이름·전화번호를 나타냅니다.
- Java가 컴포넌트에 대한 생성자와 `id()`, `email()`, `name()`, `phoneNumber()` 접근자를 생성합니다. 본문은 비어 있고 직접 작성한 메서드나 필드가 없습니다.
- record 컴포넌트는 생성 후 다시 대입할 수 없습니다. 다만 컴포넌트 타입이 참조형이라는 사실과 내부 객체의 변경 가능성은 별개입니다. 여기서는 `Long`과 `String`을 사용합니다.

## 4. 각 메서드의 목적·입력·순서·호출·결과·예외

직접 작성된 메서드는 없습니다. 자동 생성된 생성자는 네 컴포넌트 값을 받아 DTO를 만들고, 접근자는 각 값을 반환합니다.

[`MyPageService.toResponse`](../../../src/main/java/com/cleaner/be/mypage/MyPageService.java)는 `Member`의 `getId()`, `getEmail()`, `getName()`, `getPhoneNumber()`를 호출해 이 DTO를 만듭니다. `getMyPage`와 `updateMyPage`가 이를 Controller에 반환합니다. 이 클래스 자체는 조회·저장·검증·예외 처리를 하지 않습니다.

## 5. 실제 실행 흐름

서비스가 DB에서 읽은 `Member` 엔티티를 그대로 반환하지 않고 공개 필드 네 개를 DTO 생성자에 전달합니다. Controller가 DTO를 반환하면 Spring MVC의 메시지 변환기가 일반적으로 JSON으로 직렬화합니다. 결과 예시는 다음과 같습니다.

```json
{"id":42,"email":"member@example.com","name":"김하늘","phoneNumber":"01012345678"}
```

실제 필드값은 DB 데이터에 따라 달라집니다. 비밀번호와 OAuth 식별자는 생성자 인자에 포함되지 않으므로 이 응답 타입의 필드가 아닙니다.

## 6. 확인된 관계와 추론을 구분하기

**소스에서 확인됨:** [`MyPageService`](../../../src/main/java/com/cleaner/be/mypage/MyPageService.java)가 이 DTO를 생성하고 조회/프로필 수정 반환값으로 사용합니다. 생성자는 id, email, name, phoneNumber 네 값만 받습니다. `Member`의 password 및 OAuth 필드는 매핑 코드에 포함되어 있지 않습니다.

**합리적인 해석/추론:** Spring MVC의 기본 JSON 메시지 변환기가 등록된 정상적인 애플리케이션 설정에서 record 컴포넌트가 JSON 속성으로 직렬화됩니다. 구체적인 JSON 직렬화 설정이나 명명 정책은 프로젝트 설정에 좌우되지만, 이 DTO만으로 특정 클라이언트 UI 표시 방식까지 알 수는 없습니다.

## 7. 사용된 개념

- **응답 DTO:** 서버 내부 엔티티와 외부 API 응답 모델을 분리합니다.
- **최소 공개:** 클라이언트에 필요한 필드만 선택하여 민감한 필드 노출을 피합니다.
- **Java record:** 값 전달 타입을 짧고 읽기 쉽게 정의합니다.
- **JSON 직렬화:** Java 응답 객체를 HTTP 응답 본문으로 변환하는 과정입니다.

## 8. 동작에 맞는 예시

이 DTO를 만드는 코드의 모양은 다음과 같습니다. 실제 회원 값은 예시입니다.

```java
new MyPageResponse(42L, "member@example.com", "김하늘", "01012345678");
```

이 객체에는 `password`, `oauthProvider`, `oauthProviderUserId` 컴포넌트가 없습니다. 따라서 이 DTO 자체에는 그 값들을 담을 자리가 없습니다.

## 9. 요약

`MyPageResponse`는 마이페이지에서 공개할 회원 프로필 네 값만 담는 응답 DTO입니다. 서비스가 엔티티에서 선택한 값을 넣어 만들며 Controller를 통해 응답됩니다. 데이터 구조를 분리함으로써 내부 인증 정보를 API 응답 필드에 섞지 않습니다.
