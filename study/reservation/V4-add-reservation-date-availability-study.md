# V4 예약 날짜 가용성 migration 학습 가이드

## 1. 역할·왜·언제

[`V4__add_reservation_date_availability.sql`](../../src/main/resources/db/migration/V4__add_reservation_date_availability.sql)은 예약 시각에서 날짜 키를 분리하고 날짜별 중복 예약을 DB 차원에서 차단합니다. 예약 달력의 월 범위 조회도 날짜 인덱스로 지원합니다.

## 2. 원본 migration

```sql
ALTER TABLE reservations ADD COLUMN scheduled_date date;
UPDATE reservations SET scheduled_date = CAST(scheduled_at AS date);
ALTER TABLE reservations ALTER COLUMN scheduled_date SET NOT NULL;
ALTER TABLE reservations ADD CONSTRAINT uk_reservations_scheduled_date UNIQUE (scheduled_date);
CREATE INDEX idx_reservations_scheduled_date_status ON reservations (scheduled_date, status);
```

위 SQL은 동작을 설명하기 위해 주석을 제외한 migration 문장만 간추려 옮겼습니다.

## 3. 실행 순서

1. `scheduled_date`를 우선 nullable 컬럼으로 추가합니다.
2. 기존 행의 `scheduled_at`에서 날짜를 계산해 채워 기존 예약 데이터를 보존합니다.
3. 값이 채워진 뒤 컬럼을 `NOT NULL`로 바꿉니다.
4. 날짜 unique constraint로 전체 테이블에서 날짜당 하나의 예약만 허용합니다.
5. 월별 날짜 조회가 범위 조건을 효율적으로 사용할 수 있도록 예약 날짜 및 상태 복합 인덱스를 생성합니다.

## 4. 코드와의 연결

[`Reservation`](../../src/main/java/com/cleaner/be/reservation/Reservation.java)은 `scheduledAt.toLocalDate()`로 날짜 값을 채웁니다. [`ReservationRepository`](../../src/main/java/com/cleaner/be/reservation/ReservationRepository.java)는 해당 컬럼으로 월 범위 날짜를 조회하고 존재 여부를 확인합니다. 서비스는 unique constraint 위반을 예약 불가 예외로 바꿉니다.

## 5. 기존 데이터와 제약

기존 행을 먼저 채운 다음 `NOT NULL`과 unique constraint를 적용해 null 데이터가 남지 않도록 합니다. 이 migration은 예약 시각의 날짜를 기준으로 변환하므로, 같은 날짜에 이미 여러 예약이 있는 DB에서는 unique constraint 생성 단계가 실패할 수 있습니다. 적용 전 기존 중복 날짜 여부를 확인해야 합니다.

## 6. 인덱스와 주의점

인덱스는 `scheduled_date`와 `status` 순서로 구성됩니다. 현재 repository의 월별 조회는 `scheduled_date` 범위 조건을 사용하며, 실제 실행 계획과 인덱스 활용은 DB 엔진이 결정합니다. 현재 JPQL 날짜 조회에는 상태 조건이 없으므로 이 인덱스의 두 번째 컬럼이 모든 쿼리에 사용된다고 단정하지 않습니다.

## 7. 요약

예약 날짜 컬럼을 기존 데이터에서 채우고 필수·고유 제약 및 복합 인덱스를 추가하여 날짜별 중복 방지와 달력 조회를 DB 스키마에 반영합니다.
