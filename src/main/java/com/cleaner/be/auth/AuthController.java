package com.cleaner.be.auth;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.cleaner.be.auth.dto.LoginRequest;
import com.cleaner.be.auth.dto.OAuthLoginRequest;
import com.cleaner.be.auth.dto.SignupRequest;
import com.cleaner.be.auth.dto.SignupResponse;
import com.cleaner.be.auth.dto.TokenResponse;

import jakarta.validation.Valid;

/**
 * 인증 요청의 시작점입니다.
 * 로그인과 소셜 로그인은 토큰을 발급해 응답하고, 토큰 갱신과 로그아웃은 Refresh Token 쿠키를 사용합니다.
 * Access Token은 응답 본문으로, Refresh Token은 JavaScript에서 읽을 수 없는 HTTP 전용 쿠키로 분리합니다.
 */
@RestController
@RequestMapping("/api/auth")
public class AuthController {

	private final AuthService authService;
	private final OAuthService oauthService;
	private final JwtTokenProvider jwtTokenProvider;
	private final boolean refreshCookieSecure;
	private final String refreshCookieSameSite;

	public AuthController(AuthService authService, OAuthService oauthService, JwtTokenProvider jwtTokenProvider,
		@Value("${jwt.refresh-cookie-secure}") boolean refreshCookieSecure,
		@Value("${jwt.refresh-cookie-same-site}") String refreshCookieSameSite) {
		this.authService = authService;
		this.oauthService = oauthService;
		this.jwtTokenProvider = jwtTokenProvider;
		this.refreshCookieSecure = refreshCookieSecure;
		this.refreshCookieSameSite = refreshCookieSameSite;
	}

	/** 검증된 회원가입 요청을 AuthService에 전달하고 생성된 공개 회원 정보를 반환합니다. */
	@PostMapping("/signup")
	@ResponseStatus(HttpStatus.CREATED)
	public SignupResponse signup(@Valid @RequestBody SignupRequest request) {
		return authService.signup(request);
	}

	/**
	 * 이메일/비밀번호 로그인 흐름입니다.
	 * 요청값 검증 → 회원 인증 → Access/Refresh Token 발급 → Access Token 응답 및 Refresh Token 쿠키 설정 순으로 처리합니다.
	 */
	@PostMapping("/login")
	public ResponseEntity<TokenResponse> login(@Valid @RequestBody LoginRequest request) {
		Long memberId = authService.login(request).id();
		return tokenResponse(jwtTokenProvider.issueTokens(authService.getMember(memberId)));
	}

	/** 제공자 이름과 외부 Access Token을 OAuthService에 전달해 내부 토큰 쌍으로 교환합니다. */
	@PostMapping("/oauth/{provider}")
	public ResponseEntity<TokenResponse> oauthLogin(@PathVariable String provider, @Valid @RequestBody OAuthLoginRequest request) {
		return tokenResponse(oauthService.login(provider, request.accessToken()));
	}

	/**
	 * Access Token이 만료됐을 때 브라우저가 자동 전송한 Refresh Token 쿠키로 토큰을 갱신합니다.
	 * Refresh Token도 함께 교체해, 탈취된 이전 토큰을 장기간 재사용할 위험을 줄입니다.
	 */
	@PostMapping("/refresh")
	public ResponseEntity<TokenResponse> refresh(@CookieValue(name = "refresh_token", required = false) String refreshToken) {
		if (refreshToken == null || !jwtTokenProvider.isValidRefreshToken(refreshToken)) {
			throw new InvalidCredentialsException();
		}
		return tokenResponse(jwtTokenProvider.issueTokens(authService.getMember(jwtTokenProvider.getMemberId(refreshToken))));
	}

	/**
	 * 서버가 세션을 저장하지 않는 JWT 방식의 로그아웃 처리입니다.
	 * Refresh Token 쿠키의 만료 시간을 0으로 내려 브라우저에서 제거하고, 클라이언트는 응답 후 localStorage의 Access Token을 삭제해야 합니다.
	 */
	@PostMapping("/logout")
	@ResponseStatus(HttpStatus.NO_CONTENT)
	public ResponseEntity<Void> logout() {
		ResponseCookie expiredCookie = ResponseCookie.from("refresh_token", "")
			.httpOnly(true).secure(refreshCookieSecure).sameSite(refreshCookieSameSite).path("/api/auth").maxAge(0).build();
		return ResponseEntity.noContent().header(HttpHeaders.SET_COOKIE, expiredCookie.toString()).build();
	}

	/**
	 * JWT 필터가 남긴 SecurityContext의 회원 ID를 사용해 현재 사용자의 계정을 삭제합니다.
	 * 다른 사용자의 ID를 전달할 수 없으며, 성공 시 204 응답을 반환합니다.
	 */
	@DeleteMapping("/withdraw")
	@ResponseStatus(HttpStatus.NO_CONTENT)
	public ResponseEntity<Void> withdraw() {
		Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
		if (authentication == null || authentication.getPrincipal() == null) {
			throw new InvalidCredentialsException();
		}
		Long memberId = ((Number) authentication.getPrincipal()).longValue();
		authService.withdraw(memberId);
		return ResponseEntity.noContent().build();
	}

	/** Access Token은 본문에, Refresh Token은 보안 속성이 설정된 쿠키에 분리해 전달합니다. */
	private ResponseEntity<TokenResponse> tokenResponse(IssuedTokens tokens) {
		// Refresh Token은 XSS로부터 보호하기 위해 응답 본문이 아닌 HTTP 전용 쿠키로만 전달합니다.
		ResponseCookie refreshCookie = ResponseCookie.from("refresh_token", tokens.refreshToken())
			.httpOnly(true).secure(refreshCookieSecure).sameSite(refreshCookieSameSite).path("/api/auth")
			.maxAge(jwtTokenProvider.getRefreshTokenValiditySeconds()).build();
		return ResponseEntity.ok().header(HttpHeaders.SET_COOKIE, refreshCookie.toString()).body(tokens.accessTokenResponse());
	}
}
