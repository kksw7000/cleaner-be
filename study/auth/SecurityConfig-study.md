# SecurityConfig 학습 가이드

> 원본: [SecurityConfig.java](../../src/main/java/com/cleaner/be/auth/SecurityConfig.java)

## 1. 역할·레이어·작성 이유·사용 시점

Spring Security 인가 규칙과 JWT 필터 연결을 정의하는 구성 계층입니다.

## 2. 전체 원본 코드

아래 코드는 현재 Java 원본을 생략 없이 그대로 수록했습니다.

```java
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

/**
 * JWT 인증 흐름을 Spring Security 필터 체인에 연결합니다.
 * 인증 API는 토큰이 없을 때도 접근해야 하므로 허용하고, 그 외 API는 필터에서 인증된 요청만 통과시킵니다.
 */
@Configuration
@EnableWebSecurity
public class SecurityConfig {

	/** URL별 인가 규칙과 JWT 필터를 하나의 무상태 보안 체인으로 구성합니다. */
	@Bean
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
				.requestMatchers(HttpMethod.GET, "/api/boards", "/api/boards/*").permitAll()
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
```

## 3. 줄별 구성과 문법 읽기

`@Configuration`/`@EnableWebSecurity`가 보안 설정을 켭니다. `@Bean` 메서드에서 HttpSecurity를 구성합니다. CSRF를 끄고 SessionCreationPolicy.STATELESS, `/admin/**` ADMIN, auth 공개 경로, board GET 공개, 나머지 authenticated 순입니다. 매처 순서가 중요합니다. 예외 처리부는 인증 필요 401 및 권한 부족 403을 보냅니다. JWT 필터를 UsernamePasswordAuthenticationFilter보다 앞에 넣고 `build()`로 체인을 만듭니다. 람다 `->`는 설정 callback입니다.

## 4. 메서드별 목적·입력·처리 순서·호출·결과·예외

`securityFilterChain(HttpSecurity,JwtAuthenticationFilter)`: 무상태 정책→URL 인가→401/403 처리기→JWT 필터 삽입→SecurityFilterChain 반환 순입니다. 설정 구성 실패 예외는 `throws Exception`으로 전달됩니다.

## 5. 실제 실행 흐름

요청이 SecurityFilterChain에 들어오고 JWT 필터가 인증을 설정합니다. 뒤이어 HTTP 경로/메서드 규칙이 평가됩니다. 보호 경로 미인증은 401, 권한 부족은 403입니다.

## 6. 확인된 관계와 추론의 구분

확인: JwtAuthenticationFilter를 앞에 연결하고 AuthController 인증 URL 및 `/error`를 공개합니다. `/admin/**`는 ADMIN, board GET만 공개이며 쓰기 요청은 authenticated 규칙입니다. CSRF 비활성화 이유는 주석상 stateless JWT입니다.

## 7. 사용된 Java·Spring 핵심 개념

Spring Java Configuration, HttpSecurity DSL, SecurityFilterChain, stateless auth, URL/method 인가, 401/403 차이와 람다를 사용합니다. `hasRole`은 ROLE_ 접두어 규칙을 적용합니다.

## 8. 코드에 충실한 예시

`GET /api/boards`는 공개이나 `POST /api/boards`는 공개 GET 매처에 해당하지 않아 로그인해야 합니다. `/admin/...`은 ROLE_ADMIN이 필요합니다.

## 9. 요약

JWT 인증과 경로별 공개/보호/관리자 접근 규칙을 체인으로 구성합니다.
