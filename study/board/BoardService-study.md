# BoardService.java 학습 노트

## 1. 파일의 역할·계층·존재 이유·사용 시점
- **계층:** 서비스
- **역할:** 게시글·댓글 업무 규칙, 권한, 트랜잭션 및 DTO 변환입니다.
- **왜 필요한가:** HTTP와 DB 사이에서 규칙을 일관되게 적용합니다.
- **언제 사용하는가:** Controller의 게시판 요청 및 관리자 작업 시
- **원본:** [BoardService.java](../../src/main/java/com/cleaner/be/board/BoardService.java) (문서와 같은 디렉터리).

## 2. 현재 소스 전체
아래는 현재 Java 원본 전체이며 생략한 코드는 없습니다.

````java
package com.cleaner.be.board;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.cleaner.be.auth.Member;
import com.cleaner.be.auth.MemberRepository;
import com.cleaner.be.auth.Role;
import com.cleaner.be.board.dto.BoardCreateRequest;
import com.cleaner.be.board.dto.BoardListResponse;
import com.cleaner.be.board.dto.BoardPageResponse;
import com.cleaner.be.board.dto.BoardResponse;
import com.cleaner.be.board.dto.BoardUpdateRequest;
import com.cleaner.be.board.dto.CommentCreateRequest;
import com.cleaner.be.board.dto.CommentResponse;
import com.cleaner.be.board.dto.CommentUpdateRequest;

/**
 * 게시글·댓글 요청의 도메인 규칙을 처리합니다.
 * Controller가 SecurityContext에서 얻은 회원 ID만 전달하면, 이 서비스가 작성자 확인·저장·DTO 변환을 순서대로 수행합니다.
 */
@Service
public class BoardService {

	private final BoardRepository boardRepository;
	private final CommentRepository commentRepository;
	private final MemberRepository memberRepository;

	public BoardService(BoardRepository boardRepository, CommentRepository commentRepository, MemberRepository memberRepository) {
		this.boardRepository = boardRepository;
		this.commentRepository = commentRepository;
		this.memberRepository = memberRepository;
	}

	/** 인증된 회원을 작성자로 연결해 게시글을 저장하고, 공지로 지정할 때는 관리자 권한을 확인합니다. */
	@Transactional
	public BoardResponse createBoard(Long memberId, BoardCreateRequest request) {
		// 회원 조회 → 공지 권한 확인 → 게시글 저장 및 응답 변환 순서로 처리합니다.
		Member author = findMember(memberId);
		validateNoticePermission(author, request.notice());
		Board board = boardRepository.save(new Board(request.title().trim(), request.content().trim(), request.notice(), author));
		return toBoardResponse(board, List.of());
	}

	/** 공지를 먼저 보여주고 최신순으로 정렬해 페이지를 반환하며, 키워드가 있으면 제목만 검색해 목록 범위를 제한합니다. */
	@Transactional(readOnly = true)
	public BoardPageResponse getBoards(int page, int size, String keyword) {
		// 정렬·페이지 조건 생성 → 키워드 정리 → 제목 검색 또는 전체 조회 → 페이지 응답 변환 순서로 처리합니다.
		PageRequest pageRequest = PageRequest.of(page, size, Sort.by(
			Sort.Order.desc("notice"), Sort.Order.desc("createdAt")));
		String normalizedKeyword = keyword == null ? "" : keyword.trim();
		Page<Board> boards = normalizedKeyword.isEmpty()
			? boardRepository.findAll(pageRequest)
			: boardRepository.findByTitleContainingIgnoreCase(normalizedKeyword, pageRequest);

		return new BoardPageResponse(
			boards.map(this::toBoardListResponse).getContent(), boards.getNumber(), boards.getSize(),
			boards.getTotalElements(), boards.getTotalPages(), boards.isFirst(), boards.isLast());
	}

	/** 게시글 상세 정보와 시간순 댓글을 함께 제공하고, 조회 요청마다 조회수를 반영해 열람 횟수를 기록합니다. */
	@Transactional
	public BoardResponse getBoard(Long boardId) {
		// 게시글 조회 → 조회수 증가 → 댓글 조회 및 응답 변환 순서로 처리합니다.
		Board board = findBoard(boardId);
		board.increaseViewCount();
		List<CommentResponse> comments = commentRepository.findByBoardIdOrderByCreatedAtAsc(boardId).stream()
			.map(this::toCommentResponse).toList();
		return toBoardResponse(board, comments);
	}

