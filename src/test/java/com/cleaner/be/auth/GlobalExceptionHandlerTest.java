package com.cleaner.be.auth;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.core.MethodParameter;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;

import com.cleaner.be.auth.dto.SignupRequest;

class GlobalExceptionHandlerTest {

	@Test
	void validationFailureReturnsTheFieldReason() throws NoSuchMethodException {
		BeanPropertyBindingResult bindingResult = new BeanPropertyBindingResult(new SignupRequest(
			"member@example.com", "password", "사용자", "010-1234-5678"), "signupRequest");
		bindingResult.addError(new FieldError("signupRequest", "password", "비밀번호에는 특수문자를 최소 1개 이상 포함해야 합니다."));
		MethodParameter parameter = new MethodParameter(
			AuthController.class.getMethod("signup", SignupRequest.class), 0);
		MethodArgumentNotValidException exception = new MethodArgumentNotValidException(parameter, bindingResult);

		assertThat(new GlobalExceptionHandler().handleInvalidRequestBody(exception))
			.containsEntry("message", "비밀번호에는 특수문자를 최소 1개 이상 포함해야 합니다.");
	}
}
