# Board.java 학습 노트

## 1. 파일의 역할·계층·존재 이유·사용 시점
- **계층:** 엔티티
- **역할:** 게시글 영속 상태와 도메인 동작입니다.
- **왜 필요한가:** DB 매핑과 허용된 상태 변경을 캡슐화합니다.
- **언제 사용하는가:** 게시글 생성·수정·상세 조회 시
- **원본:** [Board.java](../../src/main/java/com/cleaner/be/board/Board.java) (문서와 같은 디렉터리).

## 2. 현재 소스 전체
아래는 현재 Java 원본 전체이며 생략한 코드는 없습니다.

````java
package com.cleaner.be.board;

import java.util.ArrayList;
import java.util.List;

import com.cleaner.be.auth.Member;
import com.cleaner.be.common.entity.BaseTimeEntity;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;

/** 자유 게시판의 게시글과 게시글에 종속되는 댓글 컬렉션을 관리합니다. */
@Entity
@Table(name = "boards")
public class Board extends BaseTimeEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(nullable = false, length = 100)
	private String title;

	@Column(nullable = false, columnDefinition = "TEXT")
	private String content;

	@Column(nullable = false)
	private long viewCount;

	@Column(nullable = false)
	private boolean notice;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	private Member author;

	// 게시글이 삭제되면 독립적으로 존재할 수 없는 댓글도 함께 정리합니다.
	@OneToMany(mappedBy = "board", cascade = CascadeType.REMOVE, orphanRemoval = true)
	private List<Comment> comments = new ArrayList<>();

	protected Board() {
	}

	public Board(String title, String content, boolean notice, Member author) {
		this.title = title;
		this.content = content;
		this.notice = notice;
		this.author = author;
	}

	public Long getId() { return id; }
	public String getTitle() { return title; }
	public String getContent() { return content; }
	public long getViewCount() { return viewCount; }
	public boolean isNotice() { return notice; }
	public Member getAuthor() { return author; }
	public List<Comment> getComments() { return comments; }

	/** 수정 요청에서 검증된 값만 반영해 작성자와 조회수 같은 보호 대상은 변경하지 않습니다. */
	public void update(String title, String content, boolean notice) {
		this.title = title;
		this.content = content;
		this.notice = notice;
	}

	/** 상세 조회 시 열람 횟수를 엔티티 변경 감지로 반영합니다. */
	public void increaseViewCount() {
		viewCount++;
	}
}
````

## 3. import·애노테이션·필드·메서드·키워드 설명
### 이 파일에서 실제 사용한 애노테이션
- `@Entity`: JPA 관리 엔티티임을 표시합니다. (적용 소스: `@Entity`)
- `@Table`: DB 테이블 이름을 지정합니다. (적용 소스: `@Table(name = "boards")`)
- `@Id`: 기본 키 필드를 지정합니다. (적용 소스: `@Id`)
- `@GeneratedValue`: 기본 키 생성 전략을 지정합니다. (적용 소스: `@GeneratedValue(strategy = GenerationType.IDENTITY)`)
- `@Column`: DB 컬럼 제약과 매핑을 지정합니다. (적용 소스: `@Column(nullable = false, length = 100)`)
- `@Column`: DB 컬럼 제약과 매핑을 지정합니다. (적용 소스: `@Column(nullable = false, columnDefinition = "TEXT")`)
- `@Column`: DB 컬럼 제약과 매핑을 지정합니다. (적용 소스: `@Column(nullable = false)`)
- `@Column`: DB 컬럼 제약과 매핑을 지정합니다. (적용 소스: `@Column(nullable = false)`)
- `@ManyToOne`: 여러 엔티티가 하나의 대상 엔티티를 참조하는 관계입니다. (적용 소스: `@ManyToOne(fetch = FetchType.LAZY, optional = false)`)
- `@OneToMany`: 현재 엔티티가 여러 자식 엔티티를 갖는 관계입니다. (적용 소스: `@OneToMany(mappedBy = "board", cascade = CascadeType.REMOVE, orphanRemoval = true)`)

### 이 파일에 나타난 Java 키워드
- `true`: 참을 나타내는 boolean 리터럴입니다.
- `this`: 현재 객체를 가리킵니다.
- `boolean`: true/false 논리 타입입니다.
- `extends`: 상속 관계를 선언합니다.
- `return`: 메서드 결과를 반환하거나 실행을 끝냅니다.
- `class`: 클래스를 선언합니다.
- `void`: 반환값이 없는 메서드입니다.
- `protected`: 패키지/하위 클래스에서 접근할 수 있습니다.
- `private`: 현재 클래스 내부에서만 접근할 수 있습니다.
- `false`: 거짓을 나타내는 boolean 리터럴입니다.
- `new`: 새 객체를 생성합니다.
- `long`: 정수 기본 타입입니다.
- `package`: 타입이 속한 네임스페이스를 선언합니다.
- `public`: 외부에서 접근할 수 있습니다.
- `import`: 다른 패키지 타입을 짧은 이름으로 사용하게 합니다.

