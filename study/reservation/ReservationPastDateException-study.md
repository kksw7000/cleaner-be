# ReservationPastDateException 학습 가이드

## 1. 역할과 사용 시점

`ReservationPastDateException`은 대한민국 날짜 기준으로 오늘보다 앞선 날에 예약을 요청했을 때 발생하는 예약 도메인 예외입니다. 프론트엔드 검증을 우회한 요청도 예약 서비스에서 차단하며, 예외 자체는 HTTP 규격에 의존하지 않습니다.

## 2. 처리 흐름

1. 예약 생성 요청의 `scheduledAt`에서 달력 날짜를 추출합니다. PostgreSQL의 `scheduled_at`은 시간대가 없는 `timestamp`이므로 이 프로젝트에서는 입력값을 대한민국 현지 날짜와 시간으로 취급합니다.
2. 서비스가 `Clock`을 `Asia/Seoul`로 고정해 오늘 날짜를 구합니다.
3. 예약 날짜가 오늘보다 이전일 때 이 예외를 발생시킵니다. 오늘 날짜는 시각과 관계없이 허용합니다.
4. `GlobalExceptionHandler`가 예외를 HTTP 400과 `{"message":"지난 날짜는 예약할 수 없습니다."}` 응답으로 바꿉니다.

## 3. 날짜 정책과 제약

요청 DTO에는 `@Future`를 두지 않습니다. 해당 검증은 서버 기본 시간대와 시각을 기준으로 오늘 중 과거 시각도 거부할 수 있지만, 예약 정책은 오늘 날짜 전체를 허용하기 때문입니다. DB 날짜 키는 `LocalDateTime.toLocalDate()`에서 생성되므로 같은 달력 날짜의 시간대별 슬롯은 별도로 나뉘지 않습니다.
