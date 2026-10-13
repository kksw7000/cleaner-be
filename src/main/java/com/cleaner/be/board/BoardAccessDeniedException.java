package com.cleaner.be.board;

public class BoardAccessDeniedException extends RuntimeException {
	public BoardAccessDeniedException() { super("게시글을 수정하거나 삭제할 권한이 없습니다."); }
}
