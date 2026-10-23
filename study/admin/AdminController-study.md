# AdminController 학습 문서

## 1. 파일 역할·계층·필요한 이유·사용 시점

[`AdminController.java`](../../src/main/java/com/cleaner/be/admin/AdminController.java)는 관리자 기능의 HTTP 진입점인 Spring MVC Controller입니다. `/admin` 아래 게시글 목록·공지 변경·게시글/댓글 삭제·회원 목록·회원 정지 API를 선언하고, 실제 규칙은 [`AdminService.java`](../../src/main/java/com/cleaner/be/admin/AdminService.java)에 위임합니다. URL 권한은 [`auth/SecurityConfig.java`](../../src/main/java/com/cleaner/be/auth/SecurityConfig.java)가 Controller에 도달하기 전에 검사하며, 공지·삭제 작업에는 인증된 관리자 ID도 Service로 전달합니다. 웹 요청과 내부 도메인 처리를 분리하고 싶을 때 사용하는 계층입니다.

## 2. 전체 원본 코드

아래는 현재 `AdminController.java`의 전체 내용입니다.

```java
package com.cleaner.be.admin;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.cleaner.be.admin.dto.AdminMemberPageResponse;
import com.cleaner.be.admin.dto.AdminMemberResponse;
import com.cleaner.be.admin.dto.MemberSuspensionRequest;
import com.cleaner.be.admin.dto.NoticeUpdateRequest;
import com.cleaner.be.auth.InvalidCredentialsException;
import com.cleaner.be.board.dto.BoardPageResponse;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;

/** 관리자 전용 HTTP 진입점입니다. ADMIN 권한 검사는 SecurityConfig에서 선행하고 현재 ID만 서비스에 전달합니다. */
@RestController
@Validated
@RequestMapping("/admin")
public class AdminController {

	private final AdminService adminService;

	public AdminController(AdminService adminService) {
		this.adminService = adminService;
	}

	/** 페이지와 선택적 검색어를 받아 관리자 게시글 목록을 조회하도록 AdminService에 전달합니다. */
	@GetMapping("/boards")
	public BoardPageResponse getBoards(@RequestParam(defaultValue = "0") @Min(0) int page,
		@RequestParam(defaultValue = "10") @Min(1) @Max(100) int size,
		@RequestParam(required = false) String keyword) {
		return adminService.getBoards(page, size, keyword);
	}

	/** 관리자 요청의 공지 상태를 변경하고, 완료 후 응답 본문 없이 반환합니다. */
	@PatchMapping("/boards/{id}/notice")
	public ResponseEntity<Void> updateNotice(@PathVariable Long id, @Valid @RequestBody NoticeUpdateRequest request) {
		// 관리자 ID를 서비스에도 전달해 권한 검증과 공지 변경을 수행합니다.
		adminService.updateNotice(id, currentMemberId(), request.notice());
		return ResponseEntity.noContent().build();
	}

	/** 게시글을 관리자 권한으로 삭제하고, 성공 시 응답 본문 없이 반환합니다. */
	@DeleteMapping("/boards/{id}")
	public ResponseEntity<Void> deleteBoard(@PathVariable Long id) {
		// 서비스에서 관리자 권한과 게시글 삭제를 함께 처리하도록 인증 ID를 전달합니다.
		adminService.deleteBoard(id, currentMemberId());
		return ResponseEntity.noContent().build();
	}

	/** 댓글 ID로 대상을 지정해 관리자 권한으로 삭제하고, 성공 시 응답 본문 없이 반환합니다. */
	@DeleteMapping("/comments/{commentId}")
	public ResponseEntity<Void> deleteComment(@PathVariable Long commentId) {
		// 서비스에서 관리자 권한과 댓글 삭제를 함께 처리하도록 인증 ID를 전달합니다.
		adminService.deleteComment(commentId, currentMemberId());
		return ResponseEntity.noContent().build();
	}

	/** 페이지 정보를 받아 회원 관리 화면에 필요한 회원 목록을 조회합니다. */
	@GetMapping("/members")
	public AdminMemberPageResponse getMembers(@RequestParam(defaultValue = "0") @Min(0) int page,
		@RequestParam(defaultValue = "10") @Min(1) @Max(100) int size) {
		return adminService.getMembers(page, size);
	}
 
	/** 요청의 정지 여부를 회원 계정에 반영하고 변경된 회원 정보를 반환합니다. */
	@PatchMapping("/members/{id}/suspension")
	public AdminMemberResponse updateMemberSuspension(@PathVariable Long id,
		@Valid @RequestBody MemberSuspensionRequest request) {
		// 계정 이용 상태 변경은 관리자 엔드포인트에서 검증된 요청만 서비스에 반영합니다.
		return adminService.updateMemberSuspension(id, request.suspended());
	}

	/** 인증 필터가 저장한 회원 ID를 반환하고, 유효한 숫자 ID가 없으면 인증 오류로 처리합니다. */
	private Long currentMemberId() {
		// 보안 설정을 통과한 요청의 인증 정보를 서비스 권한 검증에 사용할 수 있도록 읽습니다.
		Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
		if (authentication == null || !(authentication.getPrincipal() instanceof Number memberId)) {
			// 회원 ID로 사용할 수 없는 principal은 서비스에 전달하지 않고 인증 오류로 거부합니다.
			throw new InvalidCredentialsException();
		}
		return memberId.longValue();
	}
}
```

