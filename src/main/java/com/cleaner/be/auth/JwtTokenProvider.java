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

/** 서명된 Access Token 및 Refresh Token JWT를 생성하고 검증합니다. */
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
			return type.equals(parse(token).get("type", String.class));
		} catch (RuntimeException exception) {
			return false;
		}
	}

	private Claims parse(String token) {
		return Jwts.parser().verifyWith(signingKey).build().parseSignedClaims(token).getPayload();
	}

	private String createToken(Member member, String type, Duration validity) {
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