	/** 게시글 작성자만 내용을 수정할 수 있고, 공지로 지정하는 경우에는 관리자 권한도 확인해 일반 회원의 공지 등록을 막습니다. */
	@Transactional
	public BoardResponse updateBoard(Long boardId, Long memberId, BoardUpdateRequest request) {
		// 게시글·회원 조회 → 작성자와 공지 권한 확인 → 내용 수정 → 댓글을 포함한 응답 변환 순서로 처리합니다.
		Board board = findBoard(boardId);
		Member member = findMember(memberId);
		validateAuthor(board.getAuthor(), member, new BoardAccessDeniedException());
		validateNoticePermission(member, request.notice());
		board.update(request.title().trim(), request.content().trim(), request.notice());
		List<CommentResponse> comments = commentRepository.findByBoardIdOrderByCreatedAtAsc(boardId).stream()
			.map(this::toCommentResponse).toList();
		return toBoardResponse(board, comments);
	}

	/** 작성자 또는 관리자만 게시글을 삭제할 수 있으며, 게시글과 댓글의 관계가 어긋나지 않도록 연관 댓글도 함께 제거합니다. */
	@Transactional
	public void deleteBoard(Long boardId, Long memberId) {
		// 게시글·회원 조회 → 삭제 권한 확인 → 게시글 삭제 순서로 처리합니다.
		Board board = findBoard(boardId);
		Member member = findMember(memberId);
		validateDeletePermission(board.getAuthor(), member, new BoardAccessDeniedException());
		boardRepository.delete(board);
	}

	/** 관리자 권한을 확인한 뒤 작성자와 관계없이 공지 상태를 변경해 관리자의 공지 관리를 허용합니다. */
	@Transactional
	public void updateNoticeByAdmin(Long boardId, Long memberId, boolean notice) {
		// 관리자 권한 확인 → 게시글 조회 → 공지 상태 변경 순서로 처리합니다.
		Member member = findMember(memberId);
		validateAdmin(member);
		Board board = findBoard(boardId);
		board.update(board.getTitle(), board.getContent(), notice);
	}

	/** 게시글 ID 없이 댓글 ID로 삭제하되, 관리자만 작성자 확인 없이 삭제할 수 있도록 권한을 먼저 확인합니다. */
	@Transactional
	public void deleteCommentByAdmin(Long commentId, Long memberId) {
		// 관리자 권한 확인 → 댓글 조회 → 댓글 삭제 순서로 처리합니다.
		Member member = findMember(memberId);
		validateAdmin(member);
		Comment comment = commentRepository.findById(commentId).orElseThrow(CommentNotFoundException::new);
		commentRepository.delete(comment);
	}

	/** URL의 게시글 ID로 댓글의 소속 게시글을 지정하고, 인증된 회원을 작성자로 저장해 댓글 관계를 보장합니다. */
	@Transactional
	public CommentResponse createComment(Long boardId, Long memberId, CommentCreateRequest request) {
		// 게시글·회원 조회 → 댓글 저장 → 댓글 응답 변환 순서로 처리합니다.
		Board board = findBoard(boardId);
		Member author = findMember(memberId);
		Comment comment = commentRepository.save(new Comment(request.content().trim(), board, author));
		return toCommentResponse(comment);
	}

	/** 댓글이 URL의 게시글에 속하는지 먼저 확인하고 작성자만 수정하게 해 다른 게시글을 통한 우회 수정을 막습니다. */
	@Transactional
	public CommentResponse updateComment(Long boardId, Long commentId, Long memberId, CommentUpdateRequest request) {
		// 게시글 소속 댓글·회원 조회 → 작성자 권한 확인 → 내용 수정 및 응답 변환 순서로 처리합니다.
		Comment comment = findCommentInBoard(boardId, commentId);
		Member member = findMember(memberId);
		validateAuthor(comment.getAuthor(), member, new CommentAccessDeniedException());
		comment.update(request.content().trim());
		return toCommentResponse(comment);
	}

	/** 댓글이 URL의 게시글에 속하는지 확인한 뒤 작성자 또는 관리자만 삭제하게 해 다른 게시글 URL을 통한 우회 삭제를 막습니다. */
	@Transactional
	public void deleteComment(Long boardId, Long commentId, Long memberId) {
		// 게시글 소속 댓글·회원 조회 → 삭제 권한 확인 → 댓글 삭제 순서로 처리합니다.
		Comment comment = findCommentInBoard(boardId, commentId);
		Member member = findMember(memberId);
		validateDeletePermission(comment.getAuthor(), member, new CommentAccessDeniedException());
		commentRepository.delete(comment);
	}

