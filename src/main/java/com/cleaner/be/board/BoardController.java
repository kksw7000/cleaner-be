package com.cleaner.be.board;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.cleaner.be.auth.InvalidCredentialsException;
import com.cleaner.be.board.dto.BoardCreateRequest;
import com.cleaner.be.board.dto.BoardPageResponse;
import com.cleaner.be.board.dto.BoardResponse;
import com.cleaner.be.board.dto.BoardUpdateRequest;
import com.cleaner.be.board.dto.CommentCreateRequest;
import com.cleaner.be.board.dto.CommentResponse;
import com.cleaner.be.board.dto.CommentUpdateRequest;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;

/** 자유 게시판 HTTP 진입점입니다. 로그인 정보는 요청 본문 대신 JWT 필터가 넣은 SecurityContext에서만 읽습니다. */
@RestController
@Validated
@RequestMapping("/boards")
public class BoardController {

	private final BoardService boardService;

	public BoardController(BoardService boardService) {
		this.boardService = boardService;
	}

	@PostMapping
	@ResponseStatus(HttpStatus.CREATED)
	public BoardResponse createBoard(@Valid @RequestBody BoardCreateRequest request) {
		return boardService.createBoard(currentMemberId(), request);
	}

	@GetMapping
	public BoardPageResponse getBoards(
		@RequestParam(defaultValue = "0") @Min(0) int page,
		@RequestParam(defaultValue = "10") @Min(1) @Max(100) int size,
		@RequestParam(required = false) String keyword) {
		return boardService.getBoards(page, size, keyword);
	}

	@GetMapping("/{id}")
	public BoardResponse getBoard(@PathVariable Long id) {
		return boardService.getBoard(id);
	}

	@PatchMapping("/{id}")
	public BoardResponse updateBoard(@PathVariable Long id, @Valid @RequestBody BoardUpdateRequest request) {
		return boardService.updateBoard(id, currentMemberId(), request);
	}

	@DeleteMapping("/{id}")
	public ResponseEntity<Void> deleteBoard(@PathVariable Long id) {
		boardService.deleteBoard(id, currentMemberId());
		return ResponseEntity.noContent().build();
	}

	@PostMapping("/{id}/comments")
	@ResponseStatus(HttpStatus.CREATED)
	public CommentResponse createComment(@PathVariable Long id, @Valid @RequestBody CommentCreateRequest request) {
		return boardService.createComment(id, currentMemberId(), request);
	}

	@PatchMapping("/{id}/comments/{commentId}")
	public CommentResponse updateComment(@PathVariable Long id, @PathVariable Long commentId,
		@Valid @RequestBody CommentUpdateRequest request) {
		return boardService.updateComment(id, commentId, currentMemberId(), request);
	}

	@DeleteMapping("/{id}/comments/{commentId}")
	public ResponseEntity<Void> deleteComment(@PathVariable Long id, @PathVariable Long commentId) {
		boardService.deleteComment(id, commentId, currentMemberId());
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
