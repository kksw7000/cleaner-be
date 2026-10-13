package com.cleaner.be.auth;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

import jakarta.servlet.http.HttpServletResponse;

@Configuration
@EnableWebSecurity
/**
 * JWT 인증 흐름을 Spring Security 필터 체인에 연결합니다.
 * 인증 API는 토큰이 없을 때도 접근해야 하므로 허용하고, 그 외 API는 필터에서 인증된 요청만 통과시킵니다.
 */
public class SecurityConfig {

	@Bean
	// Spring이 리플렉션으로 Bean을 등록하므로 Java 언어 서버의 사용되지 않음 경고를 억제합니다.
	SecurityFilterChain securityFilterChain(HttpSecurity http, JwtAuthenticationFilter jwtAuthenticationFilter) throws Exception {
		return http
			.csrf(csrf -> csrf.disable())
			// 인증 상태를 서버 세션에 저장하지 않고 요청마다 Access Token으로 다시 확인합니다.
			.sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
			.authorizeHttpRequests(authorize -> authorize
				// 관리자 URL은 Controller 진입 전 JWT의 ROLE_ADMIN 권한을 검사합니다.
				.requestMatchers("/admin/**").hasRole("ADMIN")
				// 회원가입, 로그인, 소셜 로그인, 토큰 갱신, 로그아웃은 토큰 없이도 사용 가능하게 합니다.
				.requestMatchers(
					"/api/auth/signup",
					"/api/auth/login",
					"/api/auth/oauth/**",
					"/api/auth/refresh",
					"/api/auth/logout",
					"/error").permitAll()
				// 게시판 읽기만 공개하고, 같은 URL의 쓰기 요청은 아래 authenticated 규칙으로 로그인 검사를 거칩니다.
				.requestMatchers(HttpMethod.GET, "/boards", "/boards/*").permitAll()
				// 회원 탈퇴는 JWT가 생성한 인증 정보가 있어야만 현재 사용자의 계정을 삭제할 수 있습니다.
				.anyRequest().authenticated())
			// 로그인하지 않은 요청과 권한 부족 요청을 각각 401·403으로 명확히 구분합니다.
			.exceptionHandling(exception -> exception
				.authenticationEntryPoint((request, response, authException) ->
					response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Authentication is required"))
				.accessDeniedHandler((request, response, accessDeniedException) ->
					response.sendError(HttpServletResponse.SC_FORBIDDEN, "Access is denied")))
			// 기본 아이디/비밀번호 필터보다 먼저 JWT를 해석해야 인가 단계에서 회원 정보를 사용할 수 있습니다.
			.addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class)
			.build();
	}
}
