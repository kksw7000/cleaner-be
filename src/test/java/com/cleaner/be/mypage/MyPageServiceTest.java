package com.cleaner.be.mypage;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import com.cleaner.be.auth.AuthService;
import com.cleaner.be.auth.DuplicateEmailException;
import com.cleaner.be.auth.InvalidCredentialsException;
import com.cleaner.be.auth.InvalidCurrentPasswordException;
import com.cleaner.be.auth.dto.LoginRequest;
import com.cleaner.be.auth.dto.SignupRequest;
import com.cleaner.be.auth.dto.SignupResponse;
import com.cleaner.be.mypage.dto.MyPageResponse;
import com.cleaner.be.mypage.dto.ChangePasswordRequest;
import com.cleaner.be.mypage.dto.UpdateMyPageRequest;

@SpringBootTest
@Transactional
class MyPageServiceTest {

	@Autowired
	private AuthService authService;

	@Autowired
	private MyPageService myPageService;

	@Test
	void 로그인한_회원의_공개_프로필만_조회한다() {
		// 실제 회원가입 데이터를 사용해 마이페이지가 DB의 현재 회원 정보를 조회하는지 확인합니다.
		SignupResponse signupResponse = authService.signup(
			new SignupRequest("mypage@example.com", "Password!1", "마이페이지 사용자", "010-3333-4444"));

		MyPageResponse response = myPageService.getMyPage(signupResponse.id());

		assertThat(response.id()).isEqualTo(signupResponse.id());
		assertThat(response.email()).isEqualTo("mypage@example.com");
		assertThat(response.name()).isEqualTo("마이페이지 사용자");
		assertThat(response.phoneNumber()).isEqualTo("01033334444");
	}

	@Test
	void 로그인한_회원이_자신의_프로필을_수정한다() {
		SignupResponse signupResponse = authService.signup(
			new SignupRequest("before@example.com", "Password!1", "수정 전 사용자", "010-5555-6666"));

		MyPageResponse response = myPageService.updateMyPage(signupResponse.id(),
			new UpdateMyPageRequest(" AFTER@EXAMPLE.COM ", "수정 후 사용자", "010-7777-8888"));

		assertThat(response.email()).isEqualTo("after@example.com");
		assertThat(response.name()).isEqualTo("수정 후 사용자");
		assertThat(response.phoneNumber()).isEqualTo("01077778888");
	}

	@Test
	void 다른_회원의_이메일로는_수정할_수_없다() {
		authService.signup(new SignupRequest("already@example.com", "Password!1", "기존 사용자", "010-9999-0000"));
		SignupResponse signupResponse = authService.signup(
			new SignupRequest("change@example.com", "Password!1", "변경 사용자", "010-1111-3333"));

		assertThatThrownBy(() -> myPageService.updateMyPage(signupResponse.id(),
			new UpdateMyPageRequest("already@example.com", "변경 사용자", "010-1111-3333")))
			.isInstanceOf(DuplicateEmailException.class);
	}

	@Test
	void 현재_비밀번호를_확인한_후_새_비밀번호로_변경한다() {
		SignupResponse signupResponse = authService.signup(
			new SignupRequest("password@example.com", "Password!1", "비밀번호 사용자", "010-2222-4444"));

		myPageService.changePassword(signupResponse.id(), new ChangePasswordRequest("Password!1", "NewPassword!2"));

		assertThat(authService.login(new LoginRequest("password@example.com", "NewPassword!2")).email())
			.isEqualTo("password@example.com");
		assertThatThrownBy(() -> authService.login(
			new LoginRequest("password@example.com", "Password!1")))
			.isInstanceOf(InvalidCredentialsException.class);
	}

	@Test
	void 현재_비밀번호가_다르면_변경하지_않는다() {
		SignupResponse signupResponse = authService.signup(
			new SignupRequest("wrong-current@example.com", "Password!1", "비밀번호 사용자", "010-2222-5555"));

		assertThatThrownBy(() -> myPageService.changePassword(signupResponse.id(),
			new ChangePasswordRequest("WrongPassword!1", "NewPassword!2")))
			.isInstanceOf(InvalidCurrentPasswordException.class);
	}
}
