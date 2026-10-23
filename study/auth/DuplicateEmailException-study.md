# DuplicateEmailException 학습 가이드

> 원본: [DuplicateEmailException.java](../../src/main/java/com/cleaner/be/auth/DuplicateEmailException.java)

## 1. 역할·레이어·작성 이유·사용 시점

이메일 중복으로 가입 또는 수정하려는 상황을 표현하는 도메인 예외입니다.

## 2. 전체 원본 코드

아래 코드는 현재 Java 원본을 생략 없이 그대로 수록했습니다.

```java
package com.cleaner.be.auth;

/** 이미 사용 중인 이메일의 가입 또는 프로필 변경을 거부하기 위한 도메인 예외입니다. */
public class DuplicateEmailException extends RuntimeException {
}
```

## 3. 줄별 구성과 문법 읽기

package는 인증 도메인이고 import는 없습니다. `public class`가 `RuntimeException`을 상속합니다. 필드·생성자·메서드가 없으므로 기본 생성자가 제공되고 메시지는 별도로 지정되지 않습니다.

## 4. 메서드별 목적·입력·처리 순서·호출·결과·예외

명시 메서드 없음. 기본 생성자로 생성되며 부모의 예외 기능을 상속합니다. AuthService가 중복 이메일을 확인하면 던지고 GlobalExceptionHandler가 HTTP 409로 변환합니다. 메시지는 handler에서 정합니다.

## 5. 실제 실행 흐름

이메일 조회에서 기존 계정이 발견됨→`throw`→Spring MVC 예외 handler가 409와 고정 JSON 메시지를 반환합니다.

## 6. 확인된 관계와 추론의 구분

확인: `AuthService.signup`에서 던지고 `GlobalExceptionHandler.handleDuplicateEmail`이 처리합니다. 주석에는 프로필 변경도 언급되지만 그 경로의 실제 사용은 이 클래스만으로 확정하지 않습니다.

## 7. 사용된 Java·Spring 핵심 개념

RuntimeException, 비검사 예외, 도메인 오류 타입, Spring 예외 매핑을 사용합니다.

## 8. 코드에 충실한 예시

중복 이메일이 확인되면 `new DuplicateEmailException()`으로 가입 흐름을 중단하고 handler가 409로 응답합니다.

## 9. 요약

이 클래스는 이메일 중복 실패 조건을 타입으로 식별합니다.
