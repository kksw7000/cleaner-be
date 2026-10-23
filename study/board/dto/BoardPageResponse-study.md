# BoardPageResponse.java 학습 노트

## 1. 파일의 역할·계층·존재 이유·사용 시점
- **계층:** 응답 DTO
- **역할:** 목록과 페이지 메타데이터입니다.
- **왜 필요한가:** Spring Data Page 대신 고정 API 계약을 제공합니다.
- **언제 사용하는가:** 목록 응답 시
- **원본:** [BoardPageResponse.java](../../../src/main/java/com/cleaner/be/board/dto/BoardPageResponse.java) (문서와 같은 디렉터리).

## 2. 현재 소스 전체
아래는 현재 Java 원본 전체이며 생략한 코드는 없습니다.

````java
package com.cleaner.be.board.dto;

import java.util.List;

/** Spring Data 내부 Page 객체를 API 계약으로 노출하지 않기 위한 목록 페이징 응답입니다. */
public record BoardPageResponse(
	List<BoardListResponse> content,
	int page,
	int size,
	long totalElements,
	int totalPages,
	boolean first,
	boolean last
) {
}
````

## 3. import·애노테이션·필드·메서드·키워드 설명
### 이 파일에서 실제 사용한 애노테이션


### 이 파일에 나타난 Java 키워드
- `boolean`: true/false 논리 타입입니다.
- `record`: 컴포넌트 기반 데이터 타입을 선언합니다.
- `int`: 정수 기본 타입입니다.
- `long`: 정수 기본 타입입니다.
- `package`: 타입이 속한 네임스페이스를 선언합니다.
- `public`: 외부에서 접근할 수 있습니다.
- `import`: 다른 패키지 타입을 짧은 이름으로 사용하게 합니다.

### import별 용도
- `java.util.List`: 리스트 컬렉션 타입입니다.

### 필드와 컴포넌트
content 목록, page/size 현재 페이지, totalElements/totalPages 전체 규모, first/last 페이지 상태.

### 애노테이션과 Java 문법
`package`는 네임스페이스입니다. `public/private/protected`는 접근 범위를 제어합니다. 아래 소스에서 실제로 사용한 애노테이션은 해당 선언에 프레임워크 동작을 연결합니다. 소스에 없는 애노테이션 동작은 가정하지 않습니다. `record`는 컴포넌트 기반 타입이며 생성자·접근자 등의 일부 메서드를 자동 생성합니다. 저장소 `interface` 구현은 Spring Data가 제공합니다.

## 4. 메서드별 목적·매개변수·순서·호출·반환·예외
### 선언된 메서드/생성자 시그니처
매개변수 타입과 나열 순서는 아래 시그니처 그대로이며, 괄호 앞 타입은 반환형입니다. 생성자는 반환형이 없습니다. `void`는 값을 반환하지 않습니다.
- `public record BoardPageResponse( List<BoardListResponse> content, int page, int size, long totalElements, int totalPages, boolean first, boolean last )`

이 파일은 도메인 예외를 직접 던지는 코드가 없습니다. 프레임워크/호출자의 오류는 별도로 전파될 수 있습니다.
직접 메서드 없음. record accessor 제공.

직접 선언 메서드와 record 자동 생성/저장소 상속 메서드를 구분했습니다. 예외가 발생하면 이후 정상 단계는 수행되지 않습니다. 프레임워크의 바인딩·DB·검증 오류는 도메인 예외와 별도로 전파될 수 있습니다.

## 5. 확인된 end-to-end 실행 흐름
서비스가 Page를 목록 DTO로 map하고 메타데이터를 복사합니다.

## 6. 확인된 클래스 관계와 추론
확인됨: Controller 목록 반환형이며 content는 BoardListResponse 목록.

## 7. 이 파일에서 익힐 개념
페이지네이션 메타데이터.

## 8. 소스와 일치하는 예시
목록과 페이지 번호/크기를 함께 표시합니다.

## 9. 핵심 요약
페이지 결과와 상태를 반환합니다.



### 관련 Java 원본 바로가기
- [`BoardController.java`](../../../src/main/java/com/cleaner/be/board/BoardController.java)
- [`BoardService.java`](../../../src/main/java/com/cleaner/be/board/BoardService.java)
- [`BoardResponse.java`](../../../src/main/java/com/cleaner/be/board/dto/BoardResponse.java)
- [`CommentResponse.java`](../../../src/main/java/com/cleaner/be/board/dto/CommentResponse.java)

## 관련 상대 링크
- Java 원본: [BoardPageResponse.java](../../../src/main/java/com/cleaner/be/board/dto/BoardPageResponse.java)
- 게시판 기준: `cleaner-be/src/main/java/com/cleaner/be/board/`
- DTO 기준: `cleaner-be/src/main/java/com/cleaner/be/board/dto/`
- 관련 타입: `cleaner-be/src/main/java/com/cleaner/be/auth/Member.java`, `cleaner-be/src/main/java/com/cleaner/be/common/entity/BaseTimeEntity.java