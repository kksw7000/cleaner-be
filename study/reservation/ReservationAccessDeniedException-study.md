# ReservationAccessDeniedException 학습 가이드

## 1. 역할·레이어·왜·언제

`ReservationAccessDeniedException`은 예약 업무에서 회원 식별이 되지 않거나 권한이 부족할 때 사용하는 도메인 예외입니다. 서비스는 권한 실패를 이 타입으로 표현하고, 공통 예외 처리기가 HTTP 403 응답으로 변환합니다.

## 2. 현재 원본 코드

```java
package com.cleaner.be.reservation;

/** 예약 소유자가 아닌 회원이 조회하거나 관리자가 아닌 사용자가 처리할 때 발생하는 권한 예외입니다. */
public class ReservationAccessDeniedException extends RuntimeException {
	public ReservationAccessDeniedException() { super("예약에 접근할 권한이 없습니다."); }
}
```

## 3. 코드 한 줄씩

- `package`는 예약 패키지에 둡니다.
- 클래스 주석은 소유권/관리자 권한 관련 예외임을 설명합니다.
- `extends RuntimeException`은 검사되지 않는 예외로 만들어 호출 시그니처의 `throws` 선언을 강제하지 않습니다.
- 기본 생성자는 고정된 메시지를 상위 예외 클래스에 전달합니다.
- 필드, 어노테이션, 다른 import는 없습니다.

## 4. 메서드 분석

`ReservationAccessDeniedException()`은 매개변수 없이 예외 객체를 만들고 `"예약에 접근할 권한이 없습니다."` 메시지를 설정합니다. 호출 자체가 예외 객체를 반환하며, 실제 `throw`는 서비스를 비롯한 호출 지점이 수행합니다. 별도 메서드나 사용자 정의 예외 로직은 없습니다.

## 5. 실제 실행 흐름

서비스의 `findMember`가 전달된 회원 ID를 찾지 못하면 이 예외를 던집니다. 관리자 경로의 `validateAdmin`도 역할이 `ADMIN`이 아닐 때 같은 예외를 던집니다. [`GlobalExceptionHandler`](../../src/main/java/com/cleaner/be/auth/GlobalExceptionHandler.java)는 이 예외를 403 그룹으로 처리합니다. 회원별 목록은 repository 쿼리에서 해당 회원 ID로 범위를 제한합니다.

## 6. 관계: 확인된 사실과 추론

- **코드로 확인됨:** [`ReservationService.java`](../../src/main/java/com/cleaner/be/reservation/ReservationService.java)의 회원 조회 실패와 관리자 역할 실패 경로에서 사용됩니다.
- **코드로 확인됨:** [`GlobalExceptionHandler.java`](../../src/main/java/com/cleaner/be/auth/GlobalExceptionHandler.java)의 access-denied handler가 403으로 응답합니다.
- **코드로 확인됨:** 관리자 HTTP 요청의 앞단은 [`AdminReservationController.java`](../../src/main/java/com/cleaner/be/admin/AdminReservationController.java)입니다.
- **추론:** 이 예외 한 종류가 회원 소유권 오류와 관리자 권한 오류를 함께 표현하지만, 응답 메시지도 하나이므로 외부에서 두 원인을 구별하지 못합니다.

## 7. 이 파일에만 적용되는 개념

HTTP 상태 코드가 도메인 예외에 직접 들어 있지 않습니다. 이로써 서비스는 HTTP 라이브러리와 분리되고, 별도 전역 advice가 예외를 HTTP 의미로 바꿀 수 있습니다. `RuntimeException`은 체크 예외가 아닙니다.

## 8. 예시

```java
if (member.getRole() != Role.ADMIN) {
	throw new ReservationAccessDeniedException();
}
```

서비스의 관리자 권한 확인과 같은 사용 패턴입니다. 조건이 참이면 이후의 예약 조회나 상태 변경 단계로 가지 않습니다.

## 9. 요약

예약 접근 권한 실패를 표현하는 도메인 예외이며, 전역 처리기에 의해 403으로 반환됩니다.
