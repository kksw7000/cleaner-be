# CommentUpdateRequest.java 학습 노트

## 1. 파일의 역할·계층·존재 이유·사용 시점
- **계층:** 요청 DTO
- **역할:** 댓글 수정 본문 계약입니다.
- **왜 필요한가:** 본문만 받고 대상/작성자는 URL과 인증 정보에서 정합니다.
- **언제 사용하는가:** PATCH /api/boards/{id}/comments/{commentId}
- **원본:** [CommentUpdateRequest.java](../../../src/main/java/com/cleaner/be/board/dto/CommentUpdateRequest.java) (문서와 같은 디렉터리).

## 2. 현재 소스 전체
아래는 현재 Java 원본 전체이며 생략한 코드는 없습니다.

````java
package com.cleaner.be.board.dto;

import jakarta.validation.constraints.NotBlank;

/** 댓글 수정 요청 본문이며 작성자와 대상 게시글은 URL 및 인증 정보로 결정합니다. */
public record CommentUpdateRequest(@NotBlank String content) {
}
````

## 3. import·애노테이션·필드·메서드·키워드 설명
### 이 파일에서 실제 사용한 애노테이션
- `@NotBlank`: null·빈 문자열·공백 문자열을 허용하지 않습니다. (적용 소스: `public record CommentUpdateRequest(@NotBlank String content) {`)

### 이 파일에 나타난 Java 키워드
- `record`: 컴포넌트 기반 데이터 타입을 선언합니다.
- `package`: 타입이 속한 네임스페이스를 선언합니다.
- `public`: 외부에서 접근할 수 있습니다.
- `import`: 다른 패키지 타입을 짧은 이름으로 사용하게 합니다.

### import별 용도
- `jakarta.validation.constraints.NotBlank`: Bean Validation 제약입니다.

### 필드와 컴포넌트
content NotBlank.

### 애노테이션과 Java 문법
`package`는 네임스페이스입니다. `public/private/protected`는 접근 범위를 제어합니다. 아래 소스에서 실제로 사용한 애노테이션은 해당 선언에 프레임워크 동작을 연결합니다. 소스에 없는 애노테이션 동작은 가정하지 않습니다. `record`는 컴포넌트 기반 타입이며 생성자·접근자 등의 일부 메서드를 자동 생성합니다. 저장소 `interface` 구현은 Spring Data가 제공합니다.

## 4. 메서드별 목적·매개변수·순서·호출·반환·예외
### 선언된 메서드/생성자 시그니처
매개변수 타입과 나열 순서는 아래 시그니처 그대로이며, 괄호 앞 타입은 반환형입니다. 생성자는 반환형이 없습니다. `void`는 값을 반환하지 않습니다.
- `public record CommentUpdateRequest(@NotBlank String content)`

이 파일은 도메인 예외를 직접 던지는 코드가 없습니다. 프레임워크/호출자의 오류는 별도로 전파될 수 있습니다.
직접 메서드 없음. record accessor. @Valid가 공백 거부.

직접 선언 메서드와 record 자동 생성/저장소 상속 메서드를 구분했습니다. 예외가 발생하면 이후 정상 단계는 수행되지 않습니다. 프레임워크의 바인딩·DB·검증 오류는 도메인 예외와 별도로 전파될 수 있습니다.

## 5. 확인된 end-to-end 실행 흐름
검증→ID/인증회원과 서비스 호출→소속 및 작성자 검사→trim/update.

## 6. 확인된 클래스 관계와 추론
확인됨: Controller 및 Service에서 사용.

## 7. 이 파일에서 익힐 개념
record, Bean Validation, URL 식별과 인증 분리.

## 8. 소스와 일치하는 예시
본문만 전달하고 작성자 권한은 로그인 회원 ID로 확인합니다.

## 9. 핵심 요약
검증된 댓글 수정 입력입니다.



### 관련 Java 원본 바로가기
- [`BoardController.java`](../../../src/main/java/com/cleaner/be/board/BoardController.java)
- [`BoardService.java`](../../../src/main/java/com/cleaner/be/board/BoardService.java)
- [`BoardResponse.java`](../../../src/main/java/com/cleaner/be/board/dto/BoardResponse.java)
- [`CommentResponse.java`](../../../src/main/java/com/cleaner/be/board/dto/CommentResponse.java)

## 관련 상대 링크
- Java 원본: [CommentUpdateRequest.java](../../../src/main/java/com/cleaner/be/board/dto/CommentUpdateRequest.java)
- 게시판 기준: `cleaner-be/src/main/java/com/cleaner/be/board/`
- DTO 기준: `cleaner-be/src/main/java/com/cleaner/be/board/dto/`
- 관련 타입: `cleaner-be/src/main/java/com/cleaner/be/auth/Member.java`, `cleaner-be/src/main/java/com/cleaner/be/common/entity/BaseTimeEntity.java