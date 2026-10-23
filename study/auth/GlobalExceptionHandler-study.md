# GlobalExceptionHandler 학습 가이드

> 원본: [GlobalExceptionHandler.java](../../src/main/java/com/cleaner/be/auth/GlobalExceptionHandler.java)

## 1. 역할·레이어·작성 이유·사용 시점

Controller 전역 예외를 HTTP 오류 응답으로 변환하는 경계 계층입니다.

## 2. 전체 원본 코드

아래 코드는 현재 Java 원본을 생략 없이 그대로 수록했습니다.

```java
package com.cleaner.be.auth;

import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import com.cleaner.be.board.BoardAccessDeniedException;
import com.cleaner.be.board.BoardNotFoundException;
import com.cleaner.be.board.CommentAccessDeniedException;
import com.cleaner.be.board.CommentNotFoundException;
import com.cleaner.be.reservation.ReservationAccessDeniedException;
import com.cleaner.be.reservation.ReservationNotFoundException;
import com.cleaner.be.reservation.ReservationDateUnavailableException;

/**
 * Controller가 비즈니스 예외를 HTTP 규격으로 변환하는 공통 경계입니다.
 * 각 도메인은 HTTP 객체에 의존하지 않고 의미 있는 예외만 발생시키며, 이곳에서 일관된 오류 응답을 만듭니다.
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

	/** 이메일 중복 예외를 가입 요청의 충돌 상태와 일관된 메시지로 변환합니다. */
	@ExceptionHandler(DuplicateEmailException.class)
	@ResponseStatus(HttpStatus.CONFLICT)
	public Map<String, String> handleDuplicateEmail() {
		return Map.of("message", "중복된 이메일입니다.");
	}

	/** 전화번호 중복 예외를 가입 요청의 충돌 상태로 변환합니다. */
	@ExceptionHandler(DuplicatePhoneNumberException.class)
	@ResponseStatus(HttpStatus.CONFLICT)
	public Map<String, String> handleDuplicatePhoneNumber() {
		return Map.of("message", "이미 가입된 전화번호입니다.");
	}

	/** 자격 증명 오류를 401로 변환해 계정 존재 여부가 오류로 노출되지 않게 합니다. */
	@ExceptionHandler(InvalidCredentialsException.class)
	@ResponseStatus(HttpStatus.UNAUTHORIZED)
	public Map<String, String> handleInvalidCredentials() {
		return Map.of("message", "이메일 또는 비밀번호가 올바르지 않습니다.");
	}

	/** 현재 비밀번호 확인 실패를 입력 오류인 400 응답으로 변환합니다. */
	@ExceptionHandler(InvalidCurrentPasswordException.class)
	@ResponseStatus(HttpStatus.BAD_REQUEST)
	public Map<String, String> handleInvalidCurrentPassword() {
		return Map.of("message", "현재 비밀번호가 올바르지 않습니다.");
	}

	/** 정지된 계정의 인증 거부 사유를 403 응답으로 전달합니다. */
	@ExceptionHandler(SuspendedMemberException.class)
	@ResponseStatus(HttpStatus.FORBIDDEN)
	public Map<String, String> handleSuspendedMember(SuspendedMemberException exception) {
		return Map.of("message", exception.getMessage());
	}

	/** 게시글·댓글·예약 미존재 예외를 공통 404 응답으로 변환합니다. */
	@ExceptionHandler({BoardNotFoundException.class, CommentNotFoundException.class, ReservationNotFoundException.class})
	@ResponseStatus(HttpStatus.NOT_FOUND)
	public Map<String, String> handleNotFound(RuntimeException exception) {
		return Map.of("message", exception.getMessage());
	}

	/** 게시글·댓글·예약 권한 예외를 공통 403 응답으로 변환합니다. */
	@ExceptionHandler({BoardAccessDeniedException.class, CommentAccessDeniedException.class, ReservationAccessDeniedException.class})
	@ResponseStatus(HttpStatus.FORBIDDEN)
	public Map<String, String> handleAccessDenied(RuntimeException exception) {
		return Map.of("message", exception.getMessage());
	}

	/** 이미 선택된 날짜의 예약 요청을 409로 변환하여 클라이언트가 다른 날짜를 선택하도록 안내합니다. */
	@ExceptionHandler(ReservationDateUnavailableException.class)
	@ResponseStatus(HttpStatus.CONFLICT)
	public Map<String, String> handleReservationDateUnavailable(ReservationDateUnavailableException exception) {
		return Map.of("message", exception.getMessage());
	}
}
```

## 3. 줄별 구성과 문법 읽기

`@RestControllerAdvice`가 Controller 예외를 전역 처리하고 반환 Map을 JSON으로 직렬화합니다. `@ExceptionHandler`는 대상 예외, `@ResponseStatus`는 HTTP 코드입니다. `Map.of("message",...)`는 message 한 항목 응답입니다. 이메일/전화/예약 날짜 중복=409, 인증 실패=401, 현재 비밀번호=400, 정지/권한거부=403, 대상 없음=404입니다. 예약 날짜 예외는 도메인 메시지를 그대로 돌려줍니다. 여러 게시판/예약 예외는 하나의 handler에 배열로 등록되고 매개변수 RuntimeException의 `getMessage()`를 사용합니다.

## 4. 메서드별 목적·입력·처리 순서·호출·결과·예외

`handleDuplicateEmail`: 중복 예외→409/고정 메시지. `handleDuplicatePhoneNumber`: 전화 중복→409/메시지. `handleInvalidCredentials`: 인증 실패→401, 계정 존재 차이를 감춥니다. `handleInvalidCurrentPassword`: →400. `handleSuspendedMember(exception)`: →403 및 exception message. `handleNotFound(RuntimeException)`: 게시글/댓글/예약 미존재 예외→404. `handleAccessDenied(RuntimeException)`: 해당 권한 예외→403. `handleReservationDateUnavailable(exception)`: 날짜 중복 예외→409 및 예외 메시지. 각 메서드는 `Map<String,String>`을 반환하며 새 예외를 던지지 않습니다.

## 5. 실제 실행 흐름

Controller/service에서 예외 발생→Spring MVC가 타입에 해당하는 handler 선택→`@ResponseStatus` 코드와 JSON message 반환입니다. 예약 생성 시 사전 조회 또는 DB 고유 제약 위반에서 발생한 날짜 예약 불가 예외는 HTTP 409와 `message` JSON으로 전달됩니다. 등록되지 않은 예외는 이 클래스 밖의 기본 처리로 갈 수 있습니다.

## 6. 확인된 관계와 추론의 구분

확인: auth 도메인 예외와 게시판/예약 예외를 직접 import합니다. AuthService/OAuthService 등 서비스 예외가 여기서 HTTP로 변환됩니다. 필터의 `sendError` 응답은 필터 내부의 별도 경로입니다. 추론: 클라이언트가 공통 `message` key를 사용하도록 한 응답 규격입니다.

## 7. 사용된 Java·Spring 핵심 개념

Spring `@RestControllerAdvice`, `@ExceptionHandler`, HTTP status, RuntimeException 상속, Java Map 제네릭과 `Map.of`를 사용합니다.

## 8. 코드에 충실한 예시

중복 이메일 예외는 `{"message":"중복된 이메일입니다."}` 및 409로 매핑됩니다. 날짜 중복 예외도 `"이미 예약된 날짜입니다. 다른 날짜를 선택해주세요."`를 `message`로 반환하고 409를 사용합니다. Map JSON 필드 순서는 계약되지 않습니다.

## 9. 요약

도메인 오류를 HTTP 상태와 일관된 JSON 메시지로 변환합니다.
