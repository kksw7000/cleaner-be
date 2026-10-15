package com.cleaner.be.board;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.cleaner.be.auth.Member;
import com.cleaner.be.auth.MemberRepository;
import com.cleaner.be.auth.Role;
import com.cleaner.be.board.dto.BoardCreateRequest;
import com.cleaner.be.board.dto.BoardListResponse;
import com.cleaner.be.board.dto.BoardPageResponse;
import com.cleaner.be.board.dto.BoardResponse;
import com.cleaner.be.board.dto.BoardUpdateRequest;
import com.cleaner.be.board.dto.CommentCreateRequest;
import com.cleaner.be.board.dto.CommentResponse;
import com.cleaner.be.board.dto.CommentUpdateRequest;

/**
 * 게시글·댓글 요청의 도메인 규칙을 처리합니다.
 * Controller가 SecurityContext에서 얻은 회원 ID만 전달하면, 이 서비스가 작성자 확인·저장·DTO 변환을 순서대로 수행합니다.
 */
@Service
public class BoardService {

	private final BoardRepository boardRepository;
	private final CommentRepository commentRepository;
	private final MemberRepository memberRepository;

	public BoardService(BoardRepository boardRepository, CommentRepository commentRepository, MemberRepository memberRepository) {
		this.boardRepository = boardRepository;
		this.commentRepository = commentRepository;
		this.memberRepository = memberRepository;
	}

	/** 인증된 회원을 작성자로 연결해 게시글을 저장하고, 공지로 지정할 때는 관리자 권한을 확인합니다. */
	@Transactional
	public BoardResponse createBoard(Long memberId, BoardCreateRequest request) {
		// 회원 조회 → 공지 권한 확인 → 게시글 저장 및 응답 변환 순서로 처리합니다.
		Member author = findMember(memberId);
		validateNoticePermission(author, request.notice());
		Board board = boardRepository.save(new Board(request.title().trim(), request.content().trim(), request.notice(), author));
		return toBoardResponse(board, List.of());
	}

	/** 공지를 먼저 보여주고 최신순으로 정렬해 페이지를 반환하며, 키워드가 있으면 제목만 검색해 목록 범위를 제한합니다. */
	@Transactional(readOnly = true)
	public BoardPageResponse getBoards(int page, int size, String keyword) {
		// 정렬·페이지 조건 생성 → 키워드 정리 → 제목 검색 또는 전체 조회 → 페이지 응답 변환 순서로 처리합니다.
		PageRequest pageRequest = PageRequest.of(page, size, Sort.by(
			Sort.Order.desc("notice"), Sort.Order.desc("createdAt")));
		String normalizedKeyword = keyword == null ? "" : keyword.trim();
		Page<Board> boards = normalizedKeyword.isEmpty()
			? boardRepository.findAll(pageRequest)
			: boardRepository.findByTitleContainingIgnoreCase(normalizedKeyword, pageRequest);

		return new BoardPageResponse(
			boards.map(this::toBoardListResponse).getContent(), boards.getNumber(), boards.getSize(),
			boards.getTotalElements(), boards.getTotalPages(), boards.isFirst(), boards.isLast());
	}

	/** 게시글 상세 정보와 시간순 댓글을 함께 제공하고, 조회 요청마다 조회수를 반영해 열람 횟수를 기록합니다. */
	@Transactional
	public BoardResponse getBoard(Long boardId) {
		// 게시글 조회 → 조회수 증가 → 댓글 조회 및 응답 변환 순서로 처리합니다.
		Board board = findBoard(boardId);
		board.increaseViewCount();
		List<CommentResponse> comments = commentRepository.findByBoardIdOrderByCreatedAtAsc(boardId).stream()
			.map(this::toCommentResponse).toList();
		return toBoardResponse(board, comments);
	}

