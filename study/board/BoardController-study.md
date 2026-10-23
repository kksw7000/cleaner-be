# BoardController.java 학습 노트

## 1. 파일의 역할·계층·존재 이유·사용 시점
- **계층:** 웹 컨트롤러
- **역할:** 게시글/댓글 HTTP 진입점입니다.
- **왜 필요한가:** HTTP 바인딩·검증·상태코드를 서비스와 연결합니다.
- **언제 사용하는가:** Spring MVC가 /api/boards 요청을 매핑할 때
- **원본:** [BoardController.java](../../src/main/java/com/cleaner/be/board/BoardController.java) (문서와 같은 디렉터리).

## 2. 현재 소스 전체
아래는 현재 Java 원본 전체이며 생략한 코드는 없습니다.

````java
package com.cleaner.be.board;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.cleaner.be.auth.InvalidCredentialsException;
import com.cleaner.be.board.dto.BoardCreateRequest;
import com.cleaner.be.board.dto.BoardPageResponse;
import com.cleaner.be.board.dto.BoardResponse;
import com.cleaner.be.board.dto.BoardUpdateRequest;
import com.cleaner.be.board.dto.CommentCreateRequest;
import com.cleaner.be.board.dto.CommentResponse;
import com.cleaner.be.board.dto.CommentUpdateRequest;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;

/** 자유 게시판 HTTP 진입점입니다. 로그인 정보는 요청 본문 대신 JWT 필터가 넣은 SecurityContext에서만 읽습니다. */
@RestController
@Validated
@RequestMapping("/api/boards")
public class BoardController {

	private final BoardService boardService;

	public BoardController(BoardService boardService) {
		this.boardService = boardService;
	}

	/** 검증된 요청 값을 서비스에 전달해 로그인 회원의 게시글을 생성합니다. */
	@PostMapping
	@ResponseStatus(HttpStatus.CREATED)
	public BoardResponse createBoard(@Valid @RequestBody BoardCreateRequest request) {
		return boardService.createBoard(currentMemberId(), request);
	}

	/** 페이지와 선택적 검색어로 게시글 목록을 조회합니다. */
	@GetMapping
	public BoardPageResponse getBoards(
		@RequestParam(defaultValue = "0") @Min(0) int page,
		@RequestParam(defaultValue = "10") @Min(1) @Max(100) int size,
		@RequestParam(required = false) String keyword) {
		return boardService.getBoards(page, size, keyword);
	}

	/** 게시글 상세 정보와 댓글을 조회합니다. */
	@GetMapping("/{id}")
	public BoardResponse getBoard(@PathVariable Long id) {
		return boardService.getBoard(id);
	}

	/** 로그인 회원의 권한을 확인하며 게시글을 수정합니다. */
	@PatchMapping("/{id}")
	public BoardResponse updateBoard(@PathVariable Long id, @Valid @RequestBody BoardUpdateRequest request) {
		return boardService.updateBoard(id, currentMemberId(), request);
	}

	/** 게시글을 삭제하고 성공 시 본문 없는 응답을 반환합니다. */
	@DeleteMapping("/{id}")
	public ResponseEntity<Void> deleteBoard(@PathVariable Long id) {
		boardService.deleteBoard(id, currentMemberId());
		return ResponseEntity.noContent().build();
	}

	/** 지정한 게시글에 로그인 회원의 댓글을 추가합니다. */
	@PostMapping("/{id}/comments")
	@ResponseStatus(HttpStatus.CREATED)
	public CommentResponse createComment(@PathVariable Long id, @Valid @RequestBody CommentCreateRequest request) {
		return boardService.createComment(id, currentMemberId(), request);
	}

	/** 댓글 작성 권한을 확인하며 댓글 내용을 수정합니다. */
	@PatchMapping("/{id}/comments/{commentId}")
	public CommentResponse updateComment(@PathVariable Long id, @PathVariable Long commentId,
		@Valid @RequestBody CommentUpdateRequest request) {
		return boardService.updateComment(id, commentId, currentMemberId(), request);
	}

