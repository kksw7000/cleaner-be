# MyPageService 학습 가이드

## 1. 역할·레이어·왜 필요한가·언제 동작하는가

`MyPageService`는 **서비스 계층**으로, 마이페이지 기능의 데이터 조회와 비즈니스 규칙을 처리합니다. 현재 회원 확인, 프로필 값 정규화, 이메일·전화번호 중복 검사, 현재 비밀번호 확인, 비밀번호 해시 변경이 이 클래스의 책임입니다. Controller가 HTTP 요청을 받아 이 서비스의 메서드를 호출할 때 실행됩니다.

Controller와 DB 접근 사이에 서비스를 두면 입력/응답 경계와 회원 규칙을 분리할 수 있습니다. [`MemberRepository`](../../src/main/java/com/cleaner/be/auth/MemberRepository.java)로 엔티티를 조회하고, [`Member`](../../src/main/java/com/cleaner/be/auth/Member.java)의 도메인 메서드로 값을 바꿉니다.

## 2. 현재 전체 소스

아래는 [MyPageService.java](../../src/main/java/com/cleaner/be/mypage/MyPageService.java)의 전체 내용입니다.

```java
package com.cleaner.be.mypage;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import com.cleaner.be.auth.DuplicateEmailException;
import com.cleaner.be.auth.DuplicatePhoneNumberException;
import com.cleaner.be.auth.InvalidCredentialsException;
import com.cleaner.be.auth.InvalidCurrentPasswordException;
import com.cleaner.be.auth.Member;
import com.cleaner.be.auth.MemberRepository;
import com.cleaner.be.mypage.dto.MyPageResponse;
import com.cleaner.be.mypage.dto.ChangePasswordRequest;
import com.cleaner.be.mypage.dto.UpdateMyPageRequest;

/**
 * 마이페이지에서 현재 회원의 프로필을 조회하는 서비스입니다.
 * Controller가 인증된 회원 ID를 전달하면 저장된 회원을 다시 확인해, 탈퇴 후 남은 인증 정보가 노출되지 않게 합니다.
 */
@Service
public class MyPageService {

	private final MemberRepository memberRepository;
	private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

	public MyPageService(MemberRepository memberRepository) {
		this.memberRepository = memberRepository;
	}

	/**
	 * JWT 인증을 통과한 회원의 마이페이지 정보를 반환합니다.
	 * DB 엔티티를 그대로 반환하지 않아 비밀번호 해시와 내부 소셜 식별자가 API 응답에 포함되지 않습니다.
	 */
	@Transactional(readOnly = true)
	public MyPageResponse getMyPage(Long memberId) {
		Member member = memberRepository.findById(memberId)
			.orElseThrow(InvalidCredentialsException::new);

		return toResponse(member);
	}

	/**
	 * 인증된 회원이 입력한 프로필을 정규화한 뒤 중복 여부를 확인하고 저장합니다.
	 * 수정 대상 본인의 기존 이메일·번호는 중복으로 판단하지 않아 이름만 바꾸는 요청도 정상 처리됩니다.
	 */
	@Transactional
	public MyPageResponse updateMyPage(Long memberId, UpdateMyPageRequest request) {
		Member member = memberRepository.findById(memberId)
			.orElseThrow(InvalidCredentialsException::new);
		// 가입 처리와 같은 기준을 적용해 조회·로그인에 사용되는 이메일 형식을 일관되게 유지합니다.
		String email = request.email().trim().toLowerCase();
		String phoneNumber = request.phoneNumber().replaceAll("[^0-9]", "");

		if (!member.getEmail().equals(email) && memberRepository.findByEmail(email).isPresent()) {
			throw new DuplicateEmailException();
		}
		if (!member.getPhoneNumber().equals(phoneNumber) && memberRepository.findByPhoneNumber(phoneNumber).isPresent()) {
			throw new DuplicatePhoneNumberException();
		}

		// JPA가 트랜잭션 종료 시 변경 감지를 수행하므로 별도의 save 호출 없이 한 번에 반영됩니다.
		member.updateProfile(email, request.name().trim(), phoneNumber);
		return toResponse(member);
	}

	/**
	 * 현재 비밀번호를 BCrypt 해시와 비교한 뒤 새 비밀번호 해시로 교체합니다.
	 * 소셜 로그인처럼 비밀번호 해시가 없는 계정도 현재 비밀번호 검증을 통과할 수 없으므로 임의로 비밀번호를 설정할 수 없습니다.
	 */
	@Transactional
	public void changePassword(Long memberId, ChangePasswordRequest request) {
		Member member = memberRepository.findById(memberId)
			.orElseThrow(InvalidCredentialsException::new);

		if (member.getPassword() == null || !passwordEncoder.matches(request.currentPassword(), member.getPassword())) {
			throw new InvalidCurrentPasswordException();
		}

		// 새 원문은 즉시 BCrypt 해시로 변환하며 DB에는 해시값만 변경 감지로 저장됩니다.
		member.updatePassword(passwordEncoder.encode(request.newPassword()));
	}

	/** 엔티티 내부 인증 정보를 제외하고 마이페이지에 공개할 프로필 필드만 반환합니다. */
	private MyPageResponse toResponse(Member member) {
		return new MyPageResponse(member.getId(), member.getEmail(), member.getName(), member.getPhoneNumber());
	}
}
```

