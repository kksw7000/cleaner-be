package com.cleaner.be.auth;

import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.cleaner.be.auth.dto.SignupRequest;
import com.cleaner.be.auth.dto.SignupResponse;
import com.cleaner.be.auth.dto.LoginRequest;
import com.cleaner.be.auth.dto.LoginResponse;

@Service
public class AuthService {

	private final MemberRepository memberRepository;
	private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

	public AuthService(MemberRepository memberRepository) {
		this.memberRepository = memberRepository;
	}

		@Transactional
	/**
	 * 회원가입의 핵심 처리입니다.
	 * 이메일을 표준화하고 중복을 확인한 다음, 원문 비밀번호 대신 BCrypt 해시를 저장합니다.
	 * 처리 중 예외가 발생하면 {@code @Transactional}에 의해 DB 저장은 롤백됩니다.
	 */
	public SignupResponse signup(SignupRequest request) {
		// 대소문자와 앞뒤 공백 차이로 같은 이메일을 여러 번 가입하지 못하게 합니다.
		String email = request.email().trim().toLowerCase();
		if (memberRepository.findByEmail(email).isPresent()) {
			throw new DuplicateEmailException();
		}
		// 전화번호는 숫자만 남기고 나머지 문자는 제거합니다. (예: 010-1234-5678 → 01012345678)
		String phoneNumber = request.phoneNumber().replaceAll("[^0-9]", "");
		if (memberRepository.findByPhoneNumber(phoneNumber).isPresent()) {
			throw new DuplicatePhoneNumberException();
		}

		// 비밀번호 원문은 DB에 저장하지 않고 단방향 BCrypt 해시값만 저장합니다.
		Member member = memberRepository.save(new Member(
			email,
			passwordEncoder.encode(request.password()),
			request.name().trim(),
			phoneNumber
		));
		// 비밀번호를 응답에 포함하지 않고, 가입에 필요한 공개 정보만 반환합니다.
		return new SignupResponse(member.getId(), member.getEmail(), member.getName(), member.getPhoneNumber());
	}

	/**
	 * 저장된 이메일과 BCrypt 비밀번호 해시를 이용해 로그인 정보를 검증합니다.
	 * 이메일이 없거나 비밀번호가 틀린 경우에는 같은 예외를 반환해 계정 유추를 막습니다.
	 */
	@Transactional(readOnly = true)
	public LoginResponse login(LoginRequest request) {
		// 회원가입 때와 같은 방식으로 이메일의 공백과 대소문자 차이를 제거합니다.
		String email = request.email().trim().toLowerCase();
		Member member = memberRepository.findByEmail(email)
			.orElseThrow(InvalidCredentialsException::new);

		// 평문끼리 비교하지 않고, 입력값을 저장된 BCrypt 해시와 안전하게 비교합니다.
		if (!passwordEncoder.matches(request.password(), member.getPassword())) {
			throw new InvalidCredentialsException();
		}

		// 인증에 성공해도 비밀번호(평문·해시 모두)는 클라이언트에 반환하지 않습니다.
		return new LoginResponse(member.getId(), member.getEmail(), member.getName(), member.getPhoneNumber());
	}

	/**
	 * 로그인 또는 Refresh Token 갱신 과정에서 확인된 회원 ID로 회원 정보를 조회합니다.
	 * 토큰을 추출하거나 아이디를 찾는 메서드는 아니며, 존재하지 않는 회원이면 인증 실패로 처리합니다.
	 */
	@Transactional(readOnly = true)
	public Member getMember(Long memberId) {
		return memberRepository.findById(memberId)
			.orElseThrow(InvalidCredentialsException::new);
	}
}
