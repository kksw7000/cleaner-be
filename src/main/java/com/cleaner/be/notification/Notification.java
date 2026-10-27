package com.cleaner.be.notification;

import java.time.LocalDateTime;

import com.cleaner.be.auth.Member;
import com.cleaner.be.reservation.Reservation;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

/** 회원에게 보여줄 알림을 보관하고 예약 삭제 후에도 알림 문구를 유지합니다. */
@Entity
@Table(name = "notifications")
public class Notification {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "member_id", nullable = false)
	private Member member;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "reservation_id")
	private Reservation reservation;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 40)
	private NotificationType type;

	@Column(nullable = false, length = 200)
	private String message;

	@Column(nullable = false, updatable = false)
	private LocalDateTime createdAt;

	@Column
	private LocalDateTime readAt;

	protected Notification() {
	}

	public Notification(Member member, Reservation reservation, NotificationType type, String message, LocalDateTime createdAt) {
		this.member = member;
		this.reservation = reservation;
		this.type = type;
		this.message = message;
		this.createdAt = createdAt;
	}

	public Long getId() { return id; }
	public Member getMember() { return member; }
	public Reservation getReservation() { return reservation; }
	public NotificationType getType() { return type; }
	public String getMessage() { return message; }
	public LocalDateTime getCreatedAt() { return createdAt; }
	public LocalDateTime getReadAt() { return readAt; }

	/** 회원이 알림을 확인한 최초 시각을 기록해 읽음 상태가 다시 미읽음으로 바뀌지 않게 합니다. */
	public void markAsRead(LocalDateTime readAt) {
		if (this.readAt == null) {
			this.readAt = readAt;
		}
	}
}