## 3. 줄 단위 구성과 Java/Spring 요소

- 1행은 `com.cleaner.be.admin` 패키지 소속을 선언하고, 3~21행은 HTTP 응답·인증 정보·Spring 요청 매핑·요청 검증·응답 DTO를 가져오는 import입니다. 사용하지 않는 import는 없습니다.
- 23~25행의 `@RestController`는 메서드 반환값을 HTTP 응답 데이터로 취급하고, `@Validated`는 메서드 매개변수 제약 조건 검사를 활성화하며, `@RequestMapping("/admin")`은 모든 메서드 경로의 공통 앞부분을 지정합니다.
- 26~32행은 클래스와 생성자입니다. `private final` 필드는 생성 뒤 재할당되지 않는 Service 의존성이며, 생성자 매개변수로 받아 Spring이 연결합니다.
- 34~40행은 `GET /admin/boards`입니다. `page`는 0 이상, `size`는 1~100으로 제한되고, `keyword`는 선택 입력입니다. 기본값은 각각 0과 10입니다.
- 42~48행은 `PATCH /admin/boards/{id}/notice`입니다. 경로의 `{id}`를 `Long`으로 받고, JSON 본문을 `NoticeUpdateRequest`로 변환·검증합니다. 현재 ID와 요청의 `notice` 값을 Service로 넘긴 뒤 204 응답을 만듭니다.
- 50~56행은 `DELETE /admin/boards/{id}`, 58~64행은 `DELETE /admin/comments/{commentId}`입니다. 각각 작업 성공 후 `ResponseEntity<Void>`로 본문 없는 204를 반환합니다.
- 66~72행은 `GET /admin/members`입니다. 목록의 페이지 정보를 검증하고 Service 응답 DTO를 그대로 반환합니다.
- 74~81행은 `PATCH /admin/members/{id}/suspension`입니다. `@Valid`로 요청 객체를 검증하고 boolean 상태를 Service에 전달합니다.
- 83~92행은 인증 ID를 얻는 private 메서드입니다. `SecurityContextHolder`에서 현재 `Authentication`을 읽고, principal이 `Number`가 아니거나 인증 객체가 없으면 `InvalidCredentialsException`을 던집니다. `instanceof Number memberId`는 타입 확인과 변수 선언을 한 번에 하는 패턴 매칭 문법입니다.
- `@PathVariable`, `@RequestParam`, `@RequestBody`는 각각 URL 경로, 쿼리 문자열, HTTP 본문을 Java 매개변수에 연결합니다. `@Valid`와 `@Min`/`@Max`는 잘못된 입력을 호출 단계에서 걸러냅니다.

## 4. 메서드별 목적·입력·처리 순서·호출·반환·예외

- 생성자(29~31행): Spring이 제공한 `AdminService`를 필드에 저장합니다. 별도의 반환값이나 업무 예외를 선언하지 않습니다.
- `getBoards`(35~40행): `page`, `size`, 선택 검색어를 받아 `adminService.getBoards`에 넘기고 `BoardPageResponse`를 반환합니다. 범위 위반은 `@Validated`/Bean Validation 처리에서 거부됩니다.
- `updateNotice`(44~48행): 게시글 ID, 검증된 공지 변경 요청을 받습니다. `currentMemberId()`로 인증 ID를 읽고 Service에 전달한 뒤 `ResponseEntity.noContent().build()`로 204를 반환합니다. 인증 ID가 유효하지 않으면 `InvalidCredentialsException`이 발생합니다.
- `deleteBoard`(57~61행), `deleteComment`(65~69행): 대상 ID와 현재 관리자 ID를 Service로 전달하고 성공 시 204를 반환합니다. 인증 ID 오류 및 도메인 서비스의 삭제/권한 예외가 발생할 수 있습니다.
- `getMembers`(73~76행): 페이지와 크기를 Service로 전달해 `AdminMemberPageResponse`를 반환합니다.
- `updateMemberSuspension`(80~84행): ID와 검증된 `MemberSuspensionRequest`를 받아 `request.suspended()`를 Service에 전달합니다. 변경된 회원의 `AdminMemberResponse`를 반환합니다.
- `currentMemberId`(88~95행): 인증 객체의 principal에서 숫자 ID를 검사해 `Long`으로 반환합니다. 인증이 없거나 principal 타입이 숫자가 아니면 `InvalidCredentialsException`을 던집니다.

