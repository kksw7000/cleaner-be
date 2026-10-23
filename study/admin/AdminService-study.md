# AdminService 학습 문서

## 1. 파일 역할·계층·필요한 이유·사용 시점

[`AdminService.java`](../../src/main/java/com/cleaner/be/admin/AdminService.java)는 관리자 Controller와 기존 도메인 서비스 사이에서 요청을 조율하는 서비스 계층입니다. 게시글·댓글 작업은 [`BoardService`](../../src/main/java/com/cleaner/be/board/BoardService.java), 회원 조회·정지 변경은 [`AuthService`](../../src/main/java/com/cleaner/be/auth/AuthService.java)에 맡겨 도메인 규칙을 재사용합니다. Controller의 요청 처리가 완료된 뒤 실제 애플리케이션 작업이 필요할 때 사용합니다.

## 2. 전체 원본 코드

아래는 현재 `AdminService.java`의 전체 내용입니다.

```java
package com.cleaner.be.admin;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.cleaner.be.admin.dto.AdminMemberPageResponse;
import com.cleaner.be.admin.dto.AdminMemberResponse;
import com.cleaner.be.auth.AuthService;
import com.cleaner.be.auth.Member;
import com.cleaner.be.board.BoardService;
import com.cleaner.be.board.dto.BoardPageResponse;

/**
 * 관리자 API의 요청을 조율합니다.
 * 게시글·댓글·회원 상태의 실제 규칙은 각 도메인 서비스에 위임해 관리자 전용 중복 구현을 만들지 않습니다.
 */
@Service
public class AdminService {

	private final BoardService boardService;
	private final AuthService authService;

	public AdminService(BoardService boardService, AuthService authService) {
		this.boardService = boardService;
		this.authService = authService;
	}

	/** 검색 조건과 페이지 정보를 게시글 도메인 규칙에 맡겨 관리자 게시글 목록으로 반환합니다. */
	@Transactional(readOnly = true)
	public BoardPageResponse getBoards(int page, int size, String keyword) {
		return boardService.getBoards(page, size, keyword);
	}

	/** 관리자 권한 검증과 공지 상태 변경을 게시글 서비스에 맡겨 도메인 정책을 일관되게 적용합니다. */
	@Transactional
	public void updateNotice(Long boardId, Long adminId, boolean notice) {
		boardService.updateNoticeByAdmin(boardId, adminId, notice);
	}

	/** 게시글 삭제 가능 여부를 게시글 서비스에서 확인하고 해당 트랜잭션 안에서 삭제합니다. */
	@Transactional
	public void deleteBoard(Long boardId, Long adminId) {
		boardService.deleteBoard(boardId, adminId);
	}

	/** 관리자 댓글 삭제의 권한과 도메인 처리를 게시글 서비스에 위임합니다. */
	@Transactional
	public void deleteComment(Long commentId, Long adminId) {
		boardService.deleteCommentByAdmin(commentId, adminId);
	}

	/** 회원 ID 내림차순 페이지를 조회하고 관리자 응답 형식으로 변환해 읽기 전용으로 반환합니다. */
	@Transactional(readOnly = true)
	public AdminMemberPageResponse getMembers(int page, int size) {
		Page<Member> members = authService.getMembers(PageRequest.of(page, size, Sort.by(Sort.Order.desc("id"))));
		return new AdminMemberPageResponse(members.map(this::toMemberResponse).getContent(), members.getNumber(), members.getSize(),
			members.getTotalElements(), members.getTotalPages(), members.isFirst(), members.isLast());
	}

	/** 회원 정지 상태 변경을 인증 서비스의 정책에 맡기고 결과를 관리자 응답으로 변환합니다. */
	@Transactional
	public AdminMemberResponse updateMemberSuspension(Long memberId, boolean suspended) {
		return toMemberResponse(authService.updateMemberSuspension(memberId, suspended));
	}

	/** 회원 엔티티의 관리자 조회 정보를 응답 DTO로 옮겨 내부 엔티티 노출을 막습니다. */
	private AdminMemberResponse toMemberResponse(Member member) {
		return new AdminMemberResponse(member.getId(), member.getEmail(), member.getName(), member.getPhoneNumber(),
			member.getRole(), member.isSuspended());
	}
}
```

