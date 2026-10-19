package com.cleaner.be.reservation;

import java.time.LocalDateTime;

import com.cleaner.be.auth.Member;
import com.cleaner.be.common.entity.BaseTimeEntity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

/** 회원의 청소 요청과 관리자가 처리할 상태를 보관하며, 예약자는 회원 엔티티로 연결합니다. */
@Entity
@Table(name = "reservations")
public class Reservation extends BaseTimeEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 30)
	private ServiceCategory serviceCategory;

	@Column(nullable = false)
	private LocalDateTime scheduledAt;

	@Column(nullable = false, length = 200)
	private String address;

	@Column(nullable = false, length = 30)
	private String contactName;

	@Column(nullable = false, length = 20)
	private String contactPhone;

	@Column(columnDefinition = "TEXT")
	private String requestNote;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 20)
	private ReservationStatus status = ReservationStatus.PENDING;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	private Member member;

	protected Reservation() {
	}

	/** 생성 시에는 관리자가 아직 검토하지 않았으므로 모든 예약을 PENDING 상태로 시작합니다. */
	public Reservation(ServiceCategory serviceCategory, LocalDateTime scheduledAt, String address, String contactName,
		String contactPhone, String requestNote, Member member) {
		this.serviceCategory = serviceCategory;
		this.scheduledAt = scheduledAt;
		this.address = address;
		this.contactName = contactName;
		this.contactPhone = contactPhone;
		this.requestNote = requestNote;
		this.member = member;
	}

	public Long getId() { return id; }
	public ServiceCategory getServiceCategory() { return serviceCategory; }
	public LocalDateTime getScheduledAt() { return scheduledAt; }
	public String getAddress() { return address; }
	public String getContactName() { return contactName; }
	public String getContactPhone() { return contactPhone; }
	public String getRequestNote() { return requestNote; }
	public ReservationStatus getStatus() { return status; }
	public Member getMember() { return member; }

	/** 관리자만 검증된 상태를 전달해 예약 진행 상태가 임의로 바뀌지 않도록 합니다. */
	public void updateStatus(ReservationStatus status) {
		this.status = status;
	}
}
