package com.cleaner.be.board;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

/** 게시글과 방문자 조합을 저장해 같은 방문자의 재조회가 조회수에 반복 반영되지 않게 합니다. */
@Entity
@Table(name = "board_views", uniqueConstraints = @UniqueConstraint(
	name = "uk_board_views_board_viewer", columnNames = {"board_id", "viewer_key"}))
public class BoardView {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(name = "board_id", nullable = false)
	private Long boardId;

	@Column(name = "viewer_key", nullable = false, length = 80)
	private String viewerKey;

	protected BoardView() {
	}

	/** 게시글 ID와 방문자 키를 방문 기록으로 저장합니다. */
	public BoardView(Long boardId, String viewerKey) {
		this.boardId = boardId;
		this.viewerKey = viewerKey;
	}
}
