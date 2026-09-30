package com.cleaner.be.auth;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@EnableWebSecurity
/**
 * JWT 인증 흐름을 Spring Security 필터 체인에 연결합니다.
 * 인증 API는 토큰이 없을 때도 접근해야 하므로 허용하고, 그 외 API는 필터에서 인증된 요청만 통과시킵니다.
 */
public class SecurityConfig {

	@Bean
	SecurityFilterChain securityFilterChain(HttpSecurity http, JwtAuthenticationFilter jwtAuthenticationFilter) throws Exception {
		return http
			.csrf(csrf -> csrf.disable())
			// 인증 상태를 서버 세션에 저장하지 않고 요청마다 Access Token으로 다시 확인합니다.
			.sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
			.authorizeHttpRequests(authorize -> authorize
				.requestMatchers("/api/auth/**", "/error").permitAll()
				.anyRequest().authenticated())
			// 기본 아이디/비밀번호 필터보다 먼저 JWT를 해석해야 인가 단계에서 회원 정보를 사용할 수 있습니다.
			.addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class)
			.build();
	}
}
