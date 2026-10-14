package com.cleaner.be.board;

/** 요청한 ID의 게시글이 존재하지 않을 때 발생하는 도메인 예외입니다. */
public class BoardNotFoundException extends RuntimeException {
	public BoardNotFoundException() { super("게시글을 찾을 수 없습니다."); }
}
