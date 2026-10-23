# OAuthService 학습 가이드

> 원본: [OAuthService.java](../../src/main/java/com/cleaner/be/auth/OAuthService.java)

## 1. 역할·레이어·작성 이유·사용 시점

OAuth 제공자 토큰을 검증하고 회원을 찾거나 생성한 뒤 내부 JWT를 발급하는 공통 서비스입니다.

## 2. 전체 원본 코드

아래 코드는 현재 Java 원본을 생략 없이 그대로 수록했습니다.

```java
package com.cleaner.be.auth;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;


/** 소셜 로그인 공통 흐름입니다. 제공자 토큰을 검증하고 회원을 조회하거나 생성합니다. */
@Service
public class OAuthService {
	private final MemberRepository memberRepository;
	private final List<OAuthTokenVerifier> tokenVerifiers;
	private final JwtTokenProvider jwtTokenProvider;

	public OAuthService(MemberRepository memberRepository, List<OAuthTokenVerifier> tokenVerifiers,
		JwtTokenProvider jwtTokenProvider) {
		this.memberRepository = memberRepository;
		this.tokenVerifiers = tokenVerifiers;
		this.jwtTokenProvider = jwtTokenProvider;
	}

	/** 제공자 토큰을 검증하고 연결 회원을 조회·생성한 뒤, 정지 계정이 아니면 내부 JWT를 발급합니다. */
	@Transactional
	public IssuedTokens login(String provider, String accessToken) {
		String normalizedProvider = provider.trim().toLowerCase();
		OAuthTokenVerifier verifier = tokenVerifiers.stream().filter(candidate -> candidate.supports(normalizedProvider))
			.findFirst().orElseThrow(() -> new IllegalArgumentException("Unsupported OAuth provider: " + provider));
		OAuthUserInfo userInfo = verifier.verify(accessToken);
		if (userInfo == null || isBlank(userInfo.providerUserId()) || isBlank(userInfo.email()) || isBlank(userInfo.name())) {
			throw new InvalidCredentialsException();
		}
		String normalizedEmail = userInfo.email().trim().toLowerCase();
		Member member = memberRepository.findByOauthProviderAndOauthProviderUserId(normalizedProvider, userInfo.providerUserId())
			.or(() -> memberRepository.findByEmail(normalizedEmail))
			.orElseGet(() -> memberRepository.save(Member.fromOAuth(normalizedEmail, userInfo.name().trim(), normalizedProvider,
				userInfo.providerUserId())));
		if (member.isSuspended()) {
			throw new SuspendedMemberException();
		}
		return jwtTokenProvider.issueTokens(member);
	}

	/** 제공자 응답의 필수 식별 정보가 null 또는 공백인지 판별해 불완전한 데이터를 거부합니다. */
	private boolean isBlank(String value) {
		return value == null || value.isBlank();
	}
}
```

## 3. 줄별 구성과 문법 읽기

`List<OAuthTokenVerifier>`로 여러 구현체를 받고 MemberRepository/JwtTokenProvider를 생성자 주입합니다. `@Service`, `@Transactional`이 사용됩니다. provider는 trim/lowercase 후 Stream filter/findFirst로 선택합니다. 검증 사용자 정보가 null/blank면 거부합니다. provider ID 조회→email 조회→없으면 `Member.fromOAuth` 저장 순입니다. `Optional.or`/`orElseGet`은 조회/생성을 지연 처리합니다.

## 4. 메서드별 목적·입력·처리 순서·호출·결과·예외

생성자: 세 의존성 저장. `login(provider,accessToken)`: provider 정규화→지원 verifier 선택(미지원은 IllegalArgumentException)→외부 토큰 검증→필수 정보 검증(실패 InvalidCredentialsException)→provider ID/email로 기존 회원 매칭 또는 생성→정지 검사(SuspendedMemberException)→JWT 발급 및 `IssuedTokens` 반환. Verifier 자체 예외도 전파될 수 있습니다. `isBlank(String)`: null 또는 빈 공백 여부 반환.

## 5. 실제 실행 흐름

Controller에서 provider/token 수신→검증 구현 선택→신뢰 사용자 정보 얻기→기존 계정 매칭/신규 생성→정지 차단→내부 JWT 발급입니다.

## 6. 확인된 관계와 추론의 구분

확인: AuthController와 Repository, OAuthTokenVerifier/OAuthUserInfo, Member.fromOAuth, JwtTokenProvider를 사용합니다. auth Java 트리 안에는 verifier 구현체가 보이지 않습니다. 주입할 Bean이 없으면 지원 provider가 없을 수 있습니다. 같은 이메일로 기존 계정을 매칭하는 것은 코드에서 확인됩니다.

## 7. 사용된 Java·Spring 핵심 개념

Java Stream, interface 다형성, Optional 체인, Spring DI/transaction, `String.isBlank`, OAuth 외부 토큰의 검증 경계를 사용합니다.

## 8. 코드에 충실한 예시

`GOOGLE`은 `google`로 정규화됩니다. 검증 결과 이메일을 소문자로 만들고 provider ID로 먼저 찾은 후 이메일 조회를 시도합니다. 외부 토큰과 내부 JWT는 서로 다른 토큰입니다.

## 9. 요약

제공자별 검증을 공통 회원 매칭·내부 JWT 흐름과 분리합니다.
