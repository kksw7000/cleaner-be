# MemberRepository 학습 가이드

> 원본: [MemberRepository.java](../../src/main/java/com/cleaner/be/auth/MemberRepository.java)

## 1. 역할·레이어·작성 이유·사용 시점

Spring Data JPA 저장소입니다. 회원 CRUD 및 이메일·전화·OAuth ID 조건 조회를 선언합니다.

## 2. 전체 원본 코드

아래 코드는 현재 Java 원본을 생략 없이 그대로 수록했습니다.

```java
package com.cleaner.be.auth;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

/** 회원 영속화와 가입·로그인·소셜 계정 연결에 필요한 조건 조회를 제공합니다. */
public interface MemberRepository extends JpaRepository<Member, Long> {

	/** 회원가입과 로그인 전에 표준화된 이메일로 기존 계정을 조회합니다. */
	Optional<Member> findByEmail(String email);

	/** 가입·프로필 수정 시 전화번호 중복을 확인합니다. */
	Optional<Member> findByPhoneNumber(String phoneNumber);

	/** 같은 제공자 계정으로 재로그인할 회원을 찾아 기존 연결을 유지합니다. */
	Optional<Member> findByOauthProviderAndOauthProviderUserId(String oauthProvider, String oauthProviderUserId);
}
```

## 3. 줄별 구성과 문법 읽기

`Optional`은 결과 없음 가능성을, `JpaRepository<Member,Long>`은 엔티티와 ID 타입을 지정합니다. interface이므로 직접 구현은 없고 Spring Data가 메서드 이름에서 조회 쿼리를 생성합니다. `findByEmail`, `findByPhoneNumber`는 단일 조건, `findByOauthProviderAndOauthProviderUserId`는 AND 조건입니다.

## 4. 메서드별 목적·입력·처리 순서·호출·결과·예외

`findByEmail(String)`, `findByPhoneNumber(String)`, `findByOauthProviderAndOauthProviderUserId(String,String)`는 각각 조건에 맞는 `Optional<Member>`를 반환합니다. 명시 예외 처리는 없습니다. 상속한 JpaRepository가 save/delete/findById/findAll(Pageable) 등을 제공합니다.

## 5. 실제 실행 흐름

서비스 메서드 호출→Spring Data JPA 구현체→DB 조회/저장→엔티티 또는 Optional 반환 순입니다.

## 6. 확인된 관계와 추론의 구분

확인: Member가 엔티티이고 AuthService/OAuthService/JwtAuthenticationFilter가 조회/저장/삭제에 사용합니다. 실제 SQL·인덱스 계획은 DB와 JPA 구현에 달립니다.

## 7. 사용된 Java·Spring 핵심 개념

Repository pattern, Spring Data JPA, generic types, Optional, derived query method, CRUD/Pageable을 사용합니다.

## 8. 코드에 충실한 예시

`findByEmail("hana@example.com")`은 있으면 회원 Optional, 없으면 `Optional.empty()`를 반환하고 서비스가 적절한 분기를 정합니다.

## 9. 요약

명시적인 저장소 계약으로 서비스가 DB 검색 조건을 표현하게 합니다.
