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

	public IssuedTokens issueTokens(Member member) {
		// 한 번의 로그인 또는 갱신 요청에서 짧은 수명의 Access Token과 긴 수명의 Refresh Token을 함께 발급합니다.
		String accessToken = createToken(member, "access", accessTokenValidity);
		String refreshToken = createToken(member, "refresh", refreshTokenValidity);
		return new IssuedTokens(new TokenResponse(accessToken, "Bearer", accessTokenValidity.toSeconds()), refreshToken);
	}

	public String createAccessToken(Member member) {
		return createToken(member, "access", accessTokenValidity);
	}

	public long getRefreshTokenValiditySeconds() {
		return refreshTokenValidity.toSeconds();
	}

	public Long getMemberId(String token) {
		return Long.valueOf(parse(token).getSubject());
	}

	public boolean isValidAccessToken(String token) {
		return hasType(token, "access");
	}

	public boolean isValidRefreshToken(String token) {
		return hasType(token, "refresh");
	}

	private boolean hasType(String token, String type) {
		try {
			// 서명과 만료 시간을 먼저 검증한 뒤, 용도(type)가 요청한 토큰과 일치하는지도 확인합니다.
			return type.equals(parse(token).get("type", String.class));
		} catch (RuntimeException exception) {
			return false;
		}
	}


	//우리가 만든 JWT 토큰인지 아닌지 확인하는 로직
	private Claims parse(String token) {
		return Jwts.parser().verifyWith(signingKey).build().parseSignedClaims(token).getPayload();
	}

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
