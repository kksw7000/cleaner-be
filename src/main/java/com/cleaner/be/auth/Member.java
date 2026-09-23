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
	@Column(nullable = false, unique = true, length = 255)
	private String email;

	// BCrypt 해시값을 저장하는 컬럼입니다. 원문 비밀번호를 넣으면 안 됩니다.
	@Column(nullable = false)
	private String password;

	@Column(nullable = false, length = 50)
	private String name;

	// 숫자만 저장한 휴대폰번호입니다. 같은 번호로 중복 가입하지 못하게 합니다.
	@Column(nullable = false, unique = true, length = 11)
	private String phoneNumber;

	protected Member() {
	}

	public Member(String email, String password, String name, String phoneNumber) {
		this.email = email;
		this.password = password;
		this.name = name;
		this.phoneNumber = phoneNumber;
	}

	public Long getId() {
		return id;
	}

	public String getEmail() {
		return email;
	}

	public String getName() {
		return name;
	}

	public String getPhoneNumber() {
		return phoneNumber;
	}
}
