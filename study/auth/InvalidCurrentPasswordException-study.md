# InvalidCurrentPasswordException 학습 가이드

> 원본: [InvalidCurrentPasswordException.java](../../src/main/java/com/cleaner/be/auth/InvalidCurrentPasswordException.java)

## 1. 역할·레이어·작성 이유·사용 시점

비밀번호 변경 중 기존 비밀번호 확인 실패를 나타내는 도메인 예외입니다.

## 2. 전체 원본 코드

아래 코드는 현재 Java 원본을 생략 없이 그대로 수록했습니다.

```java
package com.cleaner.be.auth;

/** 현재 비밀번호 확인에 실패했을 때 비밀번호 변경 요청을 중단합니다. */
public class InvalidCurrentPasswordException extends RuntimeException {
}
```

## 3. 줄별 구성과 문법 읽기

`RuntimeException`을 상속하며 import·필드·생성자·메서드가 없습니다. 응답 메시지는 예외에 담지 않고 GlobalExceptionHandler가 정합니다.

## 4. 메서드별 목적·입력·처리 순서·호출·결과·예외

명시 메서드 없음. `MyPageService`의 현재 비밀번호 검증 실패 경로가 던지고 `GlobalExceptionHandler`가 400으로 변환합니다. 성공 시 별도 경로에서 저장된 해시가 변경됩니다.

## 5. 실제 실행 흐름

비밀번호 변경 요청→현재 비밀번호 비교 실패→예외로 변경 중단→400 응답입니다.

## 6. 확인된 관계와 추론의 구분

확인: `mypage/MyPageService.java`가 던지고 `GlobalExceptionHandler.handleInvalidCurrentPassword`가 처리합니다. `Member.updatePassword`는 별도 성공 경로입니다.

## 7. 사용된 Java·Spring 핵심 개념

Java RuntimeException, 도메인 오류 구분, HTTP 400 응답 처리를 사용합니다.

## 8. 코드에 충실한 예시

현재 비밀번호가 틀리면 저장된 해시는 변경되지 않고 클라이언트에 400 및 오류 메시지가 반환됩니다.

## 9. 요약

현재 비밀번호 재확인 실패를 명확하게 표현하는 예외입니다.