	/** 지정한 게시글에 속한 댓글을 삭제합니다. */
	@DeleteMapping("/{id}/comments/{commentId}")
	public ResponseEntity<Void> deleteComment(@PathVariable Long id, @PathVariable Long commentId) {
		boardService.deleteComment(id, commentId, currentMemberId());
		return ResponseEntity.noContent().build();
	}

	/** 인증 필터가 SecurityContext에 저장한 회원 ID를 읽고, 없으면 인증 오류로 처리합니다. */
	private Long currentMemberId() {
		Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
		if (authentication == null || !(authentication.getPrincipal() instanceof Number memberId)) {
			throw new InvalidCredentialsException();
		}
		return memberId.longValue();
	}
}
````

## 3. import·애노테이션·필드·메서드·키워드 설명
### 이 파일에서 실제 사용한 애노테이션
- `@RestController`: REST 응답 컨트롤러로 등록합니다. (적용 소스: `@RestController`)
- `@Validated`: 메서드 파라미터 제약 검증을 활성화합니다. (적용 소스: `@Validated`)
- `@RequestMapping`: 공통 URL 경로를 지정합니다. (적용 소스: `@RequestMapping("/api/boards")`)
- `@PostMapping`: POST 경로를 연결합니다. (적용 소스: `@PostMapping`)
- `@ResponseStatus`: 성공 응답 HTTP 상태를 지정합니다. (적용 소스: `@ResponseStatus(HttpStatus.CREATED)`)
- `@Valid`: 요청 객체 필드 검증을 수행합니다. (적용 소스: `public BoardResponse createBoard(@Valid @RequestBody BoardCreateRequest request) {`)
- `@RequestBody`: 본문을 Java 객체로 바인딩합니다. (적용 소스: `public BoardResponse createBoard(@Valid @RequestBody BoardCreateRequest request) {`)
- `@GetMapping`: GET 경로를 연결합니다. (적용 소스: `@GetMapping`)
- `@RequestParam`: 쿼리 파라미터를 인자로 바인딩합니다. (적용 소스: `@RequestParam(defaultValue = "0") @Min(0) int page,`)
- `@Min`: 숫자 최솟값을 제한합니다. (적용 소스: `@RequestParam(defaultValue = "0") @Min(0) int page,`)
- `@RequestParam`: 쿼리 파라미터를 인자로 바인딩합니다. (적용 소스: `@RequestParam(defaultValue = "10") @Min(1) @Max(100) int size,`)
- `@Min`: 숫자 최솟값을 제한합니다. (적용 소스: `@RequestParam(defaultValue = "10") @Min(1) @Max(100) int size,`)
- `@Max`: 숫자 최댓값을 제한합니다. (적용 소스: `@RequestParam(defaultValue = "10") @Min(1) @Max(100) int size,`)
- `@RequestParam`: 쿼리 파라미터를 인자로 바인딩합니다. (적용 소스: `@RequestParam(required = false) String keyword) {`)
- `@GetMapping`: GET 경로를 연결합니다. (적용 소스: `@GetMapping("/{id}")`)
- `@PathVariable`: 경로 변수 값을 인자로 바인딩합니다. (적용 소스: `public BoardResponse getBoard(@PathVariable Long id) {`)
- `@PatchMapping`: PATCH 경로를 연결합니다. (적용 소스: `@PatchMapping("/{id}")`)
- `@PathVariable`: 경로 변수 값을 인자로 바인딩합니다. (적용 소스: `public BoardResponse updateBoard(@PathVariable Long id, @Valid @RequestBody BoardUpdateRequest request) {`)
- `@Valid`: 요청 객체 필드 검증을 수행합니다. (적용 소스: `public BoardResponse updateBoard(@PathVariable Long id, @Valid @RequestBody BoardUpdateRequest request) {`)
- `@RequestBody`: 본문을 Java 객체로 바인딩합니다. (적용 소스: `public BoardResponse updateBoard(@PathVariable Long id, @Valid @RequestBody BoardUpdateRequest request) {`)
- `@DeleteMapping`: DELETE 경로를 연결합니다. (적용 소스: `@DeleteMapping("/{id}")`)
- `@PathVariable`: 경로 변수 값을 인자로 바인딩합니다. (적용 소스: `public ResponseEntity<Void> deleteBoard(@PathVariable Long id) {`)
- `@PostMapping`: POST 경로를 연결합니다. (적용 소스: `@PostMapping("/{id}/comments")`)
- `@ResponseStatus`: 성공 응답 HTTP 상태를 지정합니다. (적용 소스: `@ResponseStatus(HttpStatus.CREATED)`)
- `@PathVariable`: 경로 변수 값을 인자로 바인딩합니다. (적용 소스: `public CommentResponse createComment(@PathVariable Long id, @Valid @RequestBody CommentCreateRequest request) {`)
- `@Valid`: 요청 객체 필드 검증을 수행합니다. (적용 소스: `public CommentResponse createComment(@PathVariable Long id, @Valid @RequestBody CommentCreateRequest request) {`)
- `@RequestBody`: 본문을 Java 객체로 바인딩합니다. (적용 소스: `public CommentResponse createComment(@PathVariable Long id, @Valid @RequestBody CommentCreateRequest request) {`)
- `@PatchMapping`: PATCH 경로를 연결합니다. (적용 소스: `@PatchMapping("/{id}/comments/{commentId}")`)
- `@PathVariable`: 경로 변수 값을 인자로 바인딩합니다. (적용 소스: `public CommentResponse updateComment(@PathVariable Long id, @PathVariable Long commentId,`)
- `@PathVariable`: 경로 변수 값을 인자로 바인딩합니다. (적용 소스: `public CommentResponse updateComment(@PathVariable Long id, @PathVariable Long commentId,`)
- `@Valid`: 요청 객체 필드 검증을 수행합니다. (적용 소스: `@Valid @RequestBody CommentUpdateRequest request) {`)
- `@RequestBody`: 본문을 Java 객체로 바인딩합니다. (적용 소스: `@Valid @RequestBody CommentUpdateRequest request) {`)
- `@DeleteMapping`: DELETE 경로를 연결합니다. (적용 소스: `@DeleteMapping("/{id}/comments/{commentId}")`)
- `@PathVariable`: 경로 변수 값을 인자로 바인딩합니다. (적용 소스: `public ResponseEntity<Void> deleteComment(@PathVariable Long id, @PathVariable Long commentId) {`)
- `@PathVariable`: 경로 변수 값을 인자로 바인딩합니다. (적용 소스: `public ResponseEntity<Void> deleteComment(@PathVariable Long id, @PathVariable Long commentId) {`)

