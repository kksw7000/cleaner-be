package com.cleaner.be.mypage;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import com.cleaner.be.auth.DuplicateEmailException;
import com.cleaner.be.auth.DuplicatePhoneNumberException;
import com.cleaner.be.auth.InvalidCredentialsException;
import com.cleaner.be.auth.InvalidCurrentPasswordException;
import com.cleaner.be.auth.Member;
import com.cleaner.be.auth.MemberRepository;
import com.cleaner.be.mypage.dto.MyPageResponse;
import com.cleaner.be.mypage.dto.ChangePasswordRequest;
import com.cleaner.be.mypage.dto.UpdateMyPageRequest;

/**
 * 마이페이지에서 현재 회원의 프로필을 조회하는 서비스입니다.
 * Controller가 인증된 회원 ID를 전달하면 저장된 회원을 다시 확인해, 탈퇴 후 남은 인증 정보가 노출되지 않게 합니다.
 */
@Service
public class MyPageService {

	private final MemberRepository memberRepository;
	private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

	public MyPageService(MemberRepository memberRepository) {
		this.memberRepository = memberRepository;
	}

	/**
	 * JWT 인증을 통과한 회원의 마이페이지 정보를 반환합니다.
	 * DB 엔티티를 그대로 반환하지 않아 비밀번호 해시와 내부 소셜 식별자가 API 응답에 포함되지 않습니다.
	 */
	@Transactional(readOnly = true)
	public MyPageResponse getMyPage(Long memberId) {
		Member member = memberRepository.findById(memberId)
			.orElseThrow(InvalidCredentialsException::new);

		return toResponse(member);
	}

	/**
	 * 인증된 회원이 입력한 프로필을 정규화한 뒤 중복 여부를 확인하고 저장합니다.
	 * 수정 대상 본인의 기존 이메일·번호는 중복으로 판단하지 않아 이름만 바꾸는 요청도 정상 처리됩니다.
	 */
	@Transactional
	public MyPageResponse updateMyPage(Long memberId, UpdateMyPageRequest request) {
		Member member = memberRepository.findById(memberId)
			.orElseThrow(InvalidCredentialsException::new);
		// 가입 처리와 같은 기준을 적용해 조회·로그인에 사용되는 이메일 형식을 일관되게 유지합니다.
		String email = request.email().trim().toLowerCase();
		String phoneNumber = request.phoneNumber().replaceAll("[^0-9]", "");

		if (!member.getEmail().equals(email) && memberRepository.findByEmail(email).isPresent()) {
			throw new DuplicateEmailException();
		}
		if (!member.getPhoneNumber().equals(phoneNumber) && memberRepository.findByPhoneNumber(phoneNumber).isPresent()) {
			throw new DuplicatePhoneNumberException();
		}

		// JPA가 트랜잭션 종료 시 변경 감지를 수행하므로 별도의 save 호출 없이 한 번에 반영됩니다.
		member.updateProfile(email, request.name().trim(), phoneNumber);
		return toResponse(member);
	}

	/**
	 * 현재 비밀번호를 BCrypt 해시와 비교한 뒤 새 비밀번호 해시로 교체합니다.
	 * 소셜 로그인처럼 비밀번호 해시가 없는 계정도 현재 비밀번호 검증을 통과할 수 없으므로 임의로 비밀번호를 설정할 수 없습니다.
	 */
	@Transactional
	public void changePassword(Long memberId, ChangePasswordRequest request) {
		Member member = memberRepository.findById(memberId)
			.orElseThrow(InvalidCredentialsException::new);

		if (member.getPassword() == null || !passwordEncoder.matches(request.currentPassword(), member.getPassword())) {
			throw new InvalidCurrentPasswordException();
		}

		// 새 원문은 즉시 BCrypt 해시로 변환하며 DB에는 해시값만 변경 감지로 저장됩니다.
		member.updatePassword(passwordEncoder.encode(request.newPassword()));
	}

	private MyPageResponse toResponse(Member member) {
		return new MyPageResponse(member.getId(), member.getEmail(), member.getName(), member.getPhoneNumber());
	}
}
