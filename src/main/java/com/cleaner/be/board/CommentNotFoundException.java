package com.cleaner.be.board;

/** 요청한 ID의 댓글이 없거나 URL에 지정된 게시글에 속하지 않을 때 발생합니다. */
public class CommentNotFoundException extends RuntimeException {
	public CommentNotFoundException() { super("댓글을 찾을 수 없습니다."); }
}