### 이 파일에 나타난 Java 키워드
- `this`: 현재 객체를 가리킵니다.
- `throw`: 예외를 던져 정상 흐름을 중단합니다.
- `null`: 값이 없음을 나타내는 리터럴입니다.
- `return`: 메서드 결과를 반환하거나 실행을 끝냅니다.
- `class`: 클래스를 선언합니다.
- `instanceof`: 객체가 특정 타입인지 검사하며 패턴 변수도 선언할 수 있습니다.
- `if`: 조건이 참일 때 블록을 실행합니다.
- `private`: 현재 클래스 내부에서만 접근할 수 있습니다.
- `false`: 거짓을 나타내는 boolean 리터럴입니다.
- `new`: 새 객체를 생성합니다.
- `int`: 정수 기본 타입입니다.
- `package`: 타입이 속한 네임스페이스를 선언합니다.
- `public`: 외부에서 접근할 수 있습니다.
- `import`: 다른 패키지 타입을 짧은 이름으로 사용하게 합니다.
- `final`: 적용 대상에 따라 재할당/재정의를 제한합니다.

### import별 용도
- `org.springframework.http.HttpStatus`: Spring 프레임워크 타입입니다.
- `org.springframework.http.ResponseEntity`: Spring 프레임워크 타입입니다.
- `org.springframework.security.core.Authentication`: Spring 프레임워크 타입입니다.
- `org.springframework.security.core.context.SecurityContextHolder`: Spring 프레임워크 타입입니다.
- `org.springframework.validation.annotation.Validated`: Spring 프레임워크 타입입니다.
- `org.springframework.web.bind.annotation.DeleteMapping`: Spring 프레임워크 타입입니다.
- `org.springframework.web.bind.annotation.GetMapping`: Spring 프레임워크 타입입니다.
- `org.springframework.web.bind.annotation.PatchMapping`: Spring 프레임워크 타입입니다.
- `org.springframework.web.bind.annotation.PathVariable`: Spring 프레임워크 타입입니다.
- `org.springframework.web.bind.annotation.PostMapping`: Spring 프레임워크 타입입니다.
- `org.springframework.web.bind.annotation.RequestBody`: Spring 프레임워크 타입입니다.
- `org.springframework.web.bind.annotation.RequestMapping`: Spring 프레임워크 타입입니다.
- `org.springframework.web.bind.annotation.RequestParam`: Spring 프레임워크 타입입니다.
- `org.springframework.web.bind.annotation.ResponseStatus`: Spring 프레임워크 타입입니다.
- `org.springframework.web.bind.annotation.RestController`: Spring 프레임워크 타입입니다.
- `com.cleaner.be.auth.InvalidCredentialsException`: 인증 도메인 타입입니다.
- `com.cleaner.be.board.dto.BoardCreateRequest`: 게시판 DTO입니다.
- `com.cleaner.be.board.dto.BoardPageResponse`: 게시판 DTO입니다.
- `com.cleaner.be.board.dto.BoardResponse`: 게시판 DTO입니다.
- `com.cleaner.be.board.dto.BoardUpdateRequest`: 게시판 DTO입니다.
- `com.cleaner.be.board.dto.CommentCreateRequest`: 게시판 DTO입니다.
- `com.cleaner.be.board.dto.CommentResponse`: 게시판 DTO입니다.
- `com.cleaner.be.board.dto.CommentUpdateRequest`: 게시판 DTO입니다.
- `jakarta.validation.Valid`: Bean Validation 제약입니다.
- `jakarta.validation.constraints.Max`: Bean Validation 제약입니다.
- `jakarta.validation.constraints.Min`: Bean Validation 제약입니다.

