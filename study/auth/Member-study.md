# Member 학습 가이드

> 원본: [Member.java](../../src/main/java/com/cleaner/be/auth/Member.java)

## 1. 역할·레이어·작성 이유·사용 시점

JPA 회원 엔티티입니다. 인증·게시판 등에서 공유하며 계정 식별 정보, 역할, 정지 상태 및 프로필 변경 상태를 저장합니다.

## 2. 전체 원본 코드

아래 코드는 현재 Java 원본을 생략 없이 그대로 수록했습니다.

```java
package com.cleaner.be.auth;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/** 인증·게시판 기능이 공유하며 계정 식별 정보와 권한·정지 상태를 보관하는 회원 엔티티입니다. */
@Entity
@Table(name = "members")
public class Member {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	// DB의 unique 제약도 두어 동시에 가입 요청이 와도 이메일 중복을 방지합니다.
	@Column(nullable = false, unique = true, length = 50)
	private String email;

	// BCrypt 해시값을 저장하는 컬럼입니다. 원문 비밀번호를 넣으면 안 됩니다.
	@Column
	private String password;

	@Column(nullable = false, length = 30)
	private String name;

	// 숫자만 저장한 휴대폰번호입니다. 같은 번호로 중복 가입하지 못하게 합니다.
	@Column(unique = true, length = 11)
	private String phoneNumber;

	@Column(length = 30)
	private String oauthProvider;

	@Column(length = 100)
	private String oauthProviderUserId;

	// 게시판을 포함한 도메인별 권한 판단이 같은 기준을 사용하도록 회원에 역할을 보관합니다.
	@Column(nullable = false, length = 20)
	@jakarta.persistence.Enumerated(jakarta.persistence.EnumType.STRING)
	private Role role = Role.USER;

	// 정지 상태는 기존 Access Token을 가진 회원도 다음 요청에서 즉시 차단하기 위해 회원 정보에 보관합니다.
	@Column(nullable = false)
	private boolean suspended;

	protected Member() {
	}

	public Member(String email, String password, String name, String phoneNumber) {
		this.email = email;
		this.password = password;
		this.name = name;
		this.phoneNumber = phoneNumber;
	}

	/** 비밀번호 없이 외부 제공자 식별 정보로 소셜 회원을 생성합니다. */
	static Member fromOAuth(String email, String name, String oauthProvider, String oauthProviderUserId) {
		Member member = new Member();
		member.email = email;
		member.name = name;
		member.oauthProvider = oauthProvider;
		member.oauthProviderUserId = oauthProviderUserId;
		return member;
	}

	public Long getId() {
		return id;
	}

	public String getEmail() {
		return email;
	}

	/** 로그인 시 BCrypt 해시 비교에만 사용하며, API 응답에는 노출하지 않습니다. */
	public String getPassword() {
		return password;
	}

	public String getName() {
		return name;
	}

	public String getPhoneNumber() {
		return phoneNumber;
	}

	public String getOauthProvider() {
		return oauthProvider;
	}

	public String getOauthProviderUserId() {
		return oauthProviderUserId;
	}

	public Role getRole() {
		return role;
	}

	public boolean isSuspended() {
		return suspended;
	}

	/** 관리자가 계정을 정지 또는 해제할 때만 호출해 인증 흐름이 단일 상태 값을 확인하도록 합니다. */
	public void updateSuspension(boolean suspended) {
		this.suspended = suspended;
	}

	/**
	 * 마이페이지에서 본인이 수정한 공개 프로필만 반영합니다.
	 * 비밀번호와 소셜 로그인 연결 정보는 별도의 보안 절차가 필요한 값이므로 이 메서드에서 변경하지 않습니다.
	 */
	public void updateProfile(String email, String name, String phoneNumber) {
		this.email = email;
		this.name = name;
		this.phoneNumber = phoneNumber;
	}

	/**
	 * 현재 비밀번호 검증을 통과하고 BCrypt 해시로 변환된 값만 저장합니다.
	 * 원문 비밀번호를 엔티티에 전달하거나 저장하는 경로를 만들지 않기 위해 해시값만 받습니다.
	 */
	public void updatePassword(String encodedPassword) {
		this.password = encodedPassword;
	}
}
```

## 3. 줄별 구성과 문법 읽기

Jakarta Persistence import와 `@Entity`, `@Table(name="members")`가 DB 매핑을 선언합니다. id는 IDENTITY 기본키, email은 필수 unique 최대 50, password는 hash 문자열, name은 필수, phoneNumber는 unique, OAuth 값은 nullable 컬럼입니다. Role은 `EnumType.STRING`으로 저장되고 USER 기본값, suspended는 필수 boolean입니다. protected 기본 생성자는 JPA용이고 공개 생성자는 일반 가입용입니다. package-private static `fromOAuth`는 OAuth 회원을 만듭니다. getter는 상태 조회, update 메서드는 상태 변경입니다.

## 4. 메서드별 목적·입력·처리 순서·호출·결과·예외

`Member()`: JPA용 protected constructor. `Member(email,password,name,phone)`: 일반 회원 기본 필드 초기화(검증/해시는 호출자 책임). `fromOAuth(...)`: 비밀번호 없이 이메일/이름/provider ID로 생성. 9개 getter/boolean `isSuspended`: 대응 값 반환. `updateSuspension(boolean)`: 정지 상태 변경. `updateProfile(email,name,phone)`: 프로필 세 필드 변경(중복검사/정규화 없음). `updatePassword(encodedPassword)`: 이미 해시된 값으로 비밀번호 변경.

## 5. 실제 실행 흐름

AuthService가 검증·해시 뒤 일반 생성자로 만들고 저장합니다. OAuthService는 static factory를 씁니다. JPA 조회 후 서비스가 업데이트한 값은 트랜잭션에서 저장됩니다.

## 6. 확인된 관계와 추론의 구분

확인: MemberRepository의 Entity, AuthService/OAuthService/JWT filter가 사용하며 Role과 연결됩니다. MyPageService가 `updatePassword`를 사용합니다. 컬럼 unique 선언이 존재합니다. 실제 DB 제약 적용은 JPA DDL 설정에 달려 있습니다.

## 7. 사용된 Java·Spring 핵심 개념

JPA Entity/ORM, 기본키, column 제약, Enum STRING 매핑, protected no-arg constructor, static factory, entity state mutation, BCrypt hash 저장을 사용합니다.

## 8. 코드에 충실한 예시

일반 회원은 정규화 이메일·BCrypt 해시를 생성자에 전달하고, OAuth 회원은 `Member.fromOAuth(...)`로 비밀번호 없이 생성할 수 있습니다.

## 9. 요약

Member는 저장되는 계정 상태이고 서비스가 유효성·해시·비즈니스 규칙을 적용합니다.
