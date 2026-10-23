# BoardNotFoundException.java 학습 노트

## 1. 파일의 역할·계층·존재 이유·사용 시점
- **계층:** 도메인 예외
- **역할:** 게시글 미존재입니다.
- **왜 필요한가:** 저장소 빈 결과를 도메인 오류로 변환합니다.
- **언제 사용하는가:** BoardService.findBoard에서 조회 결과가 없을 때
- **원본:** [BoardNotFoundException.java](../../src/main/java/com/cleaner/be/board/BoardNotFoundException.java) (문서와 같은 디렉터리).

## 2. 현재 소스 전체
아래는 현재 Java 원본 전체이며 생략한 코드는 없습니다.

````java
package com.cleaner.be.board;

/** 요청한 ID의 게시글이 존재하지 않을 때 발생하는 도메인 예외입니다. */
public class BoardNotFoundException extends RuntimeException {
	public BoardNotFoundException() { super("게시글을 찾을 수 없습니다."); }
}
````

## 3. import·애노테이션·필드·메서드·키워드 설명
### 이 파일에서 실제 사용한 애노테이션


### 이 파일에 나타난 Java 키워드
- `extends`: 상속 관계를 선언합니다.
- `class`: 클래스를 선언합니다.
- `super`: 부모 클래스 부분을 가리킵니다.
- `package`: 타입이 속한 네임스페이스를 선언합니다.
- `public`: 외부에서 접근할 수 있습니다.

### import별 용도
이 파일에는 import가 없습니다.

### 필드와 컴포넌트
자체 필드 없음.

### 애노테이션과 Java 문법
`package`는 네임스페이스입니다. `public/private/protected`는 접근 범위를 제어합니다. 아래 소스에서 실제로 사용한 애노테이션은 해당 선언에 프레임워크 동작을 연결합니다. 소스에 없는 애노테이션 동작은 가정하지 않습니다. `record`는 컴포넌트 기반 타입이며 생성자·접근자 등의 일부 메서드를 자동 생성합니다. 저장소 `interface` 구현은 Spring Data가 제공합니다.

## 4. 메서드별 목적·매개변수·순서·호출·반환·예외
### 선언된 메서드/생성자 시그니처
매개변수 타입과 나열 순서는 아래 시그니처 그대로이며, 괄호 앞 타입은 반환형입니다. 생성자는 반환형이 없습니다. `void`는 값을 반환하지 않습니다.
- `public BoardNotFoundException()`

이 파일은 도메인 예외를 직접 던지는 코드가 없습니다. 프레임워크/호출자의 오류는 별도로 전파될 수 있습니다.
기본 생성자가 고정 메시지를 RuntimeException에 전달합니다.

직접 선언 메서드와 record 자동 생성/저장소 상속 메서드를 구분했습니다. 예외가 발생하면 이후 정상 단계는 수행되지 않습니다. 프레임워크의 바인딩·DB·검증 오류는 도메인 예외와 별도로 전파될 수 있습니다.

## 5. 확인된 end-to-end 실행 흐름
findById의 orElseThrow에서 발생→전역 처리기→404.

## 6. 확인된 클래스 관계와 추론
확인됨: 서비스에서 발생하고 핸들러가 댓글 부재와 함께 404 처리.

## 7. 이 파일에서 익힐 개념
Optional.orElseThrow와 도메인 예외.

## 8. 소스와 일치하는 예시
없는 게시글 ID는 이 예외로 바뀝니다.

## 9. 핵심 요약
게시글 부재를 전달하는 404 예외입니다.



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
- Java 원본: [BoardNotFoundException.java](../../src/main/java/com/cleaner/be/board/BoardNotFoundException.java)
- 게시판 기준: `cleaner-be/src/main/java/com/cleaner/be/board/`
- DTO 기준: `cleaner-be/src/main/java/com/cleaner/be/board/dto/`
- 관련 타입: `cleaner-be/src/main/java/com/cleaner/be/auth/Member.java`, `cleaner-be/src/main/java/com/cleaner/be/common/entity/BaseTimeEntity.java