## 3. 줄 단위 구성과 Java/Spring 요소

- 1행은 패키지, 3~15행은 Spring Data 페이지·정렬, Spring 서비스/트랜잭션, 관리자 DTO와 도메인 의존성 import입니다.
- 17~20행의 클래스 문서 주석은 관리자 전용 중복 규칙을 두기보다 도메인 서비스에 위임하는 의도를 설명합니다.
- 21행 `@Service`는 Spring이 이 클래스를 서비스 Bean으로 등록하게 합니다.
- 23~28행은 Board/Auth 서비스의 생성자 주입입니다. 각각의 참조는 `private final`로 선언합니다.
- 30~33행은 게시글 목록 조회입니다. `@Transactional(readOnly = true)`는 조회 전용 트랜잭션 의도를 표시합니다.
- 35~39행은 공지 상태 변경, 41~45행은 게시글 삭제, 47~51행은 관리자 댓글 삭제를 BoardService에 위임합니다. 각 작업은 `@Transactional`로 표시됩니다.
- 53~59행은 회원 페이지 조회입니다. `PageRequest.of`에 0부터 시작하는 페이지, 크기, `id` 내림차순 정렬을 넣어 AuthService에 전달하고 결과를 응답 DTO로 만듭니다.
- 61~65행은 회원 정지 상태를 AuthService에 전달하고 반환된 `Member`를 관리자 응답으로 변환합니다.
- 67~71행의 `toMemberResponse`는 Member의 선택된 필드만 새 응답 DTO에 복사합니다. `this::toMemberResponse`는 현재 객체의 변환 메서드를 페이지 각 요소에 적용하는 메서드 참조입니다.
- `Page<T>`는 결과 요소와 페이지 메타데이터를 함께 가진 Spring Data 타입입니다. 이 파일은 이를 외부 응답으로 직접 노출하지 않고 자체 DTO로 옮깁니다.

## 4. 메서드별 목적·입력·처리 순서·호출·반환·예외

- 생성자(26~29행): BoardService와 AuthService를 주입받아 필드에 저장합니다.
- `getBoards`(32~34행): 페이지·크기·검색어를 BoardService에 그대로 전달하고 `BoardPageResponse`를 반환합니다. 검색/정렬은 BoardService가 결정합니다.
- `updateNotice`(37~40행): 게시글 ID, 관리자 ID, 공지 여부를 BoardService의 `updateNoticeByAdmin`에 넘깁니다. 결과는 void이며 게시글 없음/관리자 권한 실패 등의 예외는 위임된 서비스가 결정합니다.
- `deleteBoard`(44~47행): 대상 게시글과 관리자 ID를 BoardService에 전달합니다. 메서드 자체는 반환하지 않고 삭제/권한 예외 처리를 위임합니다.
- `deleteComment`(50~53행): 댓글 ID와 관리자 ID를 BoardService에 전달해 관리자 댓글 삭제를 요청합니다.
- `getMembers`(57~61행): 페이지 요청을 `id` 내림차순으로 생성하고 회원 페이지를 조회합니다. 각 Member를 `AdminMemberResponse`로 변환한 뒤 content와 현재/전체 페이지 정보를 `AdminMemberPageResponse`로 반환합니다.
- `updateMemberSuspension`(65~67행): 회원 ID와 정지 여부를 AuthService에 넘기고, 반환된 Member를 관리자 응답으로 바꾸어 반환합니다. 미존재 회원의 예외는 AuthService에서 결정됩니다.
- `toMemberResponse`(70~73행): ID, 이메일, 이름, 전화번호, 역할, 정지 여부만 DTO로 옮깁니다. 비밀번호 같은 인증 내부 정보는 인수/응답에 포함하지 않습니다.

