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
import com.cleaner.be.reservation.ReservationYearOutOfRangeException;

import jakarta.validation.ConstraintViolationException;

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

	/** HTTP 파라미터 검증 실패를 400으로 변환해 허용 범위를 벗어난 요청이 서버 오류로 보이지 않게 합니다. */
	@ExceptionHandler(ConstraintViolationException.class)
	@ResponseStatus(HttpStatus.BAD_REQUEST)
	public Map<String, String> handleConstraintViolation() {
		return Map.of("message", "요청 파라미터가 허용 범위를 벗어났습니다.");
	}

	/** 예약 조회 연도 범위 오류를 400으로 변환해 클라이언트가 허용 범위를 안내받도록 합니다. */
	@ExceptionHandler(ReservationYearOutOfRangeException.class)
	@ResponseStatus(HttpStatus.BAD_REQUEST)
	public Map<String, String> handleReservationYearOutOfRange(ReservationYearOutOfRangeException exception) {
		return Map.of("message", exception.getMessage());
	}
}
