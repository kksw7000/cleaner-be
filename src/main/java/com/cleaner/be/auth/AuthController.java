package com.cleaner.be.auth;

import org.springframework.http.HttpStatus;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.CookieValue;

import com.cleaner.be.auth.dto.LoginRequest;
import com.cleaner.be.auth.dto.OAuthLoginRequest;
import com.cleaner.be.auth.dto.SignupRequest;
import com.cleaner.be.auth.dto.SignupResponse;
import com.cleaner.be.auth.dto.TokenResponse;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

	private final AuthService authService;
	private final OAuthService oauthService;
	private final JwtTokenProvider jwtTokenProvider;
	private final boolean refreshCookieSecure;
	private final String refreshCookieSameSite;

	public AuthController(AuthService authService, OAuthService oauthService, JwtTokenProvider jwtTokenProvider,
		@Value("${jwt.refresh-cookie-secure:true}") boolean refreshCookieSecure,
		@Value("${jwt.refresh-cookie-same-site:Strict}") String refreshCookieSameSite) {
		this.authService = authService;
		this.oauthService = oauthService;
		this.jwtTokenProvider = jwtTokenProvider;
		this.refreshCookieSecure = refreshCookieSecure;
		this.refreshCookieSameSite = refreshCookieSameSite;
	}

	@PostMapping("/signup")
	@ResponseStatus(HttpStatus.CREATED)
	public SignupResponse signup(@Valid @RequestBody SignupRequest request) {
		return authService.signup(request);
	}

	@PostMapping("/login")
	public ResponseEntity<TokenResponse> login(@Valid @RequestBody LoginRequest request) {
		Long memberId = authService.login(request).id();
		return tokenResponse(jwtTokenProvider.issueTokens(authService.getMember(memberId)));
	}

	@PostMapping("/oauth/{provider}")
	public ResponseEntity<TokenResponse> oauthLogin(@PathVariable String provider, @Valid @RequestBody OAuthLoginRequest request) {
		return tokenResponse(oauthService.login(provider, request.accessToken()));
	}

	@PostMapping("/refresh")
	public ResponseEntity<TokenResponse> refresh(@CookieValue(name = "refresh_token", required = false) String refreshToken) {
		if (refreshToken == null || !jwtTokenProvider.isValidRefreshToken(refreshToken)) {
			throw new InvalidCredentialsException();
		}
		return tokenResponse(jwtTokenProvider.issueTokens(authService.getMember(jwtTokenProvider.getMemberId(refreshToken))));
	}

	@PostMapping("/logout")
	@ResponseStatus(HttpStatus.NO_CONTENT)
	public ResponseEntity<Void> logout() {
		ResponseCookie expiredCookie = ResponseCookie.from("refresh_token", "")
			.httpOnly(true).secure(refreshCookieSecure).sameSite(refreshCookieSameSite).path("/api/auth").maxAge(0).build();
		return ResponseEntity.noContent().header(HttpHeaders.SET_COOKIE, expiredCookie.toString()).build();
	}

	private ResponseEntity<TokenResponse> tokenResponse(IssuedTokens tokens) {
		ResponseCookie refreshCookie = ResponseCookie.from("refresh_token", tokens.refreshToken())
			.httpOnly(true).secure(refreshCookieSecure).sameSite(refreshCookieSameSite).path("/api/auth")
			.maxAge(jwtTokenProvider.getRefreshTokenValiditySeconds()).build();
		return ResponseEntity.ok().header(HttpHeaders.SET_COOKIE, refreshCookie.toString()).body(tokens.accessTokenResponse());
	}
}
