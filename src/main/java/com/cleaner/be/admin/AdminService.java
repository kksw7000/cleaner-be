package com.cleaner.be.admin;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.cleaner.be.admin.dto.AdminMemberPageResponse;
import com.cleaner.be.admin.dto.AdminMemberResponse;
import com.cleaner.be.auth.AuthService;
import com.cleaner.be.auth.Member;
import com.cleaner.be.board.BoardService;
import com.cleaner.be.board.dto.BoardPageResponse;

/**
 * 관리자 API의 요청을 조율합니다.
 * 게시글·댓글·회원 상태의 실제 규칙은 각 도메인 서비스에 위임해 관리자 전용 중복 구현을 만들지 않습니다.
 */
@Service
public class AdminService {

	private final BoardService boardService;
	private final AuthService authService;

	public AdminService(BoardService boardService, AuthService authService) {
		this.boardService = boardService;
		this.authService = authService;
	}

	/** 검색 조건과 페이지 정보를 게시글 도메인 규칙에 맡겨 관리자 게시글 목록으로 반환합니다. */
	@Transactional(readOnly = true)
	public BoardPageResponse getBoards(int page, int size, String keyword) {
		return boardService.getBoards(page, size, keyword);
	}

	/** 관리자 권한 검증과 공지 상태 변경을 게시글 서비스에 맡겨 도메인 정책을 일관되게 적용합니다. */
	@Transactional
	public void updateNotice(Long boardId, Long adminId, boolean notice) {
		boardService.updateNoticeByAdmin(boardId, adminId, notice);
	}

	/** 게시글 삭제 가능 여부를 게시글 서비스에서 확인하고 해당 트랜잭션 안에서 삭제합니다. */
	@Transactional
	public void deleteBoard(Long boardId, Long adminId) {
		boardService.deleteBoard(boardId, adminId);
	}

	/** 관리자 댓글 삭제의 권한과 도메인 처리를 게시글 서비스에 위임합니다. */
	@Transactional
	public void deleteComment(Long commentId, Long adminId) {
		boardService.deleteCommentByAdmin(commentId, adminId);
	}

	/** 회원 ID 내림차순 페이지를 조회하고 관리자 응답 형식으로 변환해 읽기 전용으로 반환합니다. */
	@Transactional(readOnly = true)
	public AdminMemberPageResponse getMembers(int page, int size) {
		Page<Member> members = authService.getMembers(PageRequest.of(page, size, Sort.by(Sort.Order.desc("id"))));
		return new AdminMemberPageResponse(members.map(this::toMemberResponse).getContent(), members.getNumber(), members.getSize(),
			members.getTotalElements(), members.getTotalPages(), members.isFirst(), members.isLast());
	}

	/** 회원 정지 상태 변경을 인증 서비스의 정책에 맡기고 결과를 관리자 응답으로 변환합니다. */
	@Transactional
	public AdminMemberResponse updateMemberSuspension(Long memberId, boolean suspended) {
		return toMemberResponse(authService.updateMemberSuspension(memberId, suspended));
	}

	/** 회원 엔티티의 관리자 조회 정보를 응답 DTO로 옮겨 내부 엔티티 노출을 막습니다. */
	private AdminMemberResponse toMemberResponse(Member member) {
		return new AdminMemberResponse(member.getId(), member.getEmail(), member.getName(), member.getPhoneNumber(),
			member.getRole(), member.isSuspended());
	}
}