	/** 게시글 작성자만 내용을 수정할 수 있고, 공지로 지정하는 경우에는 관리자 권한도 확인해 일반 회원의 공지 등록을 막습니다. */
	@Transactional
	public BoardResponse updateBoard(Long boardId, Long memberId, BoardUpdateRequest request) {
		// 게시글·회원 조회 → 작성자와 공지 권한 확인 → 내용 수정 → 댓글을 포함한 응답 변환 순서로 처리합니다.
		Board board = findBoard(boardId);
		Member member = findMember(memberId);
		validateAuthor(board.getAuthor(), member, new BoardAccessDeniedException());
		validateNoticePermission(member, request.notice());
		board.update(request.title().trim(), request.content().trim(), request.notice());
		List<CommentResponse> comments = commentRepository.findByBoardIdOrderByCreatedAtAsc(boardId).stream()
			.map(this::toCommentResponse).toList();
		return toBoardResponse(board, comments);
	}

	/** 작성자 또는 관리자만 게시글을 삭제할 수 있으며, 게시글과 댓글의 관계가 어긋나지 않도록 연관 댓글도 함께 제거합니다. */
	@Transactional
	public void deleteBoard(Long boardId, Long memberId) {
		// 게시글·회원 조회 → 삭제 권한 확인 → 게시글 삭제 순서로 처리합니다.
		Board board = findBoard(boardId);
		Member member = findMember(memberId);
		validateDeletePermission(board.getAuthor(), member, new BoardAccessDeniedException());
		boardRepository.delete(board);
	}

	/** 관리자 권한을 확인한 뒤 작성자와 관계없이 공지 상태를 변경해 관리자의 공지 관리를 허용합니다. */
	@Transactional
	public void updateNoticeByAdmin(Long boardId, Long memberId, boolean notice) {
		// 관리자 권한 확인 → 게시글 조회 → 공지 상태 변경 순서로 처리합니다.
		Member member = findMember(memberId);
		validateAdmin(member);
		Board board = findBoard(boardId);
		board.update(board.getTitle(), board.getContent(), notice);
	}

	/** 게시글 ID 없이 댓글 ID로 삭제하되, 관리자만 작성자 확인 없이 삭제할 수 있도록 권한을 먼저 확인합니다. */
	@Transactional
	public void deleteCommentByAdmin(Long commentId, Long memberId) {
		// 관리자 권한 확인 → 댓글 조회 → 댓글 삭제 순서로 처리합니다.
		Member member = findMember(memberId);
		validateAdmin(member);
		Comment comment = commentRepository.findById(commentId).orElseThrow(CommentNotFoundException::new);
		commentRepository.delete(comment);
	}

	/** URL의 게시글 ID로 댓글의 소속 게시글을 지정하고, 인증된 회원을 작성자로 저장해 댓글 관계를 보장합니다. */
	@Transactional
	public CommentResponse createComment(Long boardId, Long memberId, CommentCreateRequest request) {
		// 게시글·회원 조회 → 댓글 저장 → 댓글 응답 변환 순서로 처리합니다.
		Board board = findBoard(boardId);
		Member author = findMember(memberId);
		Comment comment = commentRepository.save(new Comment(request.content().trim(), board, author));
		return toCommentResponse(comment);
	}

	/** 댓글이 URL의 게시글에 속하는지 먼저 확인하고 작성자만 수정하게 해 다른 게시글을 통한 우회 수정을 막습니다. */
	@Transactional
	public CommentResponse updateComment(Long boardId, Long commentId, Long memberId, CommentUpdateRequest request) {
		// 게시글 소속 댓글·회원 조회 → 작성자 권한 확인 → 내용 수정 및 응답 변환 순서로 처리합니다.
		Comment comment = findCommentInBoard(boardId, commentId);
		Member member = findMember(memberId);
		validateAuthor(comment.getAuthor(), member, new CommentAccessDeniedException());
		comment.update(request.content().trim());
		return toCommentResponse(comment);
	}

