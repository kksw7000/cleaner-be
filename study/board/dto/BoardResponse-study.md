# BoardResponse.java 학습 노트

## 1. 파일의 역할·계층·존재 이유·사용 시점
- **계층:** 응답 DTO
- **역할:** 게시글 상세·생성·수정 결과입니다.
- **왜 필요한가:** 본문·작성자·시각·댓글을 API 전용 구조로 제공합니다.
- **언제 사용하는가:** 상세 조회, 생성/수정 결과
- **원본:** [BoardResponse.java](../../../src/main/java/com/cleaner/be/board/dto/BoardResponse.java) (문서와 같은 디렉터리).

## 2. 현재 소스 전체
아래는 현재 Java 원본 전체이며 생략한 코드는 없습니다.

````java
package com.cleaner.be.board.dto;

import java.time.LocalDateTime;
import java.util.List;

/** 게시글 상세 API 응답으로, 작성자·조회수·생성 및 수정 시각과 댓글 목록을 함께 제공합니다. */
public record BoardResponse(
	Long id,
	String title,
	String content,
	long viewCount,
	boolean notice,
	Long authorId,
	String authorName,
	LocalDateTime createdAt,
	LocalDateTime updatedAt,
	List<CommentResponse> comments
) {
}
````

## 3. import·애노테이션·필드·메서드·키워드 설명
### 이 파일에서 실제 사용한 애노테이션


### 이 파일에 나타난 Java 키워드
- `boolean`: true/false 논리 타입입니다.
- `record`: 컴포넌트 기반 데이터 타입을 선언합니다.
- `long`: 정수 기본 타입입니다.
- `package`: 타입이 속한 네임스페이스를 선언합니다.
- `public`: 외부에서 접근할 수 있습니다.
- `import`: 다른 패키지 타입을 짧은 이름으로 사용하게 합니다.

### import별 용도
- `java.time.LocalDateTime`: 타임존 없는 날짜/시각입니다.
- `java.util.List`: 리스트 컬렉션 타입입니다.

### 필드와 컴포넌트
id,title,content,viewCount,notice,작성자 ID/이름, createdAt/updatedAt, comments.

### 애노테이션과 Java 문법
`package`는 네임스페이스입니다. `public/private/protected`는 접근 범위를 제어합니다. 아래 소스에서 실제로 사용한 애노테이션은 해당 선언에 프레임워크 동작을 연결합니다. 소스에 없는 애노테이션 동작은 가정하지 않습니다. `record`는 컴포넌트 기반 타입이며 생성자·접근자 등의 일부 메서드를 자동 생성합니다. 저장소 `interface` 구현은 Spring Data가 제공합니다.

## 4. 메서드별 목적·매개변수·순서·호출·반환·예외
### 선언된 메서드/생성자 시그니처
이 파일은 직접 선언한 메서드가 없습니다. record 접근자 또는 JpaRepository 상속 메서드는 자동 제공됩니다.

이 파일은 도메인 예외를 직접 던지는 코드가 없습니다. 프레임워크/호출자의 오류는 별도로 전파될 수 있습니다.
직접 메서드 없음. record accessor/value method 자동 생성.

직접 선언 메서드와 record 자동 생성/저장소 상속 메서드를 구분했습니다. 예외가 발생하면 이후 정상 단계는 수행되지 않습니다. 프레임워크의 바인딩·DB·검증 오류는 도메인 예외와 별도로 전파될 수 있습니다.

## 5. 확인된 end-to-end 실행 흐름
toBoardResponse가 게시글 상태와 CommentResponse 목록을 조립합니다.

## 6. 확인된 클래스 관계와 추론
확인됨: Controller 여러 응답의 반환형이며 comments는 CommentResponse 목록.

## 7. 이 파일에서 익힐 개념
상세 DTO와 중첩 리스트; 실제 JSON 시각 포맷은 설정에 좌우.

## 8. 소스와 일치하는 예시
상세는 목록 요약보다 본문과 댓글을 더 담습니다.

## 9. 핵심 요약
상세 API 계약이고 엔티티를 직접 노출하지 않습니다.



### 관련 Java 원본 바로가기
- [`BoardController.java`](../../../src/main/java/com/cleaner/be/board/BoardController.java)
- [`BoardService.java`](../../../src/main/java/com/cleaner/be/board/BoardService.java)
- [`BoardResponse.java`](../../../src/main/java/com/cleaner/be/board/dto/BoardResponse.java)
- [`CommentResponse.java`](../../../src/main/java/com/cleaner/be/board/dto/CommentResponse.java)

## 관련 상대 링크
- Java 원본: [BoardResponse.java](../../../src/main/java/com/cleaner/be/board/dto/BoardResponse.java)
- 게시판 기준: `cleaner-be/src/main/java/com/cleaner/be/board/`
- DTO 기준: `cleaner-be/src/main/java/com/cleaner/be/board/dto/`
- 관련 타입: `cleaner-be/src/main/java/com/cleaner/be/auth/Member.java`, `cleaner-be/src/main/java/com/cleaner/be/common/entity/BaseTimeEntity.java