package com.cleaner.be.auth;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

/** 회원 영속화와 가입·로그인·소셜 계정 연결에 필요한 조건 조회를 제공합니다. */
public interface MemberRepository extends JpaRepository<Member, Long> {

	/** 회원가입과 로그인 전에 표준화된 이메일로 기존 계정을 조회합니다. */
	Optional<Member> findByEmail(String email);

	/** 가입·프로필 수정 시 전화번호 중복을 확인합니다. */
	Optional<Member> findByPhoneNumber(String phoneNumber);

	/** 같은 제공자 계정으로 재로그인할 회원을 찾아 기존 연결을 유지합니다. */
	Optional<Member> findByOauthProviderAndOauthProviderUserId(String oauthProvider, String oauthProviderUserId);
}
