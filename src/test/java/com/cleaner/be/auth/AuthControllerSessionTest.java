package com.cleaner.be.auth;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.cookie;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import com.cleaner.be.auth.dto.LoginRequest;
import com.cleaner.be.auth.dto.LoginResponse;
import com.cleaner.be.auth.dto.TokenResponse;

/** JWT 로그인 HTTP 응답과 refresh token 쿠키를 검증합니다. */
class AuthControllerSessionTest {

	/** 로그인 성공 시 상태 코드와 회원 이름·권한 JSON을 반환하고 세션 대신 쿠키를 설정합니다. */
	@Test
	void loginReturnsJwtTokensInsteadOfCreatingASession() throws Exception {
		AuthService authService = mock(AuthService.class);
		JwtTokenProvider tokenProvider = mock(JwtTokenProvider.class);
		Member member = mock(Member.class);
		TokenResponse tokens = new TokenResponse("access", "Bearer", 1800L, "test-user", Role.USER);
		LoginRequest request = new LoginRequest("login@example.com", "Password!1");
		when(authService.login(request)).thenReturn(new LoginResponse(1L, "login@example.com", "name"));
		when(authService.getMember(1L)).thenReturn(member);
		when(tokenProvider.issueTokens(member)).thenReturn(new IssuedTokens(tokens, "refresh"));
		when(tokenProvider.getRefreshTokenValiditySeconds()).thenReturn(1209600L);

		AuthController controller = new AuthController(authService, mock(OAuthService.class), tokenProvider,
			true, "Strict");
		MockMvc mockMvc = MockMvcBuilders.standaloneSetup(controller).build();

		mockMvc.perform(post("/api/auth/login")
			.contentType(MediaType.APPLICATION_JSON)
			.content("{\"email\":\"login@example.com\",\"password\":\"Password!1\"}"))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.name").value("test-user"))
			.andExpect(jsonPath("$.role").value("USER"))
			.andExpect(cookie().exists("refresh_token"));
	}
}
