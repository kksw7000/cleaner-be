# Role 학습 가이드

> 원본: [Role.java](../../src/main/java/com/cleaner/be/auth/Role.java)

## 1. 역할·레이어·작성 이유·사용 시점

회원 권한을 제한된 값으로 표현하는 enum입니다. 인증/인가가 USER와 ADMIN을 공통 사용합니다.

## 2. 전체 원본 코드

아래 코드는 현재 Java 원본을 생략 없이 그대로 수록했습니다.

```java
package com.cleaner.be.auth;

/** 게시글·댓글 관리 권한을 구분하는 회원 역할입니다. */
public enum Role {
	USER,
	ADMIN
}
```

## 3. 줄별 구성과 문법 읽기

`enum Role` 본문은 `USER`, `ADMIN` 상수로 구성됩니다. import, 별도 필드·생성자·메서드는 없습니다. Member에서 기본 USER로 설정하고 JPA가 이름 문자열로 저장합니다.

## 4. 메서드별 목적·입력·처리 순서·호출·결과·예외

명시 메서드 없음. Java enum의 `values()`/`valueOf(String)`을 사용할 수 있습니다. 이 클래스는 문자열 입력의 변환이나 권한 검증을 수행하지 않습니다.

## 5. 실제 실행 흐름

Member의 역할 기본값→필터가 `name()`으로 ROLE_ authority 생성→SecurityConfig의 관리자 인가 검사 순입니다.

## 6. 확인된 관계와 추론의 구분

확인: Member의 role 타입이고 JwtAuthenticationFilter와 `hasRole("ADMIN")`에 연결됩니다. 관리자 역할 부여 경로는 이 파일에서 확인되지 않습니다.

## 7. 사용된 Java·Spring 핵심 개념

Java enum은 제한된 상수집합이며 `name()`이 상수명을 돌려줍니다. JPA 문자열 저장 설정은 Member에 있습니다.

## 8. 코드에 충실한 예시

`Role.ADMIN`은 정의된 값입니다. `Role.valueOf("admin")`은 대소문자 자동 보정이 없어 실패합니다.

## 9. 요약

권한을 임의 문자열 대신 두 가지 타입 안전 값으로 제한합니다.
