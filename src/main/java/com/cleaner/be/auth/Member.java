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
}
