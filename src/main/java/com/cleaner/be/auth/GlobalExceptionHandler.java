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

/**
 * Controller가 비즈니스 예외를 HTTP 규격으로 변환하는 공통 경계입니다.
 * 각 도메인은 HTTP 객체에 의존하지 않고 의미 있는 예외만 발생시키며, 이곳에서 일관된 오류 응답을 만듭니다.
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

	@ExceptionHandler(DuplicateEmailException.class)
	@ResponseStatus(HttpStatus.CONFLICT)
	public Map<String, String> handleDuplicateEmail() {
		return Map.of("message", "중복된 이메일입니다.");
	}

	@ExceptionHandler(DuplicatePhoneNumberException.class)
	@ResponseStatus(HttpStatus.CONFLICT)
	public Map<String, String> handleDuplicatePhoneNumber() {
		return Map.of("message", "이미 가입된 전화번호입니다.");
	}

	@ExceptionHandler(InvalidCredentialsException.class)
	@ResponseStatus(HttpStatus.UNAUTHORIZED)
	public Map<String, String> handleInvalidCredentials() {
		return Map.of("message", "이메일 또는 비밀번호가 올바르지 않습니다.");
	}

	@ExceptionHandler(InvalidCurrentPasswordException.class)
	@ResponseStatus(HttpStatus.BAD_REQUEST)
	public Map<String, String> handleInvalidCurrentPassword() {
		return Map.of("message", "현재 비밀번호가 올바르지 않습니다.");
	}

	@ExceptionHandler(SuspendedMemberException.class)
	@ResponseStatus(HttpStatus.FORBIDDEN)
	public Map<String, String> handleSuspendedMember(SuspendedMemberException exception) {
		return Map.of("message", exception.getMessage());
	}

	@ExceptionHandler({BoardNotFoundException.class, CommentNotFoundException.class})
	@ResponseStatus(HttpStatus.NOT_FOUND)
	public Map<String, String> handleNotFound(RuntimeException exception) {
		return Map.of("message", exception.getMessage());
	}

	@ExceptionHandler({BoardAccessDeniedException.class, CommentAccessDeniedException.class})
	@ResponseStatus(HttpStatus.FORBIDDEN)
	public Map<String, String> handleAccessDenied(RuntimeException exception) {
		return Map.of("message", exception.getMessage());
	}
}