## 5. 실제 실행 흐름

1. Controller가 이 서비스 메서드를 호출합니다.
2. 메서드에 표시된 트랜잭션 설정이 작업 범위를 지정합니다. 읽기 메서드는 읽기 전용이고 변경 메서드는 일반 트랜잭션입니다.
3. 게시글·댓글 요청은 BoardService, 회원 작업은 AuthService에 전달됩니다.
4. 회원 목록은 `Page<Member>`를 받아 각 요소를 응답 DTO로 변환하고 페이지 요약 정보를 복사합니다. 정지 변경도 변경 결과를 DTO로 변환합니다.
5. 예외는 여기서 재정의하지 않고 도메인 서비스로부터 전파됩니다.

## 6. 확인된 관계와 추론

- **확인됨:** AdminController가 이 서비스를 사용하고, 이 서비스가 BoardService/AuthService를 생성자 주입받습니다.
- **확인됨:** BoardService는 관리자 공지 변경·댓글 삭제에서 관리자 역할을 확인하고, 게시글 삭제는 작성자 또는 관리자의 삭제 권한을 검사합니다.
- **확인됨:** AuthService의 회원 조회는 페이지 기반이고, 정지 변경은 회원을 찾아 `updateSuspension`을 호출합니다. 존재하지 않는 회원은 `InvalidCredentialsException`으로 처리합니다.
- **확인됨:** 관리자 응답 변환에는 `Member`의 ID/이메일/이름/전화번호/역할/정지 상태만 포함됩니다.
- **추론:** Spring 프록시가 `@Transactional`을 적용하는 방식은 일반적인 Spring 동작에 따른 설명입니다. 프록시의 실제 런타임 적용 상태를 이 파일만으로 실행 검증한 것은 아닙니다.

## 7. 코드에서 실제 쓰이는 핵심 개념

- **서비스 계층 위임:** 도메인마다 이미 가진 규칙을 재사용하여 관리자 경로만 별도 복제하지 않습니다.
- **트랜잭션:** 변경 작업들을 하나의 DB 작업 단위로 묶고, 조회에는 `readOnly=true` 의도를 표시합니다.
- **페이지네이션 및 정렬:** 전체 회원을 한 번에 전달하지 않고 페이지 단위로 제한하며 ID 내림차순을 요청합니다.
- **DTO 변환:** 내부 `Member`를 API에 직접 반환하지 않고 안전하게 선택한 필드로 새 DTO를 만듭니다.
- **람다 대체 문법인 메서드 참조:** `members.map(this::toMemberResponse)`는 각 회원에 변환 메서드를 적용합니다.

## 8. 동작에 충실한 예시

`GET /admin/members?page=0&size=10` 요청이 들어오면 Controller가 `getMembers(0, 10)`을 호출합니다. 이 메서드는 ID 내림차순 `PageRequest`를 만들고 AuthService에 전달합니다. 조회한 각 `Member`에서 여섯 필드만 복사해 content를 구성하고, 총 요소 수와 페이지 수 등의 값을 포함한 `AdminMemberPageResponse`를 반환합니다.

예를 들어 첫 페이지에 10명이 있다면 응답 DTO의 `content`는 최대 10개의 `AdminMemberResponse`를 담습니다. 실제 회원 수와 값은 저장 데이터에 따라 달라지므로 샘플 회원을 사실처럼 가정하지 않습니다.

## 9. 요약

AdminService는 관리자 Controller와 도메인 서비스의 조정자입니다. 회원 응답은 내부 엔티티 대신 DTO로 변환하고, 목록은 페이지 단위로 조회합니다. 핵심 게시글/회원 규칙은 각 도메인 서비스가 유지합니다.
