# ReservationService 제약 위반 테스트 학습 가이드

## 1. 역할·레이어·왜·언제

`ReservationServiceConstraintTest`는 예약 저장 중 발생한 DB 제약 위반을 예약 서비스가 올바르게 구분하는지 단위 테스트합니다. 날짜 중복은 예약 불가 도메인 예외로 변환하고, 회원 외래 키 등 무관한 제약 오류는 원래 예외를 보존해야 합니다.

## 2. 테스트 시나리오

- `uk_reservations_scheduled_date` 위반을 포함한 `DataIntegrityViolationException`은 `ReservationDateUnavailableException`으로 변환되어야 합니다.
- `fk_reservations_member_id`처럼 다른 제약 이름의 예외는 전달받은 `DataIntegrityViolationException` 인스턴스 그대로 다시 던져야 합니다.

## 3. 테스트 구성과 실행 흐름

1. `ReservationRepository`와 `MemberRepository`를 Mockito mock으로 준비합니다.
2. 회원 조회는 성공하고 날짜 사전 중복은 없도록 설정해 저장 시점의 제약 위반 처리에 집중합니다.
3. `saveAndFlush`가 특정 제약 이름을 가진 Hibernate `ConstraintViolationException`을 원인으로 포함하는 Spring `DataIntegrityViolationException`을 던지도록 구성합니다.
4. 예약 생성 메서드를 호출해 날짜 고유 제약 변환과 기타 제약 예외의 동일 인스턴스 전파를 각각 검증합니다.

## 4. 핵심 테스트 문법

- `when(...).thenReturn(...)`과 `thenThrow(...)`는 repository mock의 응답을 정합니다.
- `assertThatThrownBy`는 실행한 서비스 호출의 예외 타입 또는 동일 인스턴스를 검증합니다.
- Hibernate 예외에 DB 제약 이름을 넣어 서비스가 원인 체인을 검사하는 실제 조건을 재현합니다.

## 5. 실행

저장소 루트에서 다음 명령으로 해당 테스트만 실행할 수 있습니다.

```powershell
.\gradlew.bat test --tests com.cleaner.be.reservation.ReservationServiceConstraintTest
```

## 6. 요약

이 테스트는 같은 Spring 무결성 오류 계열이라도 예약 날짜 고유 제약만 도메인 오류로 변환하고 나머지는 그대로 전파하는 회귀를 방지합니다.
