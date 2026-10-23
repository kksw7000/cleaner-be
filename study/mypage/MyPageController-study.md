# MyPageController 학습 가이드

## 1. 역할·레이어·왜 필요한가·언제 동작하는가

`MyPageController`는 Spring MVC의 **웹/API 경계인 Controller**입니다. `/api/mypage`로 들어오는 프로필 조회·수정과 비밀번호 변경 요청을 받고, 현재 로그인한 회원 ID를 인증 정보에서 읽어 `MyPageService`에 전달합니다. Controller에서 직접 DB를 다루지 않고 서비스에 일을 위임하는 이유는 HTTP 요청 처리와 회원 관련 규칙을 분리하기 위해서입니다.

로그인 상태에서 마이페이지를 열거나 프로필/비밀번호 변경 API를 호출할 때 동작합니다. 요청 본문에서 회원 ID를 받지 않고 JWT 인증 정보를 기준으로 대상을 정하므로, 요청자가 임의로 다른 회원 ID를 넣어 타인의 정보를 읽거나 바꾸는 구조가 아닙니다. 이는 현재 코드 흐름에 대한 설명이며, 요청 보안 전체는 Spring Security 설정과 JWT 필터도 함께 결정합니다.

## 2. 현재 전체 소스

아래는 [MyPageController.java](../../src/main/java/com/cleaner/be/mypage/MyPageController.java)의 전체 내용입니다.

```java
package com.cleaner.be.mypage;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.cleaner.be.auth.InvalidCredentialsException;
import com.cleaner.be.mypage.dto.ChangePasswordRequest;
import com.cleaner.be.mypage.dto.MyPageResponse;
import com.cleaner.be.mypage.dto.UpdateMyPageRequest;

import jakarta.validation.Valid;

/**
 * 로그인 후 마이페이지에 필요한 회원 정보를 제공하는 API입니다.
 * JWT 필터가 검증한 회원 ID만 사용하므로, URL이나 요청 본문으로 다른 회원 정보를 조회할 수 없습니다.
 */
@RestController
@RequestMapping("/api/mypage")
public class MyPageController {

	private final MyPageService myPageService;

	public MyPageController(MyPageService myPageService) {
		this.myPageService = myPageService;
	}

	/**
	 * 요청 헤더의 Access Token → JWT 필터 → SecurityContext 순서로 전달된 회원 ID를 사용합니다.
	 * 인증 정보가 없으면 서비스 조회 전에 401 응답으로 처리합니다.
	 */
	@GetMapping
	public MyPageResponse getMyPage() {
		return myPageService.getMyPage(currentMemberId());
	}

	/**
	 * 클라이언트가 수정할 프로필을 전달하면 현재 Access Token의 회원 정보만 변경합니다.
	 * 요청 본문에 회원 ID를 받지 않으므로 다른 회원의 계정 정보를 수정할 수 없습니다.
	 */
	@PatchMapping
	public MyPageResponse updateMyPage(@Valid @RequestBody UpdateMyPageRequest request) {
		return myPageService.updateMyPage(currentMemberId(), request);
	}

	/**
	 * 비밀번호 변경은 단순 프로필 변경보다 민감하므로 현재 비밀번호를 함께 받아 재확인합니다.
	 * 성공 시 비밀번호 값을 포함하지 않고 204 응답만 반환합니다.
	 */
	@PatchMapping("/password")
	public ResponseEntity<Void> changePassword(
		@Valid @RequestBody ChangePasswordRequest request) {
		myPageService.changePassword(currentMemberId(), request);
		return ResponseEntity.noContent().build();
	}

	/** JWT 필터의 인증 정보에서 현재 회원 ID를 읽고, 형식이 맞지 않으면 인증 오류로 처리합니다. */
	private Long currentMemberId() {
		Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
		if (authentication == null || !(authentication.getPrincipal() instanceof Number memberId)) {
			throw new InvalidCredentialsException();
		}

		return memberId.longValue();
	}           
}
```

## 3. import·어노테이션·필드·메서드·키워드 설명

