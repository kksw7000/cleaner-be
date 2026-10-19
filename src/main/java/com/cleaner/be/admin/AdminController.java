package com.cleaner.be.admin;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.cleaner.be.admin.dto.AdminMemberPageResponse;
import com.cleaner.be.admin.dto.AdminMemberResponse;
import com.cleaner.be.admin.dto.MemberSuspensionRequest;
import com.cleaner.be.admin.dto.NoticeUpdateRequest;
import com.cleaner.be.auth.InvalidCredentialsException;
import com.cleaner.be.board.dto.BoardPageResponse;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;

/** 관리자 전용 HTTP 진입점입니다. ADMIN 권한 검사는 SecurityConfig에서 선행하고 현재 ID만 서비스에 전달합니다. */
@RestController
@Validated
@RequestMapping("/admin")
public class AdminController {

	private final AdminService adminService;

	public AdminController(AdminService adminService) {
		this.adminService = adminService;
	}

	/** 페이지와 선택적 검색어를 받아 관리자 게시글 목록을 조회하도록 AdminService에 전달합니다. */
	@GetMapping("/boards")
	public BoardPageResponse getBoards(@RequestParam(defaultValue = "0") @Min(0) int page,
		@RequestParam(defaultValue = "10") @Min(1) @Max(100) int size,
		@RequestParam(required = false) String keyword) {
		return adminService.getBoards(page, size, keyword);
	}

	/** 관리자 요청의 공지 상태를 변경하고, 완료 후 응답 본문 없이 반환합니다. */
	@PatchMapping("/boards/{id}/notice")
	public ResponseEntity<Void> updateNotice(@PathVariable Long id, @Valid @RequestBody NoticeUpdateRequest request) {
		// 관리자 ID를 서비스에도 전달해 권한 검증과 공지 변경을 수행합니다.
		adminService.updateNotice(id, currentMemberId(), request.notice());
		return ResponseEntity.noContent().build();
	}

	/** 게시글을 관리자 권한으로 삭제하고, 성공 시 응답 본문 없이 반환합니다. */
	@DeleteMapping("/boards/{id}")
	public ResponseEntity<Void> deleteBoard(@PathVariable Long id) {
		// 서비스에서 관리자 권한과 게시글 삭제를 함께 처리하도록 인증 ID를 전달합니다.
		adminService.deleteBoard(id, currentMemberId());
		return ResponseEntity.noContent().build();
	}

	/** 댓글 ID로 대상을 지정해 관리자 권한으로 삭제하고, 성공 시 응답 본문 없이 반환합니다. */
	@DeleteMapping("/comments/{commentId}")
	public ResponseEntity<Void> deleteComment(@PathVariable Long commentId) {
		// 서비스에서 관리자 권한과 댓글 삭제를 함께 처리하도록 인증 ID를 전달합니다.
		adminService.deleteComment(commentId, currentMemberId());
		return ResponseEntity.noContent().build();
	}

	/** 페이지 정보를 받아 회원 관리 화면에 필요한 회원 목록을 조회합니다. */
	@GetMapping("/members")
	public AdminMemberPageResponse getMembers(@RequestParam(defaultValue = "0") @Min(0) int page,
		@RequestParam(defaultValue = "10") @Min(1) @Max(100) int size) {
		return adminService.getMembers(page, size);
	}
 
	/** 요청의 정지 여부를 회원 계정에 반영하고 변경된 회원 정보를 반환합니다. */
	@PatchMapping("/members/{id}/suspension")
	public AdminMemberResponse updateMemberSuspension(@PathVariable Long id,
		@Valid @RequestBody MemberSuspensionRequest request) {
		// 계정 이용 상태 변경은 관리자 엔드포인트에서 검증된 요청만 서비스에 반영합니다.
		return adminService.updateMemberSuspension(id, request.suspended());
	}

	/** 인증 필터가 저장한 회원 ID를 반환하고, 유효한 숫자 ID가 없으면 인증 오류로 처리합니다. */
	private Long currentMemberId() {
		// 보안 설정을 통과한 요청의 인증 정보를 서비스 권한 검증에 사용할 수 있도록 읽습니다.
		Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
		if (authentication == null || !(authentication.getPrincipal() instanceof Number memberId)) {
			// 회원 ID로 사용할 수 없는 principal은 서비스에 전달하지 않고 인증 오류로 거부합니다.
			throw new InvalidCredentialsException();
		}
		return memberId.longValue();
	}
}
