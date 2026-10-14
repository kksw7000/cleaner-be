package com.cleaner.be.board;

/** 게시글 작성·수정·삭제 또는 관리자 전용 작업의 권한이 없을 때 발생합니다. */
public class BoardAccessDeniedException extends RuntimeException {
	public BoardAccessDeniedException() { super("게시글을 수정하거나 삭제할 권한이 없습니다."); }
}
