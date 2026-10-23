# AuthController 학습 가이드

> 원본: [AuthController.java](../../src/main/java/com/cleaner/be/auth/AuthController.java)

## 1. 역할·레이어·작성 이유·사용 시점

인증 HTTP 컨트롤러(표현 계층)입니다. `/api/auth` 요청을 받고 요청 DTO 검증, 서비스 호출, HTTP 상태·쿠키 응답 구성을 연결합니다. 회원가입·로그인·OAuth·갱신·로그아웃·탈퇴를 수행할 때 사용합니다.

## 2. 전체 원본 코드

아래 코드는 현재 Java 원본을 생략 없이 그대로 수록했습니다.

```java
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
```

## 3. 줄별 구성과 문법 읽기

`package`와 import는 Spring MVC 응답·쿠키·인증 API, 5개 auth DTO, `@Valid`를 연결합니다. `@RestController`는 반환값을 응답 본문으로 직렬화하고 `@RequestMapping`은 공통 경로를 정합니다. final 필드는 생성자에서 주입되며 `@Value` 매개변수는 쿠키 Secure/SameSite 설정입니다. `@PostMapping`/`@DeleteMapping`은 라우트, `@RequestBody`는 JSON 변환, `@Valid`는 DTO 제약 검증, `@PathVariable`은 provider, `@CookieValue`는 쿠키를 읽습니다. `ResponseEntity`는 헤더·본문·상태를 묶습니다. `throw`는 예외 전달, `return`은 결과 반환입니다.

## 4. 메서드별 목적·입력·처리 순서·호출·결과·예외

`AuthController(...)`: 세 서비스/토큰 제공자와 쿠키 설정을 저장합니다. `signup(SignupRequest)`: `AuthService.signup` 결과 반환, 201 상태. `login(LoginRequest)`: 서비스 인증 → 회원 재조회 → JWT 발급 → 공통 응답. `oauthLogin(String, OAuthLoginRequest)`: provider와 외부 토큰을 OAuthService에 전달. `refresh(String)`: 쿠키 누락/유효성 실패면 `InvalidCredentialsException`; 정상 토큰에서 회원을 찾고 새 토큰을 발급합니다. `logout()`: 만료시간 0인 Refresh 쿠키를 보내 204 반환; 서버 토큰 저장소는 사용하지 않습니다. `withdraw()`: SecurityContext principal의 숫자 ID를 읽어 계정을 삭제하고 204 반환; principal 누락이면 인증 예외. `tokenResponse(IssuedTokens)`: Access Token DTO는 본문, Refresh Token은 HttpOnly 쿠키에 반환합니다. 세부 호출은 각 메서드 이름대로 위 순서이며 서비스/JWT 예외는 호출자에게 전파됩니다.

## 5. 실제 실행 흐름

회원가입은 DTO 검증→저장→201입니다. 로그인은 검증→자격 증명 확인→정지 검사→토큰 발급→본문/쿠키 응답입니다. 갱신은 쿠키→Refresh 검증→회원 조회→새 토큰 회전입니다. 탈퇴는 필터가 만든 인증 ID→삭제→204이고 로그아웃은 쿠키 삭제 지시만 보냅니다.

## 6. 확인된 관계와 추론의 구분

확인: `SecurityConfig`가 경로 공개/인가를 정하고 `JwtAuthenticationFilter`가 보호 요청 principal을 설정합니다. 이 컨트롤러는 `AuthService`, `OAuthService`, `JwtTokenProvider`, auth DTO 및 `GlobalExceptionHandler` 대상 예외와 직접 연결됩니다. 추론: 클라이언트가 Access Token을 보관하고 쿠키를 자동 전송하는 흐름입니다(주석과 응답 구조 근거).

## 7. 사용된 Java·Spring 핵심 개념

Spring MVC 라우팅·생성자 주입·`@Value`·Bean Validation·`ResponseEntity`·HTTP 상태·HttpOnly 쿠키·SecurityContext를 사용합니다. Refresh 쿠키는 응답 본문에 포함되지 않게 분리됩니다.

## 8. 코드에 충실한 예시

`POST /api/auth/login`에 이메일/비밀번호 JSON을 보내면 성공 시 `TokenResponse` JSON과 `Set-Cookie` 헤더가 함께 옵니다. 토큰 값과 쿠키 설정은 환경에 따라 달라집니다.

## 9. 요약

컨트롤러는 HTTP 입출력만 조정하고 비즈니스 규칙은 서비스와 토큰 제공자에 위임합니다. Access Token은 본문, Refresh Token은 HttpOnly 쿠키라는 분리가 핵심입니다.
