# JwtAuthenticationFilter 학습 가이드

> 원본: [JwtAuthenticationFilter.java](../../src/main/java/com/cleaner/be/auth/JwtAuthenticationFilter.java)

## 1. 역할·레이어·작성 이유·사용 시점

Spring Security 요청 필터입니다. Controller보다 먼저 Bearer Access Token, 회원 존재와 정지 상태를 확인하고 성공 시 SecurityContext에 인증 정보를 넣습니다.

## 2. 전체 원본 코드

아래 코드는 현재 Java 원본을 생략 없이 그대로 수록했습니다.

```java
package com.cleaner.be.auth;

import java.io.IOException;
import java.util.List;

import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
 * 보호된 API 요청이 Controller에 도달하기 전에 Access Token을 검사하는 필터입니다.
 * 토큰이 유효할 때만 회원 ID를 Spring Security 인증 정보에 넣어 이후 인가 규칙이 현재 회원을 알 수 있게 합니다.
 */
@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

	private final JwtTokenProvider jwtTokenProvider;
	private final MemberRepository memberRepository;

	public JwtAuthenticationFilter(JwtTokenProvider jwtTokenProvider, MemberRepository memberRepository) {
		this.jwtTokenProvider = jwtTokenProvider;
		this.memberRepository = memberRepository;
	}

	/** 요청의 Bearer 토큰을 검증하고 회원 상태를 확인한 뒤 인증 정보를 SecurityContext에 설정합니다. */
	@Override
	protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
		throws ServletException, IOException {
		// 클라이언트가 localStorage의 Access Token을 Authorization 헤더에 실어 보낸 경우에만 인증을 시도합니다.
		String authorization = request.getHeader(HttpHeaders.AUTHORIZATION);
		if (authorization == null || !authorization.startsWith("Bearer ")) {
			filterChain.doFilter(request, response);
			return;
		}

		String token = authorization.substring(7);
		if (!jwtTokenProvider.isValidAccessToken(token)) {
			// 만료·위조·Refresh Token 사용을 모두 인증 실패로 처리해 보호된 Controller까지 요청이 전달되지 않게 합니다.
			response.sendError(HttpStatus.UNAUTHORIZED.value(), "Invalid or expired access token");
			return;
		}

		Long memberId = jwtTokenProvider.getMemberId(token);
		// 탈퇴된 계정의 기존 토큰이 보호 API에 접근하지 못하도록 회원 존재 여부를 함께 확인합니다.
		Member member = memberRepository.findById(memberId).orElse(null);
		if (member == null) {
			response.sendError(HttpStatus.UNAUTHORIZED.value(), "Invalid or expired access token");
			return;
		}
		// 이미 발급된 토큰도 DB의 최신 정지 상태를 확인해 글 작성 등 보호 API를 즉시 차단합니다.
		if (member.isSuspended()) {
			response.sendError(HttpStatus.FORBIDDEN.value(), "Suspended member");
			return;
		}

		// 요청 처리 동안만 유지되는 SecurityContext에 회원 ID를 넣으며, 서버 세션에는 저장하지 않습니다.
		UsernamePasswordAuthenticationToken authentication =
			new UsernamePasswordAuthenticationToken(memberId, null,
				List.of(new SimpleGrantedAuthority("ROLE_" + member.getRole().name())));
		authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
		SecurityContextHolder.getContext().setAuthentication(authentication);
		filterChain.doFilter(request, response);
	}
}
```

## 3. 줄별 구성과 문법 읽기

`@Component`가 Bean 등록을 합니다. servlet 요청/응답/FilterChain 및 Spring Security 인증·권한 객체를 import합니다. final provider/repository는 생성자 주입입니다. `OncePerRequestFilter`의 `doFilterInternal`을 override합니다. Bearer 접두어 이후 `substring(7)`이 토큰입니다. 잘못된 토큰·회원 없음은 401, 정지는 403 후 조기 반환합니다. 정상 요청은 principal=회원 ID, 권한=`ROLE_`+enum 이름으로 인증 객체를 만들고 다음 필터로 진행합니다.

## 4. 메서드별 목적·입력·처리 순서·호출·결과·예외

생성자: provider/repository 주입. `doFilterInternal(request,response,chain)`: 헤더가 없거나 Bearer가 아니면 인증 없이 chain 호출; 토큰이 invalid면 401 후 종료; member ID 조회 후 회원이 없으면 401, 정지면 403; 정상은 UsernamePasswordAuthenticationToken 및 요청 상세를 SecurityContext에 넣고 chain 호출합니다. `IOException`, `ServletException`을 선언하며 JWT 검증 실패는 provider가 false로 처리합니다.

## 5. 실제 실행 흐름

요청→헤더 확인→토큰 서명/만료/type 검증→DB 회원/정지 조회→SecurityContext 인증 설정→나머지 체인 및 Controller입니다. 헤더 없는 요청도 공개 경로일 수 있어 통과시키고 인가는 SecurityConfig가 결정합니다.

## 6. 확인된 관계와 추론의 구분

확인: `SecurityConfig`가 UsernamePassword 필터 앞에 배치, `JwtTokenProvider`가 검증 및 ID 추출, `MemberRepository`가 최신 회원을 조회합니다. AuthController.withdraw가 principal을 읽습니다.

## 7. 사용된 Java·Spring 핵심 개념

Spring Security filter chain, OncePerRequestFilter, Bearer 인증, SecurityContext, Authentication/GrantedAuthority, servlet FilterChain을 사용합니다.

## 8. 코드에 충실한 예시

유효한 Bearer JWT와 활성 회원이면 Controller에서 principal 회원 ID를 사용할 수 있습니다. 토큰 문자열은 실제 서명키가 필요하므로 생략합니다.

## 9. 요약

필터는 요청마다 JWT와 DB의 최신 계정 상태를 확인하고 성공한 요청에 인증 주체와 역할을 설정합니다.