- `package`는 이 클래스의 네임스페이스를 정합니다. 같은 패키지의 `MyPageService`는 별도 import 없이 사용할 수 있습니다.
- Spring Security의 `Authentication`, `SecurityContextHolder`는 현재 요청에 연결된 로그인 정보를 가져옵니다. `ResponseEntity`는 HTTP 응답 상태/헤더/본문을 표현합니다.
- `GetMapping`, `PatchMapping`, `RequestBody`, `RequestMapping`, `RestController`는 URL과 HTTP 요청을 Java 메서드에 연결하는 Spring 어노테이션입니다.
- 세 DTO는 각각 비밀번호 변경 입력, 프로필 응답, 프로필 수정 입력을 나타냅니다. `InvalidCredentialsException`은 인증 정보가 없거나 예상한 형태가 아닐 때 던집니다.
- `@Valid`는 요청 DTO의 Jakarta Bean Validation 제약을 검사합니다. 예를 들어 입력값이 DTO의 `@NotBlank` 규칙에 맞지 않으면 메서드 로직에 들어가기 전에 검증 오류가 발생합니다.
- `@RestController`는 반환값을 뷰가 아니라 HTTP 응답 본문으로 직렬화하도록 합니다. `@RequestMapping("/api/mypage")`는 세 메서드의 공통 경로를 설정합니다.
- `private final MyPageService myPageService`는 생성 후 바뀌지 않는 서비스 의존성입니다. 생성자 주입을 통해 Spring이 서비스를 전달합니다. `this`는 현재 객체의 필드를 뜻합니다.
- `@GetMapping`은 GET `/api/mypage`, `@PatchMapping`은 PATCH `/api/mypage`, `@PatchMapping("/password")`는 PATCH `/api/mypage/password`에 해당합니다.
- `@RequestBody`는 JSON 요청 본문을 Java DTO로 변환합니다. `ResponseEntity<Void>`에서 `Void`는 응답 본문이 없음을 뜻합니다.
- `currentMemberId()`에서 `SecurityContextHolder`가 반환한 인증의 `principal`이 `Number`인지 확인합니다. `instanceof Number memberId`는 타입 검사와 변수 선언을 함께 하는 Java 패턴 매칭 문법입니다. `longValue()`로 `Long` 값을 얻습니다.
- `return`은 호출자에게 결과를 돌려주고 메서드를 끝냅니다. `throw`는 정상 반환 대신 예외를 발생시켜 전역 예외 처리 경계로 흐름을 넘깁니다.

## 4. 각 메서드의 목적·입력·순서·호출·결과·예외

### 생성자 `MyPageController(MyPageService myPageService)`

Spring이 만든 서비스 객체를 입력으로 받아 필드에 보관합니다. 다른 처리 메서드에서 재사용하며 별도 반환값은 없습니다.

### `getMyPage()`

입력은 없습니다. `currentMemberId()`로 인증된 회원 ID를 얻은 뒤 `myPageService.getMyPage(id)`를 호출하고 `MyPageResponse`를 반환합니다. 인증 주체가 없거나 숫자가 아니면 `InvalidCredentialsException`이 발생합니다. 회원 레코드가 없을 때의 예외는 서비스에서 발생합니다.

### `updateMyPage(UpdateMyPageRequest request)`

JSON 본문을 검증한 요청 DTO가 입력입니다. 본문 검증 후 현재 회원 ID를 읽고 서비스의 프로필 수정 메서드를 호출합니다. 성공하면 갱신 프로필 응답을 반환합니다. 입력 검증 실패, 인증 실패, 회원 없음, 이메일/전화번호 중복은 각각 검증 계층 또는 서비스의 예외 처리로 이어질 수 있습니다.

### `changePassword(ChangePasswordRequest request)`

현재 비밀번호와 새 비밀번호가 든 DTO를 입력받습니다. DTO 검증 후 회원 ID를 얻고 서비스를 호출합니다. 성공 시 본문 없는 HTTP 204 응답을 만듭니다. 현재 비밀번호가 맞지 않으면 서비스가 `InvalidCurrentPasswordException`을 던지고, 공통 예외 처리기가 HTTP 400으로 매핑합니다.

### `currentMemberId()`

입력은 없고 현재 SecurityContext의 principal을 확인합니다. 인증이 없거나 principal이 `Number`가 아니면 `InvalidCredentialsException`을 던집니다. 숫자형 principal을 `long`으로 변환해 반환합니다. 실제 JWT 필터는 회원 ID를 principal로 설정합니다.

## 5. 실제 실행 흐름

