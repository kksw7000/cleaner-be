package com.cleaner.be.auth;

import java.io.IOException;
import java.util.List;

import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
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
		if (!memberRepository.existsById(memberId)) {
			response.sendError(HttpStatus.UNAUTHORIZED.value(), "Invalid or expired access token");
			return;
		}

		// 요청 처리 동안만 유지되는 SecurityContext에 회원 ID를 넣으며, 서버 세션에는 저장하지 않습니다.
		UsernamePasswordAuthenticationToken authentication =
			new UsernamePasswordAuthenticationToken(memberId, null, List.of());
		authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
		SecurityContextHolder.getContext().setAuthentication(authentication);
		filterChain.doFilter(request, response);
	}
}
