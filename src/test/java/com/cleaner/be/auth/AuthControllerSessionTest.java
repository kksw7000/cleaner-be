package com.cleaner.be.auth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.Test;

import com.cleaner.be.auth.dto.LoginRequest;
import com.cleaner.be.auth.dto.LoginResponse;
import org.springframework.http.ResponseEntity;

import com.cleaner.be.auth.dto.TokenResponse;

class AuthControllerSessionTest {

	@Test
	void loginReturnsJwtTokensInsteadOfCreatingASession() {
		AuthService authService = mock(AuthService.class);
		JwtTokenProvider tokenProvider = mock(JwtTokenProvider.class);
		Member member = mock(Member.class);
		TokenResponse tokens = new TokenResponse("access", "Bearer", 1800L);
		LoginRequest request = new LoginRequest("login@example.com", "Password!1");
		when(authService.login(request)).thenReturn(new LoginResponse(1L, "login@example.com", "name", "01012345678"));
		when(authService.getMember(1L)).thenReturn(member);
		IssuedTokens issuedTokens = new IssuedTokens(tokens, "refresh");
		when(tokenProvider.issueTokens(member)).thenReturn(issuedTokens);
		when(tokenProvider.getRefreshTokenValiditySeconds()).thenReturn(1209600L);

		ResponseEntity<TokenResponse> response = new AuthController(authService, mock(OAuthService.class), tokenProvider,
			true, "Strict").login(request);

		assertThat(response.getBody()).isEqualTo(tokens);
		assertThat(response.getHeaders().getFirst("Set-Cookie")).contains("HttpOnly").contains("refresh_token");
	}
}