## 3. import·어노테이션·필드·메서드·키워드 설명

- `@Service`는 이 클래스를 Spring의 서비스 구성요소로 등록합니다. `@Transactional`은 메서드 실행을 DB 트랜잭션으로 감쌉니다. `readOnly = true`는 조회 작업임을 표시합니다.
- `MemberRepository`는 Spring Data JPA 저장소이고 `Member`는 회원 엔티티입니다. 네 예외는 인증 실패, 현재 비밀번호 불일치, 이메일 중복, 전화번호 중복을 나타냅니다. DTO 세 개는 요청과 응답 데이터입니다.
- `BCryptPasswordEncoder`는 비밀번호 원문을 일방향 해시로 인코딩하고, 입력 원문이 저장된 해시와 맞는지 `matches`로 확인합니다.
- `memberRepository`는 저장소 의존성이고 `passwordEncoder`는 해시 작업을 수행하는 객체입니다. 둘 다 필드이며 `final` 저장소는 생성자에서 한 번 설정됩니다.
- 생성자는 저장소를 받아 필드에 주입합니다. `getMyPage`, `updateMyPage`, `changePassword`는 Controller에서 호출하는 서비스 작업입니다. `toResponse`는 엔티티를 공개 응답 DTO로 바꾸는 내부 보조 메서드입니다.
- `findById`와 `findByEmail`/`findByPhoneNumber`는 `Optional`을 반환합니다. `orElseThrow(InvalidCredentialsException::new)`는 결과가 비었을 때 예외를 생성해 던지는 메서드 참조 문법입니다.
- `trim()`은 앞뒤 공백, `toLowerCase()`는 이메일 대소문자 차이를 정리합니다. `replaceAll("[^0-9]", "")`는 숫자가 아닌 문자를 모두 없애 전화번호를 숫자만 남깁니다.
- `!`는 논리 부정, `&&`는 AND입니다. 기존 값이 새 값과 다를 때만 저장소에서 중복을 조회하여 자기 자신의 현재 값을 중복으로 오판하지 않습니다.
- `updateProfile`/`updatePassword`는 엔티티 메서드입니다. 트랜잭션 안에서 JPA가 관리 중인 엔티티 필드 변경을 감지해 반영하므로 이 코드에는 명시적 `save`가 없습니다.
- `return`은 결과를 호출자에게 돌려줍니다. `void` 메서드는 값 없이 끝납니다. `throw`는 실패를 나타내고 호출 흐름을 중단합니다.

## 4. 각 메서드의 목적·입력·순서·호출·결과·예외

### 생성자

`MemberRepository`를 입력받아 저장소 필드에 보관합니다. Spring의 생성자 주입 방식입니다.

### `getMyPage(Long memberId)`

회원 ID를 받아 저장소에서 회원을 조회합니다. 없으면 `InvalidCredentialsException`을 던지고, 있으면 `toResponse`로 id·email·name·phoneNumber만 담아 반환합니다. 읽기 전용 트랜잭션입니다.

### `updateMyPage(Long memberId, UpdateMyPageRequest request)`

1. ID로 회원을 찾고 없으면 인증 오류를 던집니다.
2. 이메일은 앞뒤 공백 제거 후 소문자로 바꾸고, 전화번호는 숫자만 남깁니다.
3. 새 이메일이 현재 이메일과 달라졌을 때만 다른 회원이 사용하는지 조회합니다. 사용 중이면 `DuplicateEmailException`입니다.
4. 새 번호가 현재 번호와 달라졌을 때만 중복 확인을 하고, 중복이면 `DuplicatePhoneNumberException`입니다.
5. 엔티티의 `updateProfile`에 정리된 이메일·번호와 앞뒤 공백을 제거한 이름을 전달하고 응답 DTO를 반환합니다.

이 메서드는 `@Transactional`입니다. 요청 DTO가 검증되는 시점은 Controller의 `@Valid` 입력 경계입니다.

### `changePassword(Long memberId, ChangePasswordRequest request)`

