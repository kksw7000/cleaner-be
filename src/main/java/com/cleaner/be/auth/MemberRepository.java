package com.cleaner.be.auth;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MemberRepository extends JpaRepository<Member, Long> {

	// 회원가입 전에 이메일이 이미 등록되어 있는지 확인합니다.
	Optional<Member> findByEmail(String email);

	Optional<Member> findByPhoneNumber(String phoneNumber);
}
