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
