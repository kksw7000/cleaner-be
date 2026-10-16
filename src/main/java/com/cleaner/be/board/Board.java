package com.cleaner.be.board;

import java.util.ArrayList;
import java.util.List;

import com.cleaner.be.auth.Member;
import com.cleaner.be.common.entity.BaseTimeEntity;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;

/** 자유 게시판의 게시글과 게시글에 종속되는 댓글 컬렉션을 관리합니다. */
@Entity
@Table(name = "boards")
public class Board extends BaseTimeEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(nullable = false, length = 100)
	private String title;

	@Column(nullable = false, columnDefinition = "TEXT")
	private String content;

	@Column(nullable = false)
	private long viewCount;

	@Column(nullable = false)
	private boolean notice;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	private Member author;

	// 게시글이 삭제되면 독립적으로 존재할 수 없는 댓글도 함께 정리합니다.
	@OneToMany(mappedBy = "board", cascade = CascadeType.REMOVE, orphanRemoval = true)
	private List<Comment> comments = new ArrayList<>();

	protected Board() {
	}

	public Board(String title, String content, boolean notice, Member author) {
		this.title = title;
		this.content = content;
		this.notice = notice;
		this.author = author;
	}

	public Long getId() { return id; }
	public String getTitle() { return title; }
	public String getContent() { return content; }
	public long getViewCount() { return viewCount; }
	public boolean isNotice() { return notice; }
	public Member getAuthor() { return author; }
	public List<Comment> getComments() { return comments; }

	/** 수정 요청에서 검증된 값만 반영해 작성자와 조회수 같은 보호 대상은 변경하지 않습니다. */
	public void update(String title, String content, boolean notice) {
		this.title = title;
		this.content = content;
		this.notice = notice;
	}

	/** 상세 조회 시 열람 횟수를 엔티티 변경 감지로 반영합니다. */
	public void increaseViewCount() {
		viewCount++;
	}
}
