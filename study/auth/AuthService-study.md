# AuthService 학습 가이드

> 원본: [AuthService.java](../../src/main/java/com/cleaner/be/auth/AuthService.java)

## 1. 역할·레이어·작성 이유·사용 시점

회원가입·로그인·회원 상태 관련 도메인 규칙을 처리하는 서비스 계층입니다. Controller가 인증 요청을 위임하거나 회원을 조회·변경할 때 사용합니다.

## 2. 전체 원본 코드

아래 코드는 현재 Java 원본을 생략 없이 그대로 수록했습니다.

```java
package com.cleaner.be.auth;

import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import com.cleaner.be.auth.dto.SignupRequest;
import com.cleaner.be.auth.dto.SignupResponse;
import com.cleaner.be.auth.dto.LoginRequest;
import com.cleaner.be.auth.dto.LoginResponse;

/** 회원가입·로그인과 회원 상태 변경을 처리하며 인증 도메인의 저장 규칙을 관리합니다. */
@Service
public class AuthService {

	private final MemberRepository memberRepository;
	private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

	public AuthService(MemberRepository memberRepository) {
		this.memberRepository = memberRepository;
	}

	/**
	 * 회원가입의 핵심 처리입니다.
	 * 이메일을 표준화하고 중복을 확인한 다음, 원문 비밀번호 대신 BCrypt 해시를 저장합니다.
	 * 처리 중 예외가 발생하면 {@code @Transactional}에 의해 DB 저장은 롤백됩니다.
	 */
	@Transactional
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
		validateNotSuspended(member);

		// 인증에 성공해도 비밀번호(평문·해시 모두)는 클라이언트에 반환하지 않습니다.
		return new LoginResponse(member.getId(), member.getEmail(), member.getName(), member.getPhoneNumber());
	}

	/**
	 * 로그인 또는 Refresh Token 갱신 과정에서 확인된 회원 ID로 회원 정보를 조회합니다.
	 * 토큰을 추출하거나 아이디를 찾는 메서드는 아니며, 존재하지 않는 회원이면 인증 실패로 처리합니다.
	 */
	@Transactional(readOnly = true)
	public Member getMember(Long memberId) {
		Member member = memberRepository.findById(memberId)
			.orElseThrow(InvalidCredentialsException::new);
		validateNotSuspended(member);
		return member;
	}

	/** 관리자 목록 화면이 회원 엔티티를 직접 조회하지 않도록 공통 회원 조회를 제공합니다. */
	@Transactional(readOnly = true)
	public Page<Member> getMembers(Pageable pageable) {
		return memberRepository.findAll(pageable);
	}

	/** 관리자 요청으로만 호출되며, 상태 변경 후 JWT 필터가 다음 요청부터 즉시 차단합니다. */
	@Transactional
	public Member updateMemberSuspension(Long memberId, boolean suspended) {
		Member member = memberRepository.findById(memberId)
			.orElseThrow(InvalidCredentialsException::new);
		member.updateSuspension(suspended);
		return member;
	}

	/**
	 * 인증된 사용자가 자신의 계정을 탈퇴할 때 호출됩니다.
	 * 회원을 즉시 삭제해 로그인과 새 토큰 발급을 차단하며, 삭제 대상이 없으면 인증 실패로 처리합니다.
	 */
	@Transactional
	public void withdraw(Long memberId) {
		Member member = memberRepository.findById(memberId)
			.orElseThrow(InvalidCredentialsException::new);
		memberRepository.delete(member);
	}

	/** 정지된 계정의 로그인과 토큰 갱신을 차단해 보호 API 접근이 이어지지 않게 합니다. */
	private void validateNotSuspended(Member member) {
		if (member.isSuspended()) {
			throw new SuspendedMemberException();
		}
	}
}
```

## 3. 줄별 구성과 문법 읽기

import는 BCrypt, Spring Service/트랜잭션, 페이지 타입과 요청·응답 DTO입니다. `@Service`는 Spring Bean 등록, `@Transactional`은 저장 작업을 트랜잭션으로 묶고 `readOnly=true`는 조회 의도를 표시합니다. repository는 생성자 주입, BCrypt 인코더는 필드에서 생성됩니다. signup은 이메일 소문자화/공백 제거→중복검사→전화 숫자화/중복검사→비밀번호 해시→저장→공개 응답입니다. `orElseThrow(InvalidCredentialsException::new)`는 결과가 없을 때 예외 생성자를 호출합니다. `if`, `return`, `throw`, 제네릭 `Page<Member>`가 사용됩니다.

## 4. 메서드별 목적·입력·처리 순서·호출·결과·예외

생성자: repository를 저장합니다. `signup(SignupRequest)`: 이메일·전화 중복이면 각 중복 예외, 정상은 BCrypt 해시로 저장하고 `SignupResponse`를 반환합니다. `login(LoginRequest)`: 정규화 이메일 조회→BCrypt `matches`→정지검사→비밀번호 없는 `LoginResponse`; 회원 없음/비밀번호 오류는 같은 인증 예외, 정지면 정지 예외입니다. `getMember(Long)`: ID 조회, 정지검사, 정상 `Member` 반환; 없음은 인증 예외. `getMembers(Pageable)`: `findAll`에 페이지 조건을 전달하고 Page 반환. `updateMemberSuspension(Long,boolean)`: 존재 회원의 상태를 바꾸고 반환. `withdraw(Long)`: 회원 삭제, 없으면 인증 예외. `validateNotSuspended(Member)`: 정지면 예외, 아니면 정상 종료.

## 5. 실제 실행 흐름

Controller 검증 이후 가입/로그인이 호출됩니다. 가입은 하나의 쓰기 트랜잭션이며 로그인은 조회→해시 대조→정지 확인→DTO 반환입니다. ID 조회도 정지 상태를 검사해 토큰 발급 전 차단합니다.

## 6. 확인된 관계와 추론의 구분

확인: `AuthController`, `OAuthService`, `JwtAuthenticationFilter`가 이 서비스/MemberRepository/Member를 사용합니다. Repository는 JPA 조회·저장·삭제를 제공합니다. 추론: 관리자 상태 변경 호출의 권한 보호는 호출 계층에 달려 있으며 여기서는 확인하지 않습니다.

## 7. 사용된 Java·Spring 핵심 개념

Spring Service·선언적 트랜잭션·Spring Data Optional/Page·BCrypt 단방향 해시·정규화·메서드 참조·예외 흐름을 사용합니다. 원문 비밀번호를 저장하거나 반환하지 않습니다.

## 8. 코드에 충실한 예시

`Hana@Example.com`, `010-1234-5678` 가입 값은 조회·저장 전 각각 `hana@example.com`, `01012345678`로 정규화되고 비밀번호는 BCrypt 해시가 됩니다. 중복이면 409로 변환될 도메인 예외가 발생합니다.

## 9. 요약

회원가입/로그인/상태 확인을 한곳에서 처리하며 입력 정규화, 중복, 해시 및 정지 규칙을 적용합니다.
