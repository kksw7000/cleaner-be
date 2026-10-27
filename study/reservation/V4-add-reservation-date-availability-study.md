# V4 예약 날짜 가용성 migration 학습 가이드

## 1. 역할·왜·언제

[`V4__add_reservation_date_availability.sql`](../../src/main/resources/db/migration/V4__add_reservation_date_availability.sql)은 예약 시각에서 날짜 키를 분리하고 날짜별 중복 예약을 DB 차원에서 차단합니다. 예약 달력의 월 범위 조회도 날짜 인덱스로 지원합니다.

## 2. 원본 migration

```sql
ALTER TABLE reservations ADD COLUMN scheduled_date date;
UPDATE reservations SET scheduled_date = CAST(scheduled_at AS date);
ALTER TABLE reservations ALTER COLUMN scheduled_date SET NOT NULL;
-- migration 시작 전에 운영자가 아래 진단 쿼리를 별도로 실행하고, 중복이 있으면 수동 정리합니다.
-- SELECT CAST(scheduled_at AS date) AS scheduled_date, COUNT(*) AS reservation_count
-- FROM reservations
-- GROUP BY CAST(scheduled_at AS date)
-- HAVING COUNT(*) > 1;
ALTER TABLE reservations ADD CONSTRAINT uk_reservations_scheduled_date UNIQUE (scheduled_date);
CREATE INDEX idx_reservations_scheduled_date_status ON reservations (scheduled_date, status);
```

위 SQL은 동작을 설명하기 위해 주석을 제외한 migration 문장만 간추려 옮겼습니다.

## 3. 실행 순서

1. `scheduled_date`를 우선 nullable 컬럼으로 추가합니다.
2. 기존 행의 `scheduled_at`에서 날짜를 계산해 채워 기존 예약 데이터를 보존합니다.
3. migration을 적용하기 전에 `scheduled_at`의 날짜별 예약 수 진단 쿼리를 실행합니다.
4. 중복이 발견되면 예약 내용과 상태를 확인해 업무 기준에 따라 사람이 수동 정리해야 하며, migration은 예약 행을 삭제하거나 자동 변경하지 않습니다. 중복을 해결한 뒤 migration을 실행합니다.
5. `NOT NULL`과 날짜 unique constraint를 적용해 전체 테이블에서 날짜당 하나의 예약만 허용합니다. 사전 확인을 놓쳐 중복이 남아 있으면 unique constraint 적용이 실패합니다.
6. 월별 날짜 조회가 범위 조건을 효율적으로 사용할 수 있도록 예약 날짜 및 상태 복합 인덱스를 생성합니다.

## 4. 코드와의 연결

[`Reservation`](../../src/main/java/com/cleaner/be/reservation/Reservation.java)은 `scheduledAt.toLocalDate()`로 날짜 값을 채웁니다. [`ReservationRepository`](../../src/main/java/com/cleaner/be/reservation/ReservationRepository.java)는 해당 컬럼으로 월 범위 날짜를 조회하고 존재 여부를 확인합니다. 서비스는 unique constraint 위반을 예약 불가 예외로 바꿉니다.

## 5. 기존 데이터와 제약

기존 데이터를 보존하려면 migration 적용 전에 아래 진단 쿼리를 실행해야 합니다. 중복이 있으면 예약 정보를 살펴보고 업무 기준에 맞춰 수동으로 정리한 뒤 migration을 적용합니다. 이 migration에는 예약 행을 자동 삭제하거나 임의로 선택하는 로직이 없습니다. 중복을 놓치더라도 DB의 unique constraint가 적용 단계에서 실패하므로, 중복 날짜를 해결하기 전에는 제약이 생성되지 않습니다.

```sql
SELECT CAST(scheduled_at AS date) AS scheduled_date, COUNT(*) AS reservation_count
FROM reservations
GROUP BY CAST(scheduled_at AS date)
HAVING COUNT(*) > 1;
```

## 6. 인덱스와 주의점

인덱스는 `scheduled_date`와 `status` 순서로 구성됩니다. 현재 repository의 월별 조회는 `scheduled_date` 범위 조건을 사용하며, 실제 실행 계획과 인덱스 활용은 DB 엔진이 결정합니다. 현재 JPQL 날짜 조회에는 상태 조건이 없으므로 이 인덱스의 두 번째 컬럼이 모든 쿼리에 사용된다고 단정하지 않습니다.

