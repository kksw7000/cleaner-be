package com.cleaner.be.board;

/** 댓글 작성·수정·삭제 권한이 없는 회원의 요청을 거부할 때 발생합니다. */
public class CommentAccessDeniedException extends RuntimeException {
	public CommentAccessDeniedException() { super("댓글을 수정하거나 삭제할 권한이 없습니다."); }
}
