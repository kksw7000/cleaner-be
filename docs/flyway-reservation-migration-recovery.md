# V5/V6 예약 마이그레이션 실패 복구 절차

이 절차는 V5 부분 고유 인덱스 생성 또는 V6 기존 고유 제약 제거가 실패했을 때, 배포를 안전하게 멈추고 실제 DB 상태를 확인한 뒤 복구하기 위한 안내입니다. 운영 DB에 접속할 때는 대상 데이터베이스와 스키마를 먼저 확인하고, DB 운영자의 승인 절차를 따릅니다. 이 문서의 SQL은 상태 확인용이며 운영 상태를 가정하지 않습니다.

## 마이그레이션 순서와 설정

- V5 `V5__allow_rebooking_inactive_reservation_dates.sql`은 `CREATE UNIQUE INDEX CONCURRENTLY`로 `uk_reservations_active_scheduled_date` 부분 고유 인덱스를 만듭니다. 조건은 `PENDING`, `APPROVED` 상태입니다.
- V6 `V6__drop_reservation_date_unique_constraint.sql`은 `uk_reservations_scheduled_date` 기존 전체 날짜 고유 제약을 제거합니다. V5와 V6가 모두 완료되어야 비활성 예약 날짜의 재예약이 가능합니다.
- `spring.flyway.postgresql.transactional-lock: false`는 Flyway가 사용하는 PostgreSQL 마이그레이션 잠금을 세션 수준으로 바꿉니다. `CREATE INDEX CONCURRENTLY` 자체의 트랜잭션 실행 여부를 제어하는 설정이 아니며, 전역 `execute-in-transaction` 설정을 끄는 대체 설정도 아닙니다.

## 실패 시 점검 및 복구

1. **배포를 중단합니다.** 자동 재시도를 멈추고, DB/스키마, Flyway 버전, 배포 로그를 확인합니다. 아래 이력 조회에서 V5/V6 상태를 파악합니다. Flyway 이력 테이블이 기본 이름이나 현재 스키마와 다르면 대상에 맞게 지정합니다.

   ```sql
   SELECT installed_rank, version, description, type, script, installed_on, success
   FROM flyway_schema_history
   WHERE script IN (
       'V5__allow_rebooking_inactive_reservation_dates.sql',
       'V6__drop_reservation_date_unique_constraint.sql'
   )
   ORDER BY installed_rank;
   ```

2. **부분 인덱스의 존재 여부와 상태를 조회합니다.** `indisvalid = false`는 인덱스를 쿼리/제약에 신뢰할 수 없는 invalid 상태라는 뜻입니다. `indisready`, 정의와 보고된 스키마도 함께 확인합니다.

   ```sql
   SELECT index_schema.nspname AS schema_name,
          table_schema.nspname AS table_schema,
          table_class.relname AS table_name,
          index_class.relname AS index_name,
          index_info.indisvalid,
          index_info.indisready,
          index_info.indislive,
          pg_get_indexdef(index_class.oid) AS index_definition
   FROM pg_index AS index_info
   JOIN pg_class AS index_class ON index_class.oid = index_info.indexrelid
   JOIN pg_namespace AS index_schema ON index_schema.oid = index_class.relnamespace
   JOIN pg_class AS table_class ON table_class.oid = index_info.indrelid
   JOIN pg_namespace AS table_schema ON table_schema.oid = table_class.relnamespace
   WHERE index_class.relname = 'uk_reservations_active_scheduled_date'
     AND table_class.relname = 'reservations'
     AND index_schema.nspname = table_schema.nspname;
   ```

   결과가 없으면 해당 이름의 인덱스를 찾지 못한 것입니다. 결과가 있으면 실제 대상 스키마, 테이블, 이름, 정의와 상태를 대조합니다. valid 인덱스가 이미 존재하면 삭제하거나 V5를 무조건 재실행하지 말고, Flyway 이력과 인덱스 정의가 어긋난 원인을 DB 운영자와 확인합니다.

3. **invalid 인덱스만 조건부로 제거합니다.** 정확히 V5가 만든 인덱스이며 상태가 invalid임을 확인한 경우에만 아래 명령을 사용합니다. `<schema_name>`은 직전 조회 결과의 `schema_name`으로 바꾸고, 특수 문자가 포함된 식별자는 PostgreSQL 규칙에 따라 큰따옴표로 감쌉니다. 인덱스 이름은 V5 SQL에 정의된 이름을 그대로 사용합니다.

   ```sql
   DROP INDEX CONCURRENTLY <schema_name>.uk_reservations_active_scheduled_date;
   ```

   이 명령은 트랜잭션 블록 안에서 실행할 수 없습니다. 명령 실행 시 사용 중인 DB 도구가 자동으로 트랜잭션을 열지 않는지 확인합니다. 인덱스가 없거나 valid이면 이 삭제 명령을 실행하지 않습니다.

4. **DB 상태를 다시 확인한 뒤에만 Flyway 이력을 정리합니다.** 인덱스가 제거됐거나 존재하지 않는지, V6 제약 상태가 무엇인지 다시 카탈로그와 Flyway 이력으로 조회합니다. 필요한 경우에만 대상 DB에서 `flyway repair`를 실행합니다. `repair`는 Flyway 이력을 정리할 뿐 invalid 인덱스나 다른 DB 객체를 삭제하지 않습니다. 따라서 `repair`만 실행하고 재시도해서는 안 됩니다. valid 인덱스가 있는데 Flyway 이력이 실패로 남은 등 스키마와 이력이 불일치하면 임의로 객체를 삭제하지 말고 DBA와 재실행 전략을 확정합니다.

5. **재시도 후 배포를 재개하기 전에 두 마이그레이션의 결과를 검증합니다.** V5 인덱스가 올바른 정의로 존재하고 `indisvalid = true`, `indisready = true`인지 확인합니다. V6에서 제거하는 기존 제약이 남아 있는지 확인하려면 다음 쿼리를 실행합니다.

   ```sql
   SELECT table_schema.nspname AS table_schema,
          table_class.relname AS table_name,
          constraint_info.conname AS constraint_name
   FROM pg_constraint AS constraint_info
   JOIN pg_class AS table_class ON table_class.oid = constraint_info.conrelid
   JOIN pg_namespace AS table_schema ON table_schema.oid = table_class.relnamespace
   WHERE table_class.relname = 'reservations'
     AND constraint_info.conname = 'uk_reservations_scheduled_date';
   ```

   V6 완료 상태에서는 해당 제약이 조회되지 않아야 합니다. V5 인덱스가 유효하고 V6 제약이 제거된 것이 확인된 후, Flyway 이력과 일치하는지 운영자가 확인하고 배포를 재개합니다. 둘 중 하나라도 확인되지 않으면 자동 재시도하지 않습니다.