## 7. V5/V6에서 활성 예약만 날짜를 점유하도록 변경

[`V5__allow_rebooking_inactive_reservation_dates.sql`](../../src/main/resources/db/migration/V5__allow_rebooking_inactive_reservation_dates.sql)은 `CREATE UNIQUE INDEX CONCURRENTLY`로 `uk_reservations_active_scheduled_date` 부분 고유 인덱스를 추가합니다. 인덱스 조건은 `PENDING`, `APPROVED`만 포함하므로 이 인덱스 자체는 `REJECTED`, `CANCELLED`, 기존 호환 상태인 `CANCELED`, `COMPLETED` 상태를 제외합니다. 다만 V5가 적용된 직후에는 V4의 전체 날짜 고유 제약 `uk_reservations_scheduled_date`가 아직 남아 있어 비활성 예약 날짜의 재예약은 계속 제한됩니다.

[`V6__drop_reservation_date_unique_constraint.sql`](../../src/main/resources/db/migration/V6__drop_reservation_date_unique_constraint.sql)은 `ALTER TABLE reservations DROP CONSTRAINT uk_reservations_scheduled_date`로 V4의 전체 날짜 고유 제약을 제거합니다. 따라서 V5 부분 고유 인덱스 생성과 V6 제약 제거가 모두 완료된 뒤에야 취소·거절된 예약 날짜를 다시 예약할 수 있는 구조가 완성됩니다.

서비스 조회도 같은 활성 상태만 대상으로 하며, 관리자 상태 변경으로 예약을 다시 활성화할 때 기존 날짜 예약을 먼저 확인합니다. 조회 후 상태 변경과 동시에 다른 요청이 진행되더라도 부분 고유 인덱스가 최종 중복을 차단합니다. 생성 및 상태 변경은 해당 인덱스 위반을 HTTP 409 날짜 마감 응답으로 변환합니다.

## 8. 마이그레이션 실패 시 확인 순서

V5의 `CREATE INDEX CONCURRENTLY`는 트랜잭션 안에서 실행할 수 없습니다. 프로젝트 설정의 `spring.flyway.postgresql.transactional-lock: false`는 Flyway의 PostgreSQL 마이그레이션 잠금을 세션 수준 잠금으로 바꾸며, SQL 자체의 트랜잭션 실행 설정을 끄는 것은 아닙니다. 실패한 인덱스가 남을 수 있으므로 배포를 중단하고 Flyway 실패 이력과 인덱스의 유효 상태를 먼저 확인해야 합니다.

1. `flyway info`와 운영용 실패 복구 문서의 이력 조회 SQL로 V5/V6 상태를 확인합니다.
2. PostgreSQL 카탈로그에서 `uk_reservations_active_scheduled_date`가 존재하는지와 `indisvalid`, `indisready`, 정의를 확인합니다.
3. 해당 인덱스가 실제로 invalid인 경우에만, 확인한 스키마와 정확한 인덱스 이름을 사용해 트랜잭션 밖에서 `DROP INDEX CONCURRENTLY`로 제거합니다. 유효한 인덱스는 임의로 삭제하거나 재시도하지 않습니다.
4. DB 상태를 다시 확인하고 필요한 경우에만 `flyway repair`로 실패 이력을 정리합니다. `repair`는 invalid 인덱스를 삭제하지 않습니다.
5. 확인 SQL과 분기별 조치는 [Flyway 예약 마이그레이션 실패 복구 절차](../../docs/flyway-reservation-migration-recovery.md)를 따릅니다. 필요한 마이그레이션만 복구 후 재시도하고, V5 인덱스가 유효하며 V6의 기존 고유 제약이 제거되고 Flyway 이력과 일치하는 것을 확인한 뒤 배포를 재개합니다.

## 9. 요약

예약 날짜 컬럼을 기존 데이터에서 채우고 필수·고유 제약 및 복합 인덱스를 추가하여 날짜별 중복 방지와 달력 조회를 DB 스키마에 반영합니다.
