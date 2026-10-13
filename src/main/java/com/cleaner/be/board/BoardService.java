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

	/** 로그인 회원을 작성자로 연결해 게시글을 만들고, 공지 여부는 ADMIN만 지정하도록 제한합니다. */
	@Transactional
	public BoardResponse createBoard(Long memberId, BoardCreateRequest request) {
		Member author = findMember(memberId);
		validateNoticePermission(author, request.notice());
		Board board = boardRepository.save(new Board(request.title().trim(), request.content().trim(), request.notice(), author));
		return toBoardResponse(board, List.of());
	}

	/** 공지를 우선 배치한 뒤 최신 작성순으로 목록을 반환하고, 키워드가 있으면 제목·본문을 함께 검색합니다. */
	@Transactional(readOnly = true)
	public BoardPageResponse getBoards(int page, int size, String keyword) {
		PageRequest pageRequest = PageRequest.of(page, size, Sort.by(
			Sort.Order.desc("notice"), Sort.Order.desc("createdAt")));
		String normalizedKeyword = keyword == null ? "" : keyword.trim();
		Page<Board> boards = normalizedKeyword.isEmpty()
			? boardRepository.findAll(pageRequest)
			: boardRepository.findByTitleContainingIgnoreCaseOrContentContainingIgnoreCase(
				normalizedKeyword, normalizedKeyword, pageRequest);

		return new BoardPageResponse(
			boards.map(this::toBoardListResponse).getContent(), boards.getNumber(), boards.getSize(),
			boards.getTotalElements(), boards.getTotalPages(), boards.isFirst(), boards.isLast());
	}

	/** 공개 상세 조회에서만 조회수를 증가시키고, 댓글은 작성 시간순 DTO 목록으로 함께 제공합니다. */
	@Transactional
	public BoardResponse getBoard(Long boardId) {
		Board board = findBoard(boardId);
		board.increaseViewCount();
		List<CommentResponse> comments = commentRepository.findByBoardIdOrderByCreatedAtAsc(boardId).stream()
			.map(this::toCommentResponse).toList();
		return toBoardResponse(board, comments);
	}

	/** 작성자만 본문을 수정할 수 있으며, 공지 상태를 켜거나 유지하는 요청은 ADMIN인지도 확인합니다. */
	@Transactional
	public BoardResponse updateBoard(Long boardId, Long memberId, BoardUpdateRequest request) {
		Board board = findBoard(boardId);
		Member member = findMember(memberId);
		validateAuthor(board.getAuthor(), member, new BoardAccessDeniedException());
		validateNoticePermission(member, request.notice());
		board.update(request.title().trim(), request.content().trim(), request.notice());
		List<CommentResponse> comments = commentRepository.findByBoardIdOrderByCreatedAtAsc(boardId).stream()
			.map(this::toCommentResponse).toList();
		return toBoardResponse(board, comments);
	}

	/** 작성자 또는 ADMIN만 게시글을 삭제할 수 있으며, 연관 댓글은 엔티티 규칙에 따라 함께 제거됩니다. */
	@Transactional
	public void deleteBoard(Long boardId, Long memberId) {
		Board board = findBoard(boardId);
		Member member = findMember(memberId);
		validateDeletePermission(board.getAuthor(), member, new BoardAccessDeniedException());
		boardRepository.delete(board);
	}

	/** 관리자가 작성자와 관계없이 공지 상태를 변경할 때 사용하는 게시판 도메인 규칙입니다. */
	@Transactional
	public void updateNoticeByAdmin(Long boardId, Long memberId, boolean notice) {
		Member member = findMember(memberId);
		validateAdmin(member);
		Board board = findBoard(boardId);
		board.update(board.getTitle(), board.getContent(), notice);
	}

	/** 관리자 댓글 목록은 작성자·원문 게시글을 함께 조회해 별도 도메인 조회 로직을 만들지 않습니다. */
	@Transactional(readOnly = true)
	public Page<Comment> getAllComments(int page, int size) {
		return commentRepository.findAll(PageRequest.of(page, size, Sort.by(Sort.Order.desc("createdAt"))));
	}

	/** URL에 게시글 ID가 없는 관리자 강제 삭제도 기존 댓글 삭제 규칙과 같은 저장소를 사용합니다. */
	@Transactional
	public void deleteCommentByAdmin(Long commentId, Long memberId) {
		Member member = findMember(memberId);
		validateAdmin(member);
		Comment comment = commentRepository.findById(commentId).orElseThrow(CommentNotFoundException::new);
		commentRepository.delete(comment);
	}

	/** 댓글은 URL의 게시글 ID로 종속 관계를 확정하고 로그인 회원을 작성자로 저장합니다. */
	@Transactional
	public CommentResponse createComment(Long boardId, Long memberId, CommentCreateRequest request) {
		Board board = findBoard(boardId);
		Member author = findMember(memberId);
		Comment comment = commentRepository.save(new Comment(request.content().trim(), board, author));
		return toCommentResponse(comment);
	}

	@Transactional
	public CommentResponse updateComment(Long boardId, Long commentId, Long memberId, CommentUpdateRequest request) {
		Comment comment = findCommentInBoard(boardId, commentId);
		Member member = findMember(memberId);
		validateAuthor(comment.getAuthor(), member, new CommentAccessDeniedException());
		comment.update(request.content().trim());
		return toCommentResponse(comment);
	}

	/** 댓글 삭제는 해당 게시글에 속한 댓글인지 먼저 확인해 다른 게시글 URL로의 우회 접근을 막습니다. */
	@Transactional
	public void deleteComment(Long boardId, Long commentId, Long memberId) {
		Comment comment = findCommentInBoard(boardId, commentId);
		Member member = findMember(memberId);
		validateDeletePermission(comment.getAuthor(), member, new CommentAccessDeniedException());
		commentRepository.delete(comment);
	}

	private Board findBoard(Long boardId) {
		return boardRepository.findById(boardId).orElseThrow(BoardNotFoundException::new);
	}

	private Member findMember(Long memberId) {
		return memberRepository.findById(memberId).orElseThrow(BoardAccessDeniedException::new);
	}

	private Comment findCommentInBoard(Long boardId, Long commentId) {
		Comment comment = commentRepository.findById(commentId).orElseThrow(CommentNotFoundException::new);
		if (!comment.getBoard().getId().equals(boardId)) {
			throw new CommentNotFoundException();
		}
		return comment;
	}

	private void validateNoticePermission(Member member, boolean notice) {
		if (notice && member.getRole() != Role.ADMIN) {
			throw new BoardAccessDeniedException();
		}
	}

	private void validateAuthor(Member author, Member member, RuntimeException exception) {
		if (!author.getId().equals(member.getId())) {
			throw exception;
		}
	}

	private void validateDeletePermission(Member author, Member member, RuntimeException exception) {
		if (!author.getId().equals(member.getId()) && member.getRole() != Role.ADMIN) {
			throw exception;
		}
	}

	private void validateAdmin(Member member) {
		if (member.getRole() != Role.ADMIN) {
			throw new BoardAccessDeniedException();
		}
	}

	private BoardListResponse toBoardListResponse(Board board) {
		return new BoardListResponse(board.getId(), board.getTitle(), board.isNotice(), board.getAuthor().getId(),
			board.getAuthor().getName(), board.getViewCount(), board.getCreatedAt());
	}

	private BoardResponse toBoardResponse(Board board, List<CommentResponse> comments) {
		return new BoardResponse(board.getId(), board.getTitle(), board.getContent(), board.getViewCount(), board.isNotice(),
			board.getAuthor().getId(), board.getAuthor().getName(), board.getCreatedAt(), board.getUpdatedAt(), comments);
	}

	private CommentResponse toCommentResponse(Comment comment) {
		return new CommentResponse(comment.getId(), comment.getContent(), comment.getAuthor().getId(),
			comment.getAuthor().getName(), comment.getCreatedAt(), comment.getUpdatedAt());
	}

}