	/** 존재하지 않는 게시글 ID는 게시글 전용 예외로 변환합니다. */
	private Board findBoard(Long boardId) {
		return boardRepository.findById(boardId).orElseThrow(BoardNotFoundException::new);
	}

	/** 회원이 탈퇴했거나 잘못된 ID인 경우 권한 오류로 처리합니다. */
	private Member findMember(Long memberId) {
		return memberRepository.findById(memberId).orElseThrow(BoardAccessDeniedException::new);
	}

	/** 댓글 ID뿐 아니라 URL의 게시글 ID와 실제 소속 게시글도 일치하는지 확인합니다. */
	private Comment findCommentInBoard(Long boardId, Long commentId) {
		Comment comment = commentRepository.findById(commentId).orElseThrow(CommentNotFoundException::new);
		if (!comment.getBoard().getId().equals(boardId)) {
			throw new CommentNotFoundException();
		}
		return comment;
	}

	/** 공지로 지정하는 요청은 관리자에게만 허용합니다. */
	private void validateNoticePermission(Member member, boolean notice) {
		if (notice && member.getRole() != Role.ADMIN) {
			throw new BoardAccessDeniedException();
		}
	}

	/** 수정 작업의 대상 작성자와 요청 회원이 같은지 검사합니다. */
	private void validateAuthor(Member author, Member member, RuntimeException exception) {
		if (!author.getId().equals(member.getId())) {
			throw exception;
		}
	}

	/** 삭제는 작성자와 관리자에게 허용하고, 그 외 회원은 거부합니다. */
	private void validateDeletePermission(Member author, Member member, RuntimeException exception) {
		if (!author.getId().equals(member.getId()) && member.getRole() != Role.ADMIN) {
			throw exception;
		}
	}

	/** 관리자 전용 작업을 실행하기 전에 역할을 확인합니다. */
	private void validateAdmin(Member member) {
		if (member.getRole() != Role.ADMIN) {
			throw new BoardAccessDeniedException();
		}
	}

	/** 목록 화면에 필요한 게시글 요약 정보만 응답 DTO로 옮깁니다. */
	private BoardListResponse toBoardListResponse(Board board) {
		return new BoardListResponse(board.getId(), board.getTitle(), board.isNotice(), board.getAuthor().getId(),
			board.getAuthor().getName(), board.getViewCount(), board.getCreatedAt());
	}

	/** 게시글 상세 정보와 이미 조회한 댓글 목록을 응답 DTO로 구성합니다. */
	private BoardResponse toBoardResponse(Board board, List<CommentResponse> comments) {
		return new BoardResponse(board.getId(), board.getTitle(), board.getContent(), board.getViewCount(), board.isNotice(),
			board.getAuthor().getId(), board.getAuthor().getName(), board.getCreatedAt(), board.getUpdatedAt(), comments);
	}

	/** 댓글 엔티티의 작성자와 작성·수정 시각을 API 응답 형태로 변환합니다. */
	private CommentResponse toCommentResponse(Comment comment) {
		return new CommentResponse(comment.getId(), comment.getContent(), comment.getAuthor().getId(),
			comment.getAuthor().getName(), comment.getCreatedAt(), comment.getUpdatedAt());
	}

}
````

## 3. import·애노테이션·필드·메서드·키워드 설명
### 이 파일에서 실제 사용한 애노테이션
- `@Service`: Spring 서비스 객체로 등록합니다. (적용 소스: `@Service`)
- `@Transactional`: 트랜잭션 경계로 실행합니다. (적용 소스: `@Transactional`)
- `@Transactional`: 트랜잭션 경계로 실행합니다. (적용 소스: `@Transactional(readOnly = true)`)
- `@Transactional`: 트랜잭션 경계로 실행합니다. (적용 소스: `@Transactional`)
- `@Transactional`: 트랜잭션 경계로 실행합니다. (적용 소스: `@Transactional`)
- `@Transactional`: 트랜잭션 경계로 실행합니다. (적용 소스: `@Transactional`)
- `@Transactional`: 트랜잭션 경계로 실행합니다. (적용 소스: `@Transactional`)
- `@Transactional`: 트랜잭션 경계로 실행합니다. (적용 소스: `@Transactional`)
- `@Transactional`: 트랜잭션 경계로 실행합니다. (적용 소스: `@Transactional`)
- `@Transactional`: 트랜잭션 경계로 실행합니다. (적용 소스: `@Transactional`)
- `@Transactional`: 트랜잭션 경계로 실행합니다. (적용 소스: `@Transactional`)

