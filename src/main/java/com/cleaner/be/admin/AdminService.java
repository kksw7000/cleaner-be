package com.cleaner.be.admin;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.cleaner.be.admin.dto.AdminCommentPageResponse;
import com.cleaner.be.admin.dto.AdminCommentResponse;
import com.cleaner.be.admin.dto.AdminMemberPageResponse;
import com.cleaner.be.admin.dto.AdminMemberResponse;
import com.cleaner.be.auth.AuthService;
import com.cleaner.be.auth.Member;
import com.cleaner.be.board.BoardService;
import com.cleaner.be.board.Comment;
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

	@Transactional(readOnly = true)
	public BoardPageResponse getBoards(int page, int size, String keyword) {
		return boardService.getBoards(page, size, keyword);
	}

	@Transactional
	public void updateNotice(Long boardId, Long adminId, boolean notice) {
		boardService.updateNoticeByAdmin(boardId, adminId, notice);
	}

	@Transactional
	public void deleteBoard(Long boardId, Long adminId) {
		boardService.deleteBoard(boardId, adminId);
	}

	@Transactional(readOnly = true)
	public AdminCommentPageResponse getComments(int page, int size) {
		Page<Comment> comments = boardService.getAllComments(page, size);
		return new AdminCommentPageResponse(comments.map(this::toCommentResponse).getContent(), comments.getNumber(),
			comments.getSize(), comments.getTotalElements(), comments.getTotalPages(), comments.isFirst(), comments.isLast());
	}

	@Transactional
	public void deleteComment(Long commentId, Long adminId) {
		boardService.deleteCommentByAdmin(commentId, adminId);
	}

	@Transactional(readOnly = true)
	public AdminMemberPageResponse getMembers(int page, int size) {
		Page<Member> members = authService.getMembers(PageRequest.of(page, size, Sort.by(Sort.Order.desc("id"))));
		return new AdminMemberPageResponse(members.map(this::toMemberResponse).getContent(), members.getNumber(), members.getSize(),
			members.getTotalElements(), members.getTotalPages(), members.isFirst(), members.isLast());
	}

	@Transactional
	public AdminMemberResponse updateMemberSuspension(Long memberId, boolean suspended) {
		return toMemberResponse(authService.updateMemberSuspension(memberId, suspended));
	}

	private AdminCommentResponse toCommentResponse(Comment comment) {
		return new AdminCommentResponse(comment.getId(), comment.getBoard().getId(), comment.getContent(),
			comment.getAuthor().getId(), comment.getAuthor().getName(), comment.getCreatedAt(), comment.getUpdatedAt());
	}

	private AdminMemberResponse toMemberResponse(Member member) {
		return new AdminMemberResponse(member.getId(), member.getEmail(), member.getName(), member.getPhoneNumber(),
			member.getRole(), member.isSuspended());
	}
}