### 필드와 컴포넌트
boardService는 생성자 주입 서비스입니다.

### 애노테이션과 Java 문법
`package`는 네임스페이스입니다. `public/private/protected`는 접근 범위를 제어합니다. 아래 소스에서 실제로 사용한 애노테이션은 해당 선언에 프레임워크 동작을 연결합니다. 소스에 없는 애노테이션 동작은 가정하지 않습니다. `record`는 컴포넌트 기반 타입이며 생성자·접근자 등의 일부 메서드를 자동 생성합니다. 저장소 `interface` 구현은 Spring Data가 제공합니다.

## 4. 메서드별 목적·매개변수·순서·호출·반환·예외
### 선언된 메서드/생성자 시그니처
매개변수 타입과 나열 순서는 아래 시그니처 그대로이며, 괄호 앞 타입은 반환형입니다. 생성자는 반환형이 없습니다. `void`는 값을 반환하지 않습니다.
- `public BoardController(BoardService boardService)`
- `public BoardResponse createBoard(@Valid @RequestBody BoardCreateRequest request)`
- `public BoardPageResponse getBoards( @RequestParam(defaultValue = "0") @Min(0) int page, @RequestParam(defaultValue = "10") @Min(1) @Max(100) int size, @RequestParam(required = false) String keyword)`
- `public BoardResponse getBoard(@PathVariable Long id)`
- `public BoardResponse updateBoard(@PathVariable Long id, @Valid @RequestBody BoardUpdateRequest request)`
- `public ResponseEntity<Void> deleteBoard(@PathVariable Long id)`
- `public CommentResponse createComment(@PathVariable Long id, @Valid @RequestBody CommentCreateRequest request)`
- `public CommentResponse updateComment(@PathVariable Long id, @PathVariable Long commentId, @Valid @RequestBody CommentUpdateRequest request)`
- `public ResponseEntity<Void> deleteComment(@PathVariable Long id, @PathVariable Long commentId)`
- `private Long currentMemberId()`