### 이 파일에 나타난 Java 키워드
- `true`: 참을 나타내는 boolean 리터럴입니다.
- `this`: 현재 객체를 가리킵니다.
- `throw`: 예외를 던져 정상 흐름을 중단합니다.
- `boolean`: true/false 논리 타입입니다.
- `null`: 값이 없음을 나타내는 리터럴입니다.
- `return`: 메서드 결과를 반환하거나 실행을 끝냅니다.
- `class`: 클래스를 선언합니다.
- `void`: 반환값이 없는 메서드입니다.
- `if`: 조건이 참일 때 블록을 실행합니다.
- `private`: 현재 클래스 내부에서만 접근할 수 있습니다.
- `new`: 새 객체를 생성합니다.
- `int`: 정수 기본 타입입니다.
- `package`: 타입이 속한 네임스페이스를 선언합니다.
- `public`: 외부에서 접근할 수 있습니다.
- `import`: 다른 패키지 타입을 짧은 이름으로 사용하게 합니다.
- `final`: 적용 대상에 따라 재할당/재정의를 제한합니다.

### import별 용도
- `java.util.List`: 리스트 컬렉션 타입입니다.
- `org.springframework.data.domain.Page`: 페이지 데이터 타입입니다.
- `org.springframework.data.domain.PageRequest`: 페이지 데이터 타입입니다.
- `org.springframework.data.domain.Sort`: 정렬 조건 타입입니다.
- `org.springframework.stereotype.Service`: Spring 프레임워크 타입입니다.
- `org.springframework.transaction.annotation.Transactional`: Spring 프레임워크 타입입니다.
- `com.cleaner.be.auth.Member`: 작성자 회원 엔티티입니다.
- `com.cleaner.be.auth.MemberRepository`: 작성자 회원 엔티티입니다.
- `com.cleaner.be.auth.Role`: 인증 도메인 타입입니다.
- `com.cleaner.be.board.dto.BoardCreateRequest`: 게시판 DTO입니다.
- `com.cleaner.be.board.dto.BoardListResponse`: 게시판 DTO입니다.
- `com.cleaner.be.board.dto.BoardPageResponse`: 게시판 DTO입니다.
- `com.cleaner.be.board.dto.BoardResponse`: 게시판 DTO입니다.
- `com.cleaner.be.board.dto.BoardUpdateRequest`: 게시판 DTO입니다.
- `com.cleaner.be.board.dto.CommentCreateRequest`: 게시판 DTO입니다.
- `com.cleaner.be.board.dto.CommentResponse`: 게시판 DTO입니다.
- `com.cleaner.be.board.dto.CommentUpdateRequest`: 게시판 DTO입니다.

### 필드와 컴포넌트
세 저장소 boardRepository/commentRepository/memberRepository를 생성자 주입합니다.

### 애노테이션과 Java 문법
`package`는 네임스페이스입니다. `public/private/protected`는 접근 범위를 제어합니다. 아래 소스에서 실제로 사용한 애노테이션은 해당 선언에 프레임워크 동작을 연결합니다. 소스에 없는 애노테이션 동작은 가정하지 않습니다. `record`는 컴포넌트 기반 타입이며 생성자·접근자 등의 일부 메서드를 자동 생성합니다. 저장소 `interface` 구현은 Spring Data가 제공합니다.

