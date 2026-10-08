package com.cleaner.be.auth;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "members")
public class Member {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	// DB의 unique 제약도 두어 동시에 가입 요청이 와도 이메일 중복을 방지합니다.
	@Column(nullable = false, unique = true, length = 50)
	private String email;

	// BCrypt 해시값을 저장하는 컬럼입니다. 원문 비밀번호를 넣으면 안 됩니다.
	@Column
	private String password;

	@Column(nullable = false, length = 30)
	private String name;

	// 숫자만 저장한 휴대폰번호입니다. 같은 번호로 중복 가입하지 못하게 합니다.
	@Column(unique = true, length = 11)
	private String phoneNumber;

	@Column(length = 30)
	private String oauthProvider;

	@Column(length = 100)
	private String oauthProviderUserId;

	protected Member() {
	}

	public Member(String email, String password, String name, String phoneNumber) {
		this.email = email;
		this.password = password;
		this.name = name;
		this.phoneNumber = phoneNumber;
	}

	static Member fromOAuth(String email, String name, String oauthProvider, String oauthProviderUserId) {
		Member member = new Member();
		member.email = email;
		member.name = name;
		member.oauthProvider = oauthProvider;
		member.oauthProviderUserId = oauthProviderUserId;
		return member;
	}

	public Long getId() {
		return id;
	}

	public String getEmail() {
		return email;
	}

	/** 로그인 시 BCrypt 해시 비교에만 사용하며, API 응답에는 노출하지 않습니다. */
	public String getPassword() {
		return password;
	}

	public String getName() {
		return name;
	}

	public String getPhoneNumber() {
		return phoneNumber;
	}

	public String getOauthProvider() {
		return oauthProvider;
	}

	public String getOauthProviderUserId() {
		return oauthProviderUserId;
	}

	/**
	 * 마이페이지에서 본인이 수정한 공개 프로필만 반영합니다.
	 * 비밀번호와 소셜 로그인 연결 정보는 별도의 보안 절차가 필요한 값이므로 이 메서드에서 변경하지 않습니다.
	 */
	public void updateProfile(String email, String name, String phoneNumber) {
		this.email = email;
		this.name = name;
		this.phoneNumber = phoneNumber;
	}

	/**
	 * 현재 비밀번호 검증을 통과하고 BCrypt 해시로 변환된 값만 저장합니다.
	 * 원문 비밀번호를 엔티티에 전달하거나 저장하는 경로를 만들지 않기 위해 해시값만 받습니다.
	 */
	public void updatePassword(String encodedPassword) {
		this.password = encodedPassword;
	}
}
