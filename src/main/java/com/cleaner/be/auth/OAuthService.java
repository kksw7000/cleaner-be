package com.cleaner.be.auth;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;


/** 소셜 로그인 공통 흐름입니다. 제공자 토큰을 검증하고 회원을 조회하거나 생성합니다. */
@Service
public class OAuthService {
	private final MemberRepository memberRepository;
	private final List<OAuthTokenVerifier> tokenVerifiers;
	private final JwtTokenProvider jwtTokenProvider;

	public OAuthService(MemberRepository memberRepository, List<OAuthTokenVerifier> tokenVerifiers,
		JwtTokenProvider jwtTokenProvider) {
		this.memberRepository = memberRepository;
		this.tokenVerifiers = tokenVerifiers;
		this.jwtTokenProvider = jwtTokenProvider;
	}

	/** 제공자 토큰을 검증하고 연결 회원을 조회·생성한 뒤, 정지 계정이 아니면 내부 JWT를 발급합니다. */
	@Transactional
	public IssuedTokens login(String provider, String accessToken) {
		String normalizedProvider = provider.trim().toLowerCase();
		OAuthTokenVerifier verifier = tokenVerifiers.stream().filter(candidate -> candidate.supports(normalizedProvider))
			.findFirst().orElseThrow(() -> new IllegalArgumentException("Unsupported OAuth provider: " + provider));
		OAuthUserInfo userInfo = verifier.verify(accessToken);
		if (userInfo == null || isBlank(userInfo.providerUserId()) || isBlank(userInfo.email()) || isBlank(userInfo.name())) {
			throw new InvalidCredentialsException();
		}
		String normalizedEmail = userInfo.email().trim().toLowerCase();
		Member member = memberRepository.findByOauthProviderAndOauthProviderUserId(normalizedProvider, userInfo.providerUserId())
			.or(() -> memberRepository.findByEmail(normalizedEmail))
			.orElseGet(() -> memberRepository.save(Member.fromOAuth(normalizedEmail, userInfo.name().trim(), normalizedProvider,
				userInfo.providerUserId())));
		if (member.isSuspended()) {
			throw new SuspendedMemberException();
		}
		return jwtTokenProvider.issueTokens(member);
	}

	/** 제공자 응답의 필수 식별 정보가 null 또는 공백인지 판별해 불완전한 데이터를 거부합니다. */
	private boolean isBlank(String value) {
		return value == null || value.isBlank();
	}
}