## 4. 메서드별 목적·매개변수·순서·호출·반환·예외
### 선언된 메서드/생성자 시그니처
매개변수 타입과 나열 순서는 아래 시그니처 그대로이며, 괄호 앞 타입은 반환형입니다. 생성자는 반환형이 없습니다. `void`는 값을 반환하지 않습니다.
- `public BoardService(BoardRepository boardRepository, CommentRepository commentRepository, MemberRepository memberRepository)`
- `public BoardResponse createBoard(Long memberId, BoardCreateRequest request)`
- `public BoardPageResponse getBoards(int page, int size, String keyword)`
- `public BoardResponse getBoard(Long boardId)`
- `public BoardResponse updateBoard(Long boardId, Long memberId, BoardUpdateRequest request)`
- `public void deleteBoard(Long boardId, Long memberId)`
- `public void updateNoticeByAdmin(Long boardId, Long memberId, boolean notice)`
- `public void deleteCommentByAdmin(Long commentId, Long memberId)`
- `public CommentResponse createComment(Long boardId, Long memberId, CommentCreateRequest request)`
- `public CommentResponse updateComment(Long boardId, Long commentId, Long memberId, CommentUpdateRequest request)`
- `public void deleteComment(Long boardId, Long commentId, Long memberId)`
- `private Board findBoard(Long boardId)`
- `private Member findMember(Long memberId)`
- `private Comment findCommentInBoard(Long boardId, Long commentId)`
- `private void validateNoticePermission(Member member, boolean notice)`
- `private void validateAuthor(Member author, Member member, RuntimeException exception)`
- `private void validateDeletePermission(Member author, Member member, RuntimeException exception)`
- `private void validateAdmin(Member member)`
- `private BoardListResponse toBoardListResponse(Board board)`
- `private BoardResponse toBoardResponse(Board board, List<CommentResponse> comments)`
- `private CommentResponse toCommentResponse(Comment comment)`

소스에서 발생시키거나 전달하는 예외 타입: `BoardAccessDeniedException`, `BoardNotFoundException`, `CommentAccessDeniedException`, `CommentNotFoundException`, `RuntimeException`. 발생 위치에서 정상 흐름이 중단되며, 전역 처리기 매핑은 관계 설명을 참고합니다.
createBoard: 회원→공지 권한→저장→DTO. getBoards: notice DESC·createdAt DESC 정렬/검색/페이지 DTO. getBoard: 게시글 조회→조회수 증가→시간순 댓글→상세 DTO. updateBoard: 게시글/회원→작성자·공지 권한→수정→DTO. deleteBoard: 작성자/관리자 확인→삭제. updateNoticeByAdmin: 관리자 확인→공지 변경. deleteCommentByAdmin: 관리자 확인→댓글 삭제. createComment: 게시글/회원→저장. updateComment: URL 소속/작성자 확인→수정. deleteComment: 소속/작성자 또는 관리자 확인→삭제. findBoard/findMember/findCommentInBoard는 존재/소속 확인. validateNoticePermission/validateAuthor/validateDeletePermission/validateAdmin은 권한 확인. toBoardListResponse/toBoardResponse/toCommentResponse는 DTO 변환.

직접 선언 메서드와 record 자동 생성/저장소 상속 메서드를 구분했습니다. 예외가 발생하면 이후 정상 단계는 수행되지 않습니다. 프레임워크의 바인딩·DB·검증 오류는 도메인 예외와 별도로 전파될 수 있습니다.

## 5. 확인된 end-to-end 실행 흐름
Controller→Service→Repository. 목록은 공지 우선·최신순으로 페이지를 만들고 검색 여부에 따라 저장소 쿼리를 선택합니다. 상세/게시글 수정은 댓글을 생성시각 ASC로 가져옵니다. 쓰기는 @Transactional, 목록은 readOnly. 전용 예외는 전역 처리기로 전파됩니다.

## 6. 확인된 클래스 관계와 추론
확인됨: Controller, 3개 저장소, Member/Role/DTO를 사용. GlobalExceptionHandler가 부재 404·권한 403. 관리자 메서드 둘은 BoardController 호출점이 없으며 다른 호출자는 이 코드만으로 단정하지 않습니다.

## 7. 이 파일에서 익힐 개념
서비스 계층, 트랜잭션, dirty checking, 페이지/정렬, 스트림/메서드 참조.

## 8. 소스와 일치하는 예시
상세 조회는 게시글 조회→조회수 증가→댓글 조회→BoardResponse 조립 순입니다.

## 9. 핵심 요약
게시글과 댓글 업무 규칙 및 실행 순서를 담당합니다.



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
- Java 원본: [BoardService.java](../../src/main/java/com/cleaner/be/board/BoardService.java)
- 게시판 기준: `cleaner-be/src/main/java/com/cleaner/be/board/`
- DTO 기준: `cleaner-be/src/main/java/com/cleaner/be/board/dto/`
- 관련 타입: `cleaner-be/src/main/java/com/cleaner/be/auth/Member.java`, `cleaner-be/src/main/java/com/cleaner/be/common/entity/BaseTimeEntity.java