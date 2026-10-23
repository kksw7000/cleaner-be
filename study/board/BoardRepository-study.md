# BoardRepository.java 학습 노트

## 1. 파일의 역할·계층·존재 이유·사용 시점
- **계층:** 저장소 인터페이스
- **역할:** 게시글 CRUD와 제목 검색 페이지입니다.
- **왜 필요한가:** Spring Data JPA가 구현 및 CRUD를 제공합니다.
- **언제 사용하는가:** 서비스의 생성·검색·목록·상세·삭제 시
- **원본:** [BoardRepository.java](../../src/main/java/com/cleaner/be/board/BoardRepository.java) (문서와 같은 디렉터리).

## 2. 현재 소스 전체
아래는 현재 Java 원본 전체이며 생략한 코드는 없습니다.

````java
package com.cleaner.be.board;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

/** 게시글의 기본 CRUD와 제목 검색을 담당하며, 응답 변환에 필요한 작성자를 함께 조회합니다. */
public interface BoardRepository extends JpaRepository<Board, Long> {

	/** 제목에 검색어가 포함된 게시글만 대소문자 구분 없이 페이지 단위로 찾습니다. */
	@EntityGraph(attributePaths = "author")
	Page<Board> findByTitleContainingIgnoreCase(String titleKeyword, Pageable pageable);

	/** 작성자 정보도 함께 읽어 목록 응답 변환 중 추가 조회가 발생하지 않게 합니다. */
	@Override
	@EntityGraph(attributePaths = "author")
	Page<Board> findAll(Pageable pageable);
}
````

## 3. import·애노테이션·필드·메서드·키워드 설명
### 이 파일에서 실제 사용한 애노테이션
- `@EntityGraph`: 조회 때 함께 읽을 연관 필드를 지정합니다. (적용 소스: `@EntityGraph(attributePaths = "author")`)
- `@EntityGraph`: 조회 때 함께 읽을 연관 필드를 지정합니다. (적용 소스: `@EntityGraph(attributePaths = "author")`)

### 이 파일에 나타난 Java 키워드
- `extends`: 상속 관계를 선언합니다.
- `package`: 타입이 속한 네임스페이스를 선언합니다.
- `public`: 외부에서 접근할 수 있습니다.
- `interface`: 구현 계약을 선언합니다.
- `import`: 다른 패키지 타입을 짧은 이름으로 사용하게 합니다.

### import별 용도
- `org.springframework.data.domain.Page`: 페이지 데이터 타입입니다.
- `org.springframework.data.domain.Pageable`: 페이지 데이터 타입입니다.
- `org.springframework.data.jpa.repository.EntityGraph`: 연관 엔티티 fetch 계획입니다.
- `org.springframework.data.jpa.repository.JpaRepository`: Spring Data JPA CRUD 인터페이스입니다.

### 필드와 컴포넌트
필드 없음. JpaRepository<Board,Long>를 상속합니다.

### 애노테이션과 Java 문법
`package`는 네임스페이스입니다. `public/private/protected`는 접근 범위를 제어합니다. 아래 소스에서 실제로 사용한 애노테이션은 해당 선언에 프레임워크 동작을 연결합니다. 소스에 없는 애노테이션 동작은 가정하지 않습니다. `record`는 컴포넌트 기반 타입이며 생성자·접근자 등의 일부 메서드를 자동 생성합니다. 저장소 `interface` 구현은 Spring Data가 제공합니다.

## 4. 메서드별 목적·매개변수·순서·호출·반환·예외
### 선언된 메서드/생성자 시그니처
이 파일은 직접 선언한 메서드가 없습니다. record 접근자 또는 JpaRepository 상속 메서드는 자동 제공됩니다.

이 파일은 도메인 예외를 직접 던지는 코드가 없습니다. 프레임워크/호출자의 오류는 별도로 전파될 수 있습니다.
상속 save/findById/findAll/delete는 CRUD입니다. findByTitleContainingIgnoreCase(keyword,pageable)는 제목 대소문자 무시 부분 검색 페이지. findAll(pageable)는 전체 페이지 조회. EntityGraph(author)는 작성자를 fetch 대상으로 지정합니다.

직접 선언 메서드와 record 자동 생성/저장소 상속 메서드를 구분했습니다. 예외가 발생하면 이후 정상 단계는 수행되지 않습니다. 프레임워크의 바인딩·DB·검증 오류는 도메인 예외와 별도로 전파될 수 있습니다.

## 5. 확인된 end-to-end 실행 흐름
서비스가 PageRequest를 만들고 빈 키워드면 findAll, 아니면 제목 검색 선택. 생성·상세·삭제는 CRUD 호출.

## 6. 확인된 클래스 관계와 추론
확인됨: BoardService가 호출하고 Board/Long을 다룹니다. author는 Board 필드입니다.

## 7. 이 파일에서 익힐 개념
Spring Data 파생 쿼리, Page/Pageable, EntityGraph.

## 8. 소스와 일치하는 예시
검색어 공지를 페이지 요청과 함께 전달해 제목 부분 검색합니다.

## 9. 핵심 요약
DB 접근 계약이며 정렬·권한·DTO 변환은 서비스가 담당합니다.



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
- Java 원본: [BoardRepository.java](../../src/main/java/com/cleaner/be/board/BoardRepository.java)
- 게시판 기준: `cleaner-be/src/main/java/com/cleaner/be/board/`
- DTO 기준: `cleaner-be/src/main/java/com/cleaner/be/board/dto/`
- 관련 타입: `cleaner-be/src/main/java/com/cleaner/be/auth/Member.java`, `cleaner-be/src/main/java/com/cleaner/be/common/entity/BaseTimeEntity.java