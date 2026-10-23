# ReservationNotFoundException 학습 가이드

## 1. 역할·레이어·왜·언제

`ReservationNotFoundException`은 요청한 예약 ID가 없을 때 서비스가 발생시키는 도메인 예외입니다. 예약 계층은 상황을 의미하는 예외만 던지고 공통 API 경계가 이를 HTTP 404로 변환합니다.

## 2. 현재 원본 코드

```java
package com.cleaner.be.reservation;

/** 요청한 예약 식별자가 없을 때 API 경계에서 404로 변환할 도메인 예외입니다. */
public class ReservationNotFoundException extends RuntimeException {
	public ReservationNotFoundException() { super("예약을 찾을 수 없습니다."); }
}
```

## 3. 코드 한 줄씩

- `package`는 예약 도메인 소속을 지정합니다.
- 클래스 주석은 예외의 의미와 HTTP 경계에서의 변환 의도를 설명합니다.
- `extends RuntimeException`은 unchecked 예외로 선언해 호출 메서드가 `throws`로 명시하지 않아도 발생시킬 수 있게 합니다.
- 기본 생성자는 상위 클래스에 한국어 메시지를 전달합니다.
- 이 파일에는 다른 import, 필드, 어노테이션이 없습니다.

## 4. 메서드 분석

`ReservationNotFoundException()`은 인자를 받지 않고 고정 메시지로 예외 객체를 생성합니다. 반환값은 생성된 예외 객체이며, 여기서 던지는 코드는 아닙니다. 서비스가 `orElseThrow`의 예외 공급자로 사용합니다. 상위 `RuntimeException` 생성 동작 외 별도 호출은 없습니다.

## 5. 실제 실행 흐름

서비스의 `findReservation`이 `ReservationRepository.findById` 결과가 비었을 때 이 예외를 생성하고 던집니다. [`GlobalExceptionHandler`](../../src/main/java/com/cleaner/be/auth/GlobalExceptionHandler.java)의 not-found handler가 이 예외를 처리해 404와 메시지 map을 반환합니다. HTTP 변환은 예약 예외 클래스가 아닌 전역 advice에서 이뤄집니다.

## 6. 관계: 확인된 사실과 추론

- **코드로 확인됨:** [`ReservationService.java`](../../src/main/java/com/cleaner/be/reservation/ReservationService.java)에서 예약 ID 조회 실패 시 발생합니다.
- **코드로 확인됨:** 전역 예외 처리기에서 `@ResponseStatus(HttpStatus.NOT_FOUND)` 그룹에 포함됩니다.
- **코드로 확인됨:** [`ReservationRepository.java`](../../src/main/java/com/cleaner/be/reservation/ReservationRepository.java)는 `JpaRepository`의 `findById`를 상속합니다.
- **추론:** 예외 메시지와 응답 JSON의 구체적인 직렬화 형식은 ControllerAdvice 및 Spring 설정과 함께 결정됩니다.

## 7. 이 파일에만 적용되는 개념

도메인 예외는 업무상 실패 원인을 표현합니다. RuntimeException 기반은 체크 예외 선언을 강제하지 않지만, 예상된 실패를 어느 계층에서 변환하는지 명확히 하는 것이 중요합니다.

## 8. 예시

```java
reservationRepository.findById(id)
	.orElseThrow(ReservationNotFoundException::new);
```

서비스에 있는 실제 사용 방식과 같은 형태입니다. ID가 없으면 예외가 던져지고 정상 반환 경로는 이어지지 않습니다.

## 9. 요약

예약이 없음을 나타내는 unchecked 도메인 예외입니다. 서비스에서 발생하며 전역 처리기가 404로 바꿉니다.
