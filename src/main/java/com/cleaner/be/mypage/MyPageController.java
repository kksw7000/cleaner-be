package com.cleaner.be.mypage;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.cleaner.be.auth.InvalidCredentialsException;
import com.cleaner.be.mypage.dto.ChangePasswordRequest;
import com.cleaner.be.mypage.dto.MyPageResponse;
import com.cleaner.be.mypage.dto.UpdateMyPageRequest;

import jakarta.validation.Valid;

/**
 * 로그인 후 마이페이지에 필요한 회원 정보를 제공하는 API입니다.
 * JWT 필터가 검증한 회원 ID만 사용하므로, URL이나 요청 본문으로 다른 회원 정보를 조회할 수 없습니다.
 */
@RestController
@RequestMapping("/api/mypage")
public class MyPageController {

	private final MyPageService myPageService;

	public MyPageController(MyPageService myPageService) {
		this.myPageService = myPageService;
	}

	/**
	 * 요청 헤더의 Access Token → JWT 필터 → SecurityContext 순서로 전달된 회원 ID를 사용합니다.
	 * 인증 정보가 없으면 서비스 조회 전에 401 응답으로 처리합니다.
	 */
	@GetMapping
	public MyPageResponse getMyPage() {
		return myPageService.getMyPage(currentMemberId());
	}

	/**
	 * 클라이언트가 수정할 프로필을 전달하면 현재 Access Token의 회원 정보만 변경합니다.
	 * 요청 본문에 회원 ID를 받지 않으므로 다른 회원의 계정 정보를 수정할 수 없습니다.
	 */
	@PatchMapping
	public MyPageResponse updateMyPage(@Valid @RequestBody UpdateMyPageRequest request) {
		return myPageService.updateMyPage(currentMemberId(), request);
	}

	/**
	 * 비밀번호 변경은 단순 프로필 변경보다 민감하므로 현재 비밀번호를 함께 받아 재확인합니다.
	 * 성공 시 비밀번호 값을 포함하지 않고 204 응답만 반환합니다.
	 */
	@PatchMapping("/password")
	public ResponseEntity<Void> changePassword(
		@Valid @RequestBody ChangePasswordRequest request) {
		myPageService.changePassword(currentMemberId(), request);
		return ResponseEntity.noContent().build();
	}

	private Long currentMemberId() {
		Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
		if (authentication == null || !(authentication.getPrincipal() instanceof Number memberId)) {
			throw new InvalidCredentialsException();
		}

		return memberId.longValue();
	}
}
