package com.cleaner.be.board;

public class CommentAccessDeniedException extends RuntimeException {
	public CommentAccessDeniedException() { super("댓글을 수정하거나 삭제할 권한이 없습니다."); }
}
