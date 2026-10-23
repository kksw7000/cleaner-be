# Comment.java 학습 노트

## 1. 파일의 역할·계층·존재 이유·사용 시점
- **계층:** 엔티티
- **역할:** 게시글 종속 댓글 상태와 수정입니다.
- **왜 필요한가:** 본문·게시글·작성자를 함께 보관합니다.
- **언제 사용하는가:** 댓글 생성·수정·응답 시
- **원본:** [Comment.java](../../src/main/java/com/cleaner/be/board/Comment.java) (문서와 같은 디렉터리).

## 2. 현재 소스 전체
아래는 현재 Java 원본 전체이며 생략한 코드는 없습니다.

````java
package com.cleaner.be.board;

import com.cleaner.be.auth.Member;
import com.cleaner.be.common.entity.BaseTimeEntity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

/** 댓글은 반드시 하나의 게시글에 속하며 게시글 밖으로 이동할 수 없습니다. */
@Entity
@Table(name = "comments")
public class Comment extends BaseTimeEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(nullable = false, columnDefinition = "TEXT")
	private String content;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	private Board board;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	private Member author;

	protected Comment() {
	}

	public Comment(String content, Board board, Member author) {
		this.content = content;
		this.board = board;
		this.author = author;
	}

	public Long getId() { return id; }
	public String getContent() { return content; }
	public Board getBoard() { return board; }
	public Member getAuthor() { return author; }

	/** 작성자 권한을 서비스에서 확인한 뒤 검증된 댓글 내용만 교체합니다. */
	public void update(String content) {
		this.content = content;
	}
}
````

## 3. import·애노테이션·필드·메서드·키워드 설명
### 이 파일에서 실제 사용한 애노테이션
- `@Entity`: JPA 관리 엔티티임을 표시합니다. (적용 소스: `@Entity`)
- `@Table`: DB 테이블 이름을 지정합니다. (적용 소스: `@Table(name = "comments")`)
- `@Id`: 기본 키 필드를 지정합니다. (적용 소스: `@Id`)
- `@GeneratedValue`: 기본 키 생성 전략을 지정합니다. (적용 소스: `@GeneratedValue(strategy = GenerationType.IDENTITY)`)
- `@Column`: DB 컬럼 제약과 매핑을 지정합니다. (적용 소스: `@Column(nullable = false, columnDefinition = "TEXT")`)
- `@ManyToOne`: 여러 엔티티가 하나의 대상 엔티티를 참조하는 관계입니다. (적용 소스: `@ManyToOne(fetch = FetchType.LAZY, optional = false)`)
- `@ManyToOne`: 여러 엔티티가 하나의 대상 엔티티를 참조하는 관계입니다. (적용 소스: `@ManyToOne(fetch = FetchType.LAZY, optional = false)`)

### 이 파일에 나타난 Java 키워드
- `this`: 현재 객체를 가리킵니다.
- `extends`: 상속 관계를 선언합니다.
- `return`: 메서드 결과를 반환하거나 실행을 끝냅니다.
- `class`: 클래스를 선언합니다.
- `void`: 반환값이 없는 메서드입니다.
- `protected`: 패키지/하위 클래스에서 접근할 수 있습니다.
- `private`: 현재 클래스 내부에서만 접근할 수 있습니다.
- `false`: 거짓을 나타내는 boolean 리터럴입니다.
- `package`: 타입이 속한 네임스페이스를 선언합니다.
- `public`: 외부에서 접근할 수 있습니다.
- `import`: 다른 패키지 타입을 짧은 이름으로 사용하게 합니다.

### import별 용도
- `com.cleaner.be.auth.Member`: 작성자 회원 엔티티입니다.
- `com.cleaner.be.common.entity.BaseTimeEntity`: 감사 시각 기반 엔티티입니다.
- `jakarta.persistence.Column`: JPA 매핑 타입/애노테이션입니다.
- `jakarta.persistence.Entity`: JPA 매핑 타입/애노테이션입니다.
- `jakarta.persistence.FetchType`: JPA 매핑 타입/애노테이션입니다.
- `jakarta.persistence.GeneratedValue`: JPA 매핑 타입/애노테이션입니다.
- `jakarta.persistence.GenerationType`: JPA 매핑 타입/애노테이션입니다.
- `jakarta.persistence.Id`: JPA 매핑 타입/애노테이션입니다.
- `jakarta.persistence.ManyToOne`: JPA 매핑 타입/애노테이션입니다.
- `jakarta.persistence.Table`: JPA 매핑 타입/애노테이션입니다.

### 필드와 컴포넌트
`id` 자동 ID, `content` TEXT, `board` 게시글, `author` 회원. 시각은 BaseTimeEntity 상속.

### 애노테이션과 Java 문법
`package`는 네임스페이스입니다. `public/private/protected`는 접근 범위를 제어합니다. 아래 소스에서 실제로 사용한 애노테이션은 해당 선언에 프레임워크 동작을 연결합니다. 소스에 없는 애노테이션 동작은 가정하지 않습니다. `record`는 컴포넌트 기반 타입이며 생성자·접근자 등의 일부 메서드를 자동 생성합니다. 저장소 `interface` 구현은 Spring Data가 제공합니다.

## 4. 메서드별 목적·매개변수·순서·호출·반환·예외
### 선언된 메서드/생성자 시그니처
매개변수 타입과 나열 순서는 아래 시그니처 그대로이며, 괄호 앞 타입은 반환형입니다. 생성자는 반환형이 없습니다. `void`는 값을 반환하지 않습니다.
- `protected Comment()`
- `public Comment(String content, Board board, Member author)`
- `public Long getId()`
- `public String getContent()`
- `public Board getBoard()`
- `public Member getAuthor()`
- `public void update(String content)`

이 파일은 도메인 예외를 직접 던지는 코드가 없습니다. 프레임워크/호출자의 오류는 별도로 전파될 수 있습니다.
생성자는 본문/게시글/작성자를 설정합니다. getter는 값을 반환합니다. update(content)는 본문만 바꾸며 권한 확인은 서비스 책임입니다.

직접 선언 메서드와 record 자동 생성/저장소 상속 메서드를 구분했습니다. 예외가 발생하면 이후 정상 단계는 수행되지 않습니다. 프레임워크의 바인딩·DB·검증 오류는 도메인 예외와 별도로 전파될 수 있습니다.

## 5. 확인된 end-to-end 실행 흐름
서비스가 게시글과 회원을 조회한 후 저장합니다. 수정 시 URL 게시글과 댓글의 실제 소속 및 작성자 확인 후 내용을 변경합니다.

## 6. 확인된 클래스 관계와 추론
확인됨: Board·Member에 필수 LAZY 다대일, BaseTimeEntity 상속, CommentRepository에서 사용.

## 7. 이 파일에서 익힐 개념
JPA 엔티티, 외래키 가진 다대일 소유측, 지연 로딩.

## 8. 소스와 일치하는 예시
댓글 생성은 본문 외 board와 author도 필요합니다.

## 9. 핵심 요약
게시글·작성자 관계를 가진 댓글 엔티티입니다.



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
- Java 원본: [Comment.java](../../src/main/java/com/cleaner/be/board/Comment.java)
- 게시판 기준: `cleaner-be/src/main/java/com/cleaner/be/board/`
- DTO 기준: `cleaner-be/src/main/java/com/cleaner/be/board/dto/`
- 관련 타입: `cleaner-be/src/main/java/com/cleaner/be/auth/Member.java`, `cleaner-be/src/main/java/com/cleaner/be/common/entity/BaseTimeEntity.java