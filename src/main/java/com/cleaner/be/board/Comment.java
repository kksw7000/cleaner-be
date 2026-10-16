package com.cleaner.be.board;

import com.cleaner.be.auth.Member;
import com.cleaner.be.common.entity.BaseTimeEntity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

/** 댓글은 반드시 하나의 게시글에 속하며 게시글 밖으로 이동할 수 없습니다. */
@Entity
@Table(name = "comments")
public class Comment extends BaseTimeEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(nullable = false, columnDefinition = "TEXT")
	private String content;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	private Board board;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	private Member author;

	protected Comment() {
	}

	public Comment(String content, Board board, Member author) {
		this.content = content;
		this.board = board;
		this.author = author;
	}

	public Long getId() { return id; }
	public String getContent() { return content; }
	public Board getBoard() { return board; }
	public Member getAuthor() { return author; }

	/** 작성자 권한을 서비스에서 확인한 뒤 검증된 댓글 내용만 교체합니다. */
	public void update(String content) {
		this.content = content;
	}
}
