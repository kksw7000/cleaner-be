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

import com.cleaner.be.admin.dto.AdminCommentPageResponse;
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

	@GetMapping("/boards")
	public BoardPageResponse getBoards(@RequestParam(defaultValue = "0") @Min(0) int page,
		@RequestParam(defaultValue = "10") @Min(1) @Max(100) int size,
		@RequestParam(required = false) String keyword) {
		return adminService.getBoards(page, size, keyword);
	}

	@PatchMapping("/boards/{id}/notice")
	public ResponseEntity<Void> updateNotice(@PathVariable Long id, @Valid @RequestBody NoticeUpdateRequest request) {
		adminService.updateNotice(id, currentMemberId(), request.notice());
		return ResponseEntity.noContent().build();
	}

	@DeleteMapping("/boards/{id}")
	public ResponseEntity<Void> deleteBoard(@PathVariable Long id) {
		adminService.deleteBoard(id, currentMemberId());
		return ResponseEntity.noContent().build();
	}

	@GetMapping("/comments")
	public AdminCommentPageResponse getComments(@RequestParam(defaultValue = "0") @Min(0) int page,
		@RequestParam(defaultValue = "10") @Min(1) @Max(100) int size) {
		return adminService.getComments(page, size);
	}

	@DeleteMapping("/comments/{commentId}")
	public ResponseEntity<Void> deleteComment(@PathVariable Long commentId) {
		adminService.deleteComment(commentId, currentMemberId());
		return ResponseEntity.noContent().build();
	}

	@GetMapping("/members")
	public AdminMemberPageResponse getMembers(@RequestParam(defaultValue = "0") @Min(0) int page,
		@RequestParam(defaultValue = "10") @Min(1) @Max(100) int size) {
		return adminService.getMembers(page, size);
	}

	@PatchMapping("/members/{id}/suspension")
	public AdminMemberResponse updateMemberSuspension(@PathVariable Long id,
		@Valid @RequestBody MemberSuspensionRequest request) {
		return adminService.updateMemberSuspension(id, request.suspended());
	}

	private Long currentMemberId() {
		Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
		if (authentication == null || !(authentication.getPrincipal() instanceof Number memberId)) {
			throw new InvalidCredentialsException();
		}
		return memberId.longValue();
	}
}