ID로 회원을 찾습니다. 회원의 암호가 없거나 `matches(현재 원문, 저장된 해시)`가 거짓이면 `InvalidCurrentPasswordException`을 던집니다. 검증을 통과하면 새 비밀번호를 BCrypt로 인코딩하고 해시값만 엔티티의 `updatePassword`에 전달합니다. 반환은 없으며 트랜잭션 종료 시 변경이 반영됩니다.

### `toResponse(Member member)`

엔티티에서 id, email, name, phoneNumber만 골라 `MyPageResponse`를 생성합니다. password 및 OAuth 식별자는 DTO에 옮기지 않습니다. 서비스 내부에서만 쓰는 `private` 메서드입니다.

## 5. 실제 실행 흐름

프로필 변경 요청이 들어오면 Controller가 인증 회원 ID와 `UpdateMyPageRequest`를 전달합니다. 이 서비스는 ID로 엔티티를 다시 불러오고 입력값을 정규화한 후 다른 계정과 충돌하는지 검사합니다. 문제가 없으면 엔티티 프로필을 바꾸고 응답 DTO를 만듭니다. 트랜잭션이 정상 종료되면 JPA 변경 감지가 DB에 값을 반영합니다.

비밀번호 변경에서는 DB에서 회원을 찾은 다음 현재 원문을 기존 BCrypt 해시와 비교합니다. 비교 실패는 예외가 되어 롤백되고, 성공 시 새 원문은 BCrypt 해시로 바뀌어 엔티티에 설정됩니다. 현재 비밀번호가 `null`인 OAuth 계정은 이 검증을 통과할 수 없습니다.

## 6. 확인된 관계와 추론을 구분하기

**소스에서 확인됨:** 이 서비스가 `MemberRepository`를 호출하며 [`Member`](../../src/main/java/com/cleaner/be/auth/Member.java)의 `updateProfile`과 `updatePassword`를 사용합니다. 조회 결과가 없으면 `InvalidCredentialsException`을 던집니다. 중복 예외와 현재 비밀번호 예외는 [`GlobalExceptionHandler`](../../src/main/java/com/cleaner/be/auth/GlobalExceptionHandler.java)에서 각각 409, 400으로 매핑됩니다. 이 클래스에서 명시적 `save` 호출은 없습니다.

**합리적인 해석/추론:** JPA가 트랜잭션 종료 시 엔티티 변경을 감지한다는 점은 실제 동작이 JPA 영속성 컨텍스트 설정에 의존합니다. 현재 저장소가 `JpaRepository`를 상속하고 서비스가 트랜잭션을 열기 때문에 일반적인 Spring Data JPA 구성에서 기대되는 동작입니다. 구체적인 SQL 실행 시점은 구현 및 flush 시점에 따라 달라질 수 있습니다.

## 7. 사용된 개념

- **서비스 계층과 트랜잭션:** 하나의 업무 작업을 조회·검증·변경으로 묶습니다.
- **정규화:** 이메일과 번호 형식을 통일해 비교와 저장을 일관되게 합니다.
- **Optional:** 조회 결과가 없을 수 있음을 표현합니다.
- **도메인 예외:** 실패 이유를 구체적인 예외 타입으로 나타냅니다.
- **BCrypt:** 원문 비밀번호를 복구 가능한 평문으로 저장하지 않고 해시로 비교합니다.
- **JPA 변경 감지:** 관리되는 엔티티의 필드 변경을 트랜잭션 안에서 추적합니다.
- **DTO 변환:** 엔티티 내부 보안 필드를 API 응답에 포함시키지 않습니다.

## 8. 동작에 맞는 예시

저장된 전화번호가 `01012345678`, 이메일이 `old@example.com`인 회원이 다음 입력을 제출한다고 가정합니다.

```json
{"email":" New@Example.com ","name":" 김하늘 ","phoneNumber":"010-1234-5678"}
```

서비스 내부 값은 이메일 `new@example.com`, 번호 `01012345678`, 이름 `김하늘`로 정리됩니다. 이메일이 바뀌었다면 중복 조회를 하고, 전화번호가 바뀌지 않았다면 전화번호 중복 조회는 생략합니다. 다른 회원과 겹치지 않으면 프로필을 변경합니다. 이 예시는 입력 정규화의 동작을 설명하기 위한 것이며 DB에 같은 데이터가 존재한다고 가정하지 않습니다.

## 9. 요약

`MyPageService`는 마이페이지의 실제 업무 규칙을 수행합니다. 회원 존재 확인, 프로필 정규화와 중복 검사, 현재 비밀번호 확인 및 새 BCrypt 해시 저장, 공개 응답 DTO 생성을 담당합니다. 트랜잭션과 JPA 엔티티 변경을 통해 정상 처리 결과를 저장합니다.