### import별 용도
- `java.util.ArrayList`: 가변 배열 기반 리스트 구현체입니다.
- `java.util.List`: 리스트 컬렉션 타입입니다.
- `com.cleaner.be.auth.Member`: 작성자 회원 엔티티입니다.
- `com.cleaner.be.common.entity.BaseTimeEntity`: 감사 시각 기반 엔티티입니다.
- `jakarta.persistence.CascadeType`: JPA 매핑 타입/애노테이션입니다.
- `jakarta.persistence.Column`: JPA 매핑 타입/애노테이션입니다.
- `jakarta.persistence.Entity`: JPA 매핑 타입/애노테이션입니다.
- `jakarta.persistence.FetchType`: JPA 매핑 타입/애노테이션입니다.
- `jakarta.persistence.GeneratedValue`: JPA 매핑 타입/애노테이션입니다.
- `jakarta.persistence.GenerationType`: JPA 매핑 타입/애노테이션입니다.
- `jakarta.persistence.Id`: JPA 매핑 타입/애노테이션입니다.
- `jakarta.persistence.ManyToOne`: JPA 매핑 타입/애노테이션입니다.
- `jakarta.persistence.OneToMany`: JPA 매핑 타입/애노테이션입니다.
- `jakarta.persistence.Table`: JPA 매핑 타입/애노테이션입니다.

### 필드와 컴포넌트
`id` 자동 ID, `title` 최대 100자, `content` TEXT, `viewCount`, `notice`, `author`, `comments`입니다. 시각 필드는 BaseTimeEntity 상속.

### 애노테이션과 Java 문법
`package`는 네임스페이스입니다. `public/private/protected`는 접근 범위를 제어합니다. 아래 소스에서 실제로 사용한 애노테이션은 해당 선언에 프레임워크 동작을 연결합니다. 소스에 없는 애노테이션 동작은 가정하지 않습니다. `record`는 컴포넌트 기반 타입이며 생성자·접근자 등의 일부 메서드를 자동 생성합니다. 저장소 `interface` 구현은 Spring Data가 제공합니다.

## 4. 메서드별 목적·매개변수·순서·호출·반환·예외
### 선언된 메서드/생성자 시그니처
매개변수 타입과 나열 순서는 아래 시그니처 그대로이며, 괄호 앞 타입은 반환형입니다. 생성자는 반환형이 없습니다. `void`는 값을 반환하지 않습니다.
- `private List<Comment> comments = new ArrayList<>();  protected Board()`
- `public Board(String title, String content, boolean notice, Member author)`
- `public Long getId()`
- `public String getTitle()`
- `public String getContent()`
- `public long getViewCount()`
- `public boolean isNotice()`
- `public Member getAuthor()`
- `public List<Comment> getComments()`
- `public void update(String title, String content, boolean notice)`
- `public void increaseViewCount()`

이 파일은 도메인 예외를 직접 던지는 코드가 없습니다. 프레임워크/호출자의 오류는 별도로 전파될 수 있습니다.
생성자는 제목·본문·공지·작성자를 설정합니다. getter/isNotice는 값을 읽습니다. update는 제목·본문·공지 여부만 변경하고 작성자/조회수는 보존합니다. increaseViewCount는 1 증가합니다. protected 무인자는 JPA용입니다.

직접 선언 메서드와 record 자동 생성/저장소 상속 메서드를 구분했습니다. 예외가 발생하면 이후 정상 단계는 수행되지 않습니다. 프레임워크의 바인딩·DB·검증 오류는 도메인 예외와 별도로 전파될 수 있습니다.

## 5. 확인된 end-to-end 실행 흐름
서비스 생성/저장 이후 상세 조회가 게시글을 읽고 조회수를 올린 뒤 댓글을 조회/응답합니다. 트랜잭션 변경 감지가 조회수 변경을 반영합니다. REMOVE/orphanRemoval은 게시글 삭제 시 댓글 정리를 지정합니다.

## 6. 확인된 클래스 관계와 추론
확인됨: Member 다대일, Comment 컬렉션, BaseTimeEntity 상속, BoardRepository 사용. 댓글 종속 의도는 주석과 orphanRemoval에서 확인됩니다.

## 7. 이 파일에서 익힐 개념
JPA 매핑, LAZY 관계, cascade REMOVE, orphanRemoval, dirty checking.

## 8. 소스와 일치하는 예시
서비스가 Board(title,content,notice,author)를 생성해 저장하며 권한 확인은 먼저 수행합니다.

## 9. 핵심 요약
게시글 영속 상태와 허용된 변경을 정의합니다.



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
- Java 원본: [Board.java](../../src/main/java/com/cleaner/be/board/Board.java)
- 게시판 기준: `cleaner-be/src/main/java/com/cleaner/be/board/`
- DTO 기준: `cleaner-be/src/main/java/com/cleaner/be/board/dto/`
- 관련 타입: `cleaner-be/src/main/java/com/cleaner/be/auth/Member.java`, `cleaner-be/src/main/java/com/cleaner/be/common/entity/BaseTimeEntity.java