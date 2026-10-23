# ReservationDateUnavailableException 학습 가이드

## 1. 역할·레이어·왜·언제

`ReservationDateUnavailableException`은 이미 예약된 날짜에 중복 예약을 시도했을 때 예약 도메인에서 발생시키는 예외입니다. 구체적인 HTTP 상태 코드는 도메인 계층이 아니라 전역 예외 처리기가 결정합니다.

## 2. 현재 원본 코드

```java
package com.cleaner.be.reservation;

/** 이미 활성 예약이 있는 날짜에 중복 예약을 생성하려 할 때 409 응답으로 변환하기 위한 도메인 예외입니다. */
public class ReservationDateUnavailableException extends RuntimeException {

	/** 사용자에게 다른 날짜를 선택해야 하는 이유를 일관되게 전달합니다. */
	public ReservationDateUnavailableException() {
		super("이미 예약된 날짜입니다. 다른 날짜를 선택해주세요.");
	}
}
```

## 3. 구성과 문법

- `RuntimeException`을 상속해 기존 트랜잭션 흐름에서 별도 checked 예외 선언 없이 전파됩니다.
- 생성자는 사용자에게 전달할 고정 메시지를 상위 예외에 저장합니다.
- 예외 클래스 자체는 HTTP 응답을 만들지 않으며, [`GlobalExceptionHandler`](../auth/GlobalExceptionHandler-study.md)가 이 타입을 409로 변환합니다.

## 4. 발생 조건과 호출 결과

예약 서비스가 저장 전에 같은 날짜 예약을 발견하면 이 예외를 즉시 발생시킵니다. 사전 조회를 통과했더라도 동시에 다른 요청이 같은 날짜를 저장해 DB 고유 제약을 위반하면, 서비스가 `DataIntegrityViolationException`을 이 예외로 변환합니다. 따라서 호출자는 두 경우 모두 같은 도메인 오류와 메시지를 받습니다.

## 5. 실제 실행 흐름

예약 생성 서비스에서 날짜 존재 확인 또는 DB unique constraint 위반→`ReservationDateUnavailableException` 발생→Spring MVC의 [`GlobalExceptionHandler`](../auth/GlobalExceptionHandler-study.md)가 HTTP 409와 `{"message":"이미 예약된 날짜입니다. 다른 날짜를 선택해주세요."}` 형태로 반환합니다.

## 6. 관계: 확인된 사실과 추론

- **코드로 확인됨:** 직접 발생은 예약 서비스의 사전 조회 실패와 DB 제약 위반 변환에서 이뤄집니다.
- **코드로 확인됨:** 전역 예외 처리기는 해당 예외를 409 Conflict로 매핑합니다.
- **추론:** 클라이언트가 이 오류를 받았을 때 화면에서 날짜를 다시 불러올지는 클라이언트 구현에 달려 있습니다.

## 7. 이 파일에만 적용되는 개념

도메인 예외는 실패 이유를 타입으로 표현해 호출 계층이 오류를 구분하게 합니다. 동시 요청은 애플리케이션의 존재 확인만으로 막을 수 없으므로 DB 고유 제약을 최종 보장 수단으로 함께 둡니다.

## 8. 예시

같은 날짜의 예약 생성 요청이 이미 처리된 뒤 동일 날짜를 다시 제출하면 이 예외가 발생하고 API는 충돌 상태를 반환합니다.

## 9. 요약

날짜 예약 불가 상황을 일관된 도메인 예외로 나타내며, 전역 처리기를 통해 HTTP 409로 전달됩니다.