소스에서 발생시키거나 전달하는 예외 타입: `InvalidCredentialsException`. 발생 위치에서 정상 흐름이 중단되며, 전역 처리기 매핑은 관계 설명을 참고합니다.
생성자는 의존성을 저장. createBoard POST/201. getBoards GET, page 기본 0·최소 0, size 기본 10·1..100. getBoard 상세 GET. updateBoard PATCH. deleteBoard DELETE/204. createComment POST/201. updateComment PATCH. deleteComment DELETE/204. currentMemberId는 SecurityContext principal이 Number인지 확인, 아니면 InvalidCredentialsException, 맞으면 Long 반환.

직접 선언 메서드와 record 자동 생성/저장소 상속 메서드를 구분했습니다. 예외가 발생하면 이후 정상 단계는 수행되지 않습니다. 프레임워크의 바인딩·DB·검증 오류는 도메인 예외와 별도로 전파될 수 있습니다.

## 5. 확인된 end-to-end 실행 흐름
경로/쿼리/본문 바인딩→@Valid/@Validated→로그인 API는 currentMemberId→Service→응답 직렬화. 업무 권한/조회수는 서비스 처리.

## 6. 확인된 클래스 관계와 추론
확인됨: BoardService·DTO·InvalidCredentialsException 사용, 전역 처리기는 인증 오류 401. principal을 Number로 넣는 필터 구현은 여기서 확인되지 않습니다.

## 7. 이 파일에서 익힐 개념
Spring MVC, Bean Validation, ResponseEntity, SecurityContext, instanceof 패턴 변수.

## 8. 소스와 일치하는 예시
게시글 생성의 작성자는 JSON에서 받지 않고 인증 정보에서 결정합니다.

## 9. 핵심 요약
HTTP 어댑터이며 업무 정책을 서비스에 위임합니다.



### 관련 Java 원본 바로가기
- [`Board.java`](../../src/main/java/com/cleaner/be/board/Board.java)
- [`Comment.java`](../../src/main/java/com/cleaner/be/board/Comment.java)
- [`BoardController.java`](../../src/main/java/com/cleaner/be/board/BoardController.java)
- [`BoardService.java`](../../src/main/java/com/cleaner/be/board/BoardService.java)
- [`BoardRepository.java`](../../src/main/java/com/cleaner/be/board/BoardRepository.java)
- [`CommentRepository.java`](../../src/main/java/com/cleaner/be/board/CommentRepository.java)
- [`dto/BoardCreateRequest.java`](../../src/main/java/com/cleaner/be/board/dto/BoardCreateRequest.java)
- [`dto/BoardResponse.java`](../../src/main/java/com/cleaner/be/board/dto/BoardResponse.java)
- [`dto/CommentResponse.java`](../../src/main/java/com/cleaner/be/board/dto/CommentResponse.java)

## 관련 상대 링크
- Java 원본: [BoardController.java](../../src/main/java/com/cleaner/be/board/BoardController.java)
- 게시판 기준: `cleaner-be/src/main/java/com/cleaner/be/board/`
- DTO 기준: `cleaner-be/src/main/java/com/cleaner/be/board/dto/`
- 관련 타입: `cleaner-be/src/main/java/com/cleaner/be/auth/Member.java`, `cleaner-be/src/main/java/com/cleaner/be/common/entity/BaseTimeEntity.java