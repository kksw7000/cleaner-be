# JwtTokenProvider 학습 가이드

> 원본: [JwtTokenProvider.java](../../src/main/java/com/cleaner/be/auth/JwtTokenProvider.java)

## 1. 역할·레이어·작성 이유·사용 시점

JWT 발급·서명 검증 구성 요소입니다. 인증 서비스가 토큰을 발급하고 필터/Controller가 검증 및 회원 ID 추출에 사용합니다.

## 2. 전체 원본 코드

아래 코드는 현재 Java 원본을 생략 없이 그대로 수록했습니다.

```java
package com.cleaner.be.auth;

import java.time.Duration;
import java.time.Instant;
import java.util.Date;

import javax.crypto.SecretKey;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import com.cleaner.be.auth.dto.TokenResponse;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;

/**
 * JWT의 생성과 서명 검증을 전담합니다.
 * Controller와 Filter가 같은 서명 키와 검증 규칙을 사용하도록 토큰 관련 처리를 이 클래스에 모읍니다.
 */
@Component
public class JwtTokenProvider {

	private final SecretKey signingKey;
	private final Duration accessTokenValidity;
	private final Duration refreshTokenValidity;

	public JwtTokenProvider(
		@Value("${jwt.secret}") String secret,
		@Value("${jwt.access-token-validity:PT30M}") Duration accessTokenValidity,
		@Value("${jwt.refresh-token-validity:P14D}") Duration refreshTokenValidity
	) {
		byte[] keyBytes = Decoders.BASE64.decode(secret);
		if (keyBytes.length < 32) {
			throw new IllegalArgumentException("jwt.secret must be at least 256 bits when Base64-decoded");
		}
		this.signingKey = Keys.hmacShaKeyFor(keyBytes);
		this.accessTokenValidity = accessTokenValidity;
		this.refreshTokenValidity = refreshTokenValidity;
	}

	/** 한 번의 인증 흐름에서 Access Token과 Refresh Token을 함께 발급합니다. */
	public IssuedTokens issueTokens(Member member) {
		// 한 번의 로그인 또는 갱신 요청에서 짧은 수명의 Access Token과 긴 수명의 Refresh Token을 함께 발급합니다.
		String accessToken = createToken(member, "access", accessTokenValidity);
		String refreshToken = createToken(member, "refresh", refreshTokenValidity);
		return new IssuedTokens(new TokenResponse(accessToken, "Bearer", accessTokenValidity.toSeconds()), refreshToken);
	}

	/** Access Token만 필요한 경우에도 동일한 서명·만료 규칙으로 생성합니다. */
	public String createAccessToken(Member member) {
		return createToken(member, "access", accessTokenValidity);
	}

	public long getRefreshTokenValiditySeconds() {
		return refreshTokenValidity.toSeconds();
	}

	/** 서명과 만료를 확인한 토큰의 subject에서 회원 ID를 읽습니다. */
	public Long getMemberId(String token) {
		return Long.valueOf(parse(token).getSubject());
	}

	/** 토큰이 유효하고 Access Token 용도로 발급됐는지 확인합니다. */
	public boolean isValidAccessToken(String token) {
		return hasType(token, "access");
	}

	/** 토큰이 유효하고 Refresh Token 용도로 발급됐는지 확인합니다. */
	public boolean isValidRefreshToken(String token) {
		return hasType(token, "refresh");
	}

	/** 파싱 실패는 인증 거부로 바꾸고 서명·만료·토큰 용도를 함께 검사합니다. */
	private boolean hasType(String token, String type) {
		try {
			// 서명과 만료 시간을 먼저 검증한 뒤, 용도(type)가 요청한 토큰과 일치하는지도 확인합니다.
			return type.equals(parse(token).get("type", String.class));
		} catch (RuntimeException exception) {
			return false;
		}
	}


	/** 서명 검증을 통과한 JWT payload를 읽으며, 위조되거나 잘못된 토큰은 예외로 전달합니다. */
	private Claims parse(String token) {
		return Jwts.parser().verifyWith(signingKey).build().parseSignedClaims(token).getPayload();
	}

	/** 회원 식별 정보와 용도·만료 시각을 담아 서버 서명으로 JWT를 생성합니다. */
	private String createToken(Member member, String type, Duration validity) {
		// 토큰 자체에는 비밀번호 대신 회원 식별자와 최소한의 식별 정보만 담아, 이후 요청에서 회원을 식별합니다.
		Instant now = Instant.now();
		return Jwts.builder()
			.subject(member.getId().toString())
			.claim("email", member.getEmail())
			.claim("type", type)
			.issuedAt(Date.from(now))
			.expiration(Date.from(now.plus(validity)))
			.signWith(signingKey)
			.compact();
	}
}
```

## 3. 줄별 구성과 문법 읽기

Duration/Instant/Date는 만료 시간, SecretKey와 JJWT는 서명을 다룹니다. `@Component` 및 `@Value`로 secret·기간 설정을 주입합니다. 생성자는 Base64 디코딩한 키 길이가 32바이트 미만이면 시작을 거부하고 HMAC 키를 만듭니다. `issueTokens`는 access/refresh type claim이 다른 JWT 쌍 및 TokenResponse를 만듭니다. `hasType`은 파싱 예외를 false로 바꾸고 `parse`는 서명을 검증합니다. `createToken`은 subject, email/type, 발급·만료 시간, 서명을 설정합니다.

## 4. 메서드별 목적·입력·처리 순서·호출·결과·예외

생성자: Base64 secret 및 기간을 받고 키 길이 부족 시 IllegalArgumentException. `issueTokens(Member)`: access/refresh 발급 후 `IssuedTokens` 반환. `createAccessToken(Member)`: access 문자열만 반환. `getRefreshTokenValiditySeconds()`: refresh 유효기간 초 반환. `getMemberId(String)`: 검증한 subject를 Long 변환; JWT/숫자 오류는 런타임 예외. `isValidAccessToken`/`isValidRefreshToken`: type 검사. `hasType`: parse 후 type 비교, RuntimeException이면 false. `parse`: signing key로 signed claims를 검증·반환. `createToken`: 회원 ID/email/type/time claim을 담아 서명 문자열 생성.

## 5. 실제 실행 흐름

앱 시작 시 secret을 검증합니다. 로그인/OAuth/갱신에서 토큰 쌍 생성, 보호 요청과 Refresh endpoint에서 서명·만료·용도 검증 및 subject 추출에 사용됩니다.

## 6. 확인된 관계와 추론의 구분

확인: AuthController, AuthService, OAuthService, JwtAuthenticationFilter가 호출하고 TokenResponse/IssuedTokens를 만듭니다. `jwt.secret`, access/refresh validity 설정을 읽습니다. 설정 실제 값은 환경에 따라 다릅니다.

## 7. 사용된 Java·Spring 핵심 개념

JWT claims, HMAC 대칭키, Base64, 시간 기반 만료, Duration/Instant/Date, 기본값 `@Value`, 서명과 암호화의 차이를 사용합니다. payload는 암호화되지 않으므로 비밀번호를 넣지 않습니다.

## 8. 코드에 충실한 예시

ID 42 회원에 발급한 Access/Refresh는 서로 다른 type claim과 설정 기간을 가집니다. 실제 서명 문자열은 비밀키와 시간에 따라 달라지므로 쓰지 않습니다.

## 9. 요약

키 검증, claim, 만료 및 토큰 용도 구분을 한곳에서 적용합니다.
