package com.cleaner.be.admin;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import com.cleaner.be.auth.JwtTokenProvider;
import com.cleaner.be.auth.Member;
import com.cleaner.be.auth.MemberRepository;

/** 관리자 URL은 인증 유무와 USER 역할을 각각 401·403으로 구분하는지 확인합니다. */
@SpringBootTest
class AdminAuthorizationTest {

	private MockMvc mockMvc;

	@Autowired
	private WebApplicationContext webApplicationContext;

	@Autowired
	private MemberRepository memberRepository;

	@Autowired
	private JwtTokenProvider jwtTokenProvider;

	/** 실제 Spring Security 체인을 적용한 MockMvc를 준비해 URL 권한 규칙을 검증합니다. */
	@BeforeEach
	void setUp() {
		// 실제 SecurityFilterChain을 포함한 컨텍스트로 요청을 실행해 URL 권한 규칙을 검증합니다.
		mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext).apply(springSecurity()).build();
	}

	/** 공유 테스트 컨텍스트에 남은 회원 데이터가 다른 테스트에 영향을 주지 않도록 제거합니다. */
	@AfterEach
	void cleanUp() {
		// 공유 H2 컨텍스트를 사용하는 기존 인증 테스트와 회원 데이터가 충돌하지 않도록 정리합니다.
		memberRepository.deleteAll();
	}

	/** 인증된 일반 회원이 관리자 게시판에 접근하면 403으로 거부되는지 확인합니다. */
	@Test
	void userCannotAccessAdminBoards() throws Exception {
		Member user = memberRepository.save(new Member("user@example.com", "encoded-password", "user", "01012345678"));
		String accessToken = jwtTokenProvider.createAccessToken(user);

		mockMvc.perform(get("/admin/boards").header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken))
			.andExpect(status().isForbidden());
	}

	/** 인증 토큰이 없는 관리자 게시판 요청이 401로 거부되는지 확인합니다. */
	@Test
	void unauthenticatedUserCannotAccessAdminBoards() throws Exception {
		mockMvc.perform(get("/admin/boards"))
			.andExpect(status().isUnauthorized());
	}
}