## 5. 실제 실행 흐름

1. 클라이언트가 `/admin/...` 요청을 보냅니다.
2. 확인된 구성에 따르면 `SecurityConfig`가 `/admin/**`에 `hasRole("ADMIN")`을 적용하고 JWT 필터를 먼저 실행합니다. 권한이 없으면 Controller까지 도달하지 않고 401 또는 403 처리됩니다.
3. Spring MVC가 URL의 경로/쿼리/본문을 메서드 인수로 바꾸고 선언된 검증을 수행합니다.
4. Controller가 필요한 경우 `currentMemberId()`로 인증 principal을 읽고, `AdminService`에 요청을 전달합니다.
5. Service가 Board/Auth 도메인 Service에 위임합니다. 각 서비스가 조회·도메인 권한 확인·상태 변경을 수행하고, Controller는 결과 DTO 또는 204만 응답합니다.

## 6. 확인된 관계와 추론

- **코드로 확인됨:** Controller는 `AdminService`를 생성자 주입받습니다. `AdminService`는 Board/Auth 도메인 서비스로 관리자 작업을 위임합니다. 두 컨트롤러의 관리자 인가 규칙은 `SecurityConfig`에 명시되어 있습니다.
- **코드로 확인됨:** JWT 인증 필터가 `SecurityContextHolder`에 인증 객체를 저장합니다. 이 Controller는 그 객체의 principal이 숫자일 때 회원 ID로 취급합니다.
- **코드로 확인됨:** 특정 DTO는 요청/응답의 데이터 구조이며, Controller는 게시글 페이지 응답을 Board 도메인의 DTO로 반환합니다.
- **추론:** 프론트엔드 관리자 화면이 이 경로를 호출해 데이터/작업을 표시할 것으로 보입니다. 다만 이 파일만으로 실제 호출 UI를 확인한 것은 아닙니다.
- 요청 처리 이외의 DB 트랜잭션 규칙은 이 Controller가 직접 정하지 않고 Service에 위임합니다.

## 7. 코드에서 실제 쓰이는 핵심 개념

- **REST Controller와 매핑:** HTTP 동사(GET/PATCH/DELETE)와 경로를 Java 메서드에 연결합니다.
- **의존성 주입:** Controller가 Service를 직접 생성하지 않고 생성자로 받습니다.
- **DTO:** API 입력과 출력을 표현하여 도메인 엔티티를 직접 HTTP에 노출하지 않습니다.
- **Bean Validation:** 페이지 범위 및 요청 본문을 검사합니다. 이 Controller에는 `@Valid`가 붙은 DTO 필드 제약은 보이지 않지만, 입력 객체 검증을 실행하도록 연결되어 있습니다.
- **인증 컨텍스트:** 요청별 SecurityContext의 principal을 읽습니다. 숫자 ID인지 확인해야 잘못된 형식의 principal을 ID로 오인하지 않습니다.
- **HTTP 204:** 작업은 성공했으나 응답 본문은 필요하지 않다는 뜻입니다.

## 8. 동작에 충실한 예시

페이지 첫 화면을 기본값으로 요청하는 예시는 `GET /admin/boards`입니다. Controller에서 기본값 `page=0`, `size=10`을 사용하고 `keyword`는 전달하지 않으면 null입니다. 이 값이 `AdminService.getBoards(0, 10, null)`로 전달됩니다. 실제 응답 필드 구성은 [`BoardPageResponse`](../../src/main/java/com/cleaner/be/board/dto/BoardPageResponse.java)에서 확인해야 합니다.

공지 상태 변경 요청은 `PATCH /admin/boards/42/notice`와 JSON `{"notice":true}` 형태입니다. 본문은 `NoticeUpdateRequest`로 바인딩되고, 인증 관리자 ID와 `true`가 서비스에 전달됩니다. 완료 시 응답 본문 없이 HTTP 204입니다. 이 예시는 요청 구조를 설명하며, DB에 42번 게시글이 존재한다고 보장하지 않습니다.

## 9. 요약

이 파일은 관리자 HTTP API의 경로·입력·검증·응답만 담당하는 Controller입니다. `/admin/**`의 역할 검사는 Security 설정이 선행하며, 도메인 권한과 실제 변경은 서비스 계층에 맡깁니다. 인증 ID가 필요한 작업은 SecurityContext의 숫자 principal을 확인한 후 서비스로 전달합니다.