	/** 댓글이 URL의 게시글에 속하는지 확인한 뒤 작성자 또는 관리자만 삭제하게 해 다른 게시글 URL을 통한 우회 삭제를 막습니다. */
	@Transactional
	public void deleteComment(Long boardId, Long commentId, Long memberId) {
		// 게시글 소속 댓글·회원 조회 → 삭제 권한 확인 → 댓글 삭제 순서로 처리합니다.
		Comment comment = findCommentInBoard(boardId, commentId);
		Member member = findMember(memberId);
		validateDeletePermission(comment.getAuthor(), member, new CommentAccessDeniedException());
		commentRepository.delete(comment);
	}

	/** 존재하지 않는 게시글 ID는 게시글 전용 예외로 변환합니다. */
	private Board findBoard(Long boardId) {
		return boardRepository.findById(boardId).orElseThrow(BoardNotFoundException::new);
	}

	/** 회원이 탈퇴했거나 잘못된 ID인 경우 권한 오류로 처리합니다. */
	private Member findMember(Long memberId) {
		return memberRepository.findById(memberId).orElseThrow(BoardAccessDeniedException::new);
	}

	/** 댓글 ID뿐 아니라 URL의 게시글 ID와 실제 소속 게시글도 일치하는지 확인합니다. */
	private Comment findCommentInBoard(Long boardId, Long commentId) {
		Comment comment = commentRepository.findById(commentId).orElseThrow(CommentNotFoundException::new);
		if (!comment.getBoard().getId().equals(boardId)) {
			throw new CommentNotFoundException();
		}
		return comment;
	}

	/** 공지로 지정하는 요청은 관리자에게만 허용합니다. */
	private void validateNoticePermission(Member member, boolean notice) {
		if (notice && member.getRole() != Role.ADMIN) {
			throw new BoardAccessDeniedException();
		}
	}

	/** 수정 작업의 대상 작성자와 요청 회원이 같은지 검사합니다. */
	private void validateAuthor(Member author, Member member, RuntimeException exception) {
		if (!author.getId().equals(member.getId())) {
			throw exception;
		}
	}

	/** 삭제는 작성자와 관리자에게 허용하고, 그 외 회원은 거부합니다. */
	private void validateDeletePermission(Member author, Member member, RuntimeException exception) {
		if (!author.getId().equals(member.getId()) && member.getRole() != Role.ADMIN) {
			throw exception;
		}
	}

	/** 관리자 전용 작업을 실행하기 전에 역할을 확인합니다. */
	private void validateAdmin(Member member) {
		if (member.getRole() != Role.ADMIN) {
			throw new BoardAccessDeniedException();
		}
	}

	/** 목록 화면에 필요한 게시글 요약 정보만 응답 DTO로 옮깁니다. */
	private BoardListResponse toBoardListResponse(Board board) {
		return new BoardListResponse(board.getId(), board.getTitle(), board.isNotice(), board.getAuthor().getId(),
			board.getAuthor().getName(), board.getViewCount(), board.getCreatedAt());
	}

	/** 게시글 상세 정보와 이미 조회한 댓글 목록을 응답 DTO로 구성합니다. */
	private BoardResponse toBoardResponse(Board board, List<CommentResponse> comments) {
		return new BoardResponse(board.getId(), board.getTitle(), board.getContent(), board.getViewCount(), board.isNotice(),
			board.getAuthor().getId(), board.getAuthor().getName(), board.getCreatedAt(), board.getUpdatedAt(), comments);
	}

	/** 댓글 엔티티의 작성자와 작성·수정 시각을 API 응답 형태로 변환합니다. */
	private CommentResponse toCommentResponse(Comment comment) {
		return new CommentResponse(comment.getId(), comment.getContent(), comment.getAuthor().getId(),
			comment.getAuthor().getName(), comment.getCreatedAt(), comment.getUpdatedAt());
	}

}
