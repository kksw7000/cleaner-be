package com.cleaner.be.auth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import com.cleaner.be.auth.dto.LoginRequest;
import com.cleaner.be.auth.dto.LoginResponse;
import com.cleaner.be.auth.dto.SignupRequest;
import com.cleaner.be.auth.dto.SignupResponse;

@SpringBootTest
@Transactional
class AuthServiceTest {

	@Autowired
	private AuthService authService;

	@Test
	void 저장된_이메일과_비밀번호가_일치하면_로그인한다() {
		// 회원가입 과정에서 DB에는 BCrypt 해시 비밀번호가 저장됩니다.
		authService.signup(new SignupRequest("login@example.com", "Password!1", "로그인 사용자", "010-1234-5678"));

		// 로그인에서는 동일한 평문 비밀번호를 전달해 저장된 해시와 비교합니다.
		LoginResponse response = authService.login(new LoginRequest("LOGIN@example.com", "Password!1"));

		assertThat(response.email()).isEqualTo("login@example.com");
		assertThat(response.name()).isEqualTo("로그인 사용자");
	}

	@Test
	void 비밀번호가_다르면_로그인을_거부한다() {
		authService.signup(new SignupRequest("wrong-password@example.com", "Password!1", "테스트 사용자", "010-9876-5432"));

		assertThatThrownBy(() -> authService.login(new LoginRequest("wrong-password@example.com", "WrongPass!1")))
			.isInstanceOf(InvalidCredentialsException.class);
	}

	@Test
	void 회원탈퇴후_로그인은_실패한다() {
		SignupResponse signupResponse = authService.signup(
			new SignupRequest("withdraw@example.com", "Password!1", "탈퇴 사용자", "010-1111-2222"));

		authService.withdraw(signupResponse.id());

		assertThatThrownBy(() -> authService.login(
			new LoginRequest("withdraw@example.com", "Password!1")))
			.isInstanceOf(InvalidCredentialsException.class);
	}
}