1. 클라이언트가 `Authorization: Bearer <Access Token>` 헤더와 함께 API를 호출합니다.
2. [`JwtAuthenticationFilter`](../../src/main/java/com/cleaner/be/auth/JwtAuthenticationFilter.java)가 토큰을 검사하고 회원 상태를 확인한 뒤 인증 principal에 회원 ID를 넣습니다. 유효하지 않은 토큰이나 탈퇴 회원은 Controller에 도달하기 전에 거부됩니다.
3. Spring MVC가 URL·HTTP 메서드에 맞는 Controller 메서드를 고릅니다. 요청 본문이 있으면 DTO로 변환하고 `@Valid` 검증을 실행합니다.
4. Controller가 `currentMemberId()`로 인증 정보를 꺼내고 [`MyPageService`](../../src/main/java/com/cleaner/be/mypage/MyPageService.java)에 회원 ID 및 요청 DTO를 전달합니다.
5. 서비스가 저장된 회원을 조회해 처리하고, 프로필 응답 DTO를 반환합니다. 비밀번호 변경은 성공 후 Controller가 204 응답으로 마무리합니다.
6. 서비스 예외는 [`GlobalExceptionHandler`](../../src/main/java/com/cleaner/be/auth/GlobalExceptionHandler.java)가 HTTP 상태와 메시지로 변환합니다.

## 6. 확인된 관계와 추론을 구분하기

**소스에서 확인됨:** 이 Controller는 서비스 생성자 주입을 사용합니다. `/api/mypage` 경로 아래 GET·PATCH·비밀번호 PATCH를 정의합니다. 모든 작업의 회원 ID는 `currentMemberId()`에서 오며 요청 DTO에는 회원 ID가 없습니다. 인증 principal은 숫자여야 합니다. [`JwtAuthenticationFilter`](../../src/main/java/com/cleaner/be/auth/JwtAuthenticationFilter.java)는 `memberId`를 `UsernamePasswordAuthenticationToken`의 principal로 설정합니다.

**합리적인 해석/추론:** 일반적인 브라우저 화면에서 조회 API는 페이지 진입 시, 프로필 PATCH는 저장 버튼, 비밀번호 PATCH는 비밀번호 변경 버튼을 눌렀을 때 호출됩니다. Controller 코드만으로 프런트엔드가 실제 어느 시점에 호출하는지까지 단정할 수는 없습니다. 토큰이 없는 요청에서 이 메서드가 불리는지 여부는 Security 설정에 달려 있지만, 이 메서드가 호출되더라도 `currentMemberId()`는 인증 실패를 던집니다.

## 7. 사용된 개념

- **Controller/Service 분리:** HTTP 라우팅·입력·응답과 비즈니스 처리를 분리합니다.
- **의존성 주입:** Spring이 관리하는 서비스를 생성자에서 전달합니다.
- **DTO:** 요청/응답의 데이터 모양을 엔티티와 분리합니다.
- **Bean Validation:** 요청값 규칙을 선언적으로 검사합니다.
- **인증 principal:** 현재 요청의 사용자를 식별하는 값입니다. 여기서는 숫자형 회원 ID입니다.
- **HTTP 상태:** 성공한 비밀번호 변경에 본문이 없는 `204 No Content`를 씁니다.
- **예외 경계:** 도메인 예외를 전역 Advice가 HTTP 응답으로 변환합니다.

## 8. 동작에 맞는 요청 예시

프로필 조회:

```http
GET /api/mypage
Authorization: Bearer eyJ...유효한-Access-Token
```

프로필 수정:

```http
PATCH /api/mypage
Authorization: Bearer eyJ...유효한-Access-Token
Content-Type: application/json

{"email":"new@example.com","name":"김하늘","phoneNumber":"010-1234-5678"}
```

이 본문에는 회원 ID가 없습니다. 서버는 토큰에서 찾은 현재 회원만 수정합니다. 비밀번호 변경은 다음처럼 요청하며 성공 응답 본문은 없습니다.

```http
PATCH /api/mypage/password
Authorization: Bearer eyJ...유효한-Access-Token
Content-Type: application/json

{"currentPassword":"Old!pass123","newPassword":"New!pass123"}
```

## 9. 요약

이 Controller는 마이페이지 API의 입구입니다. 인증된 principal에서 회원 ID를 얻고 요청 DTO를 검증한 뒤 서비스를 호출합니다. 조회·프로필 변경은 결과 DTO를 반환하고 비밀번호 변경은 성공 시 204를 반환합니다. 인증 오류와 서비스 예외의 세부 처리는 각각 인증 흐름 및 공통 예외 처리기에 연결됩니다.
