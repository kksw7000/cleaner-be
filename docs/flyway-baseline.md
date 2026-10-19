# 기존 DB Flyway Baseline 절차

## 기준 버전

기존 DB의 기준 버전은 `3`으로 정합니다. 이 버전은 `V0_1` 회원 테이블, `V1`의 회원 권한과 게시글·댓글 테이블, `V2`의 회원 정지 여부, `V3` 예약 테이블까지 모두 반영된 현재 스키마를 의미합니다.

`spring.flyway.baseline-on-migrate`는 `false`로 유지합니다. 새 DB는 모든 migration을 실행하고, 기존 DB는 아래 사전 점검과 일회성 baseline을 배포 전에 완료해야 합니다. baseline은 스키마를 생성하거나 수정하지 않고 Flyway 이력만 기록합니다.

## 배포 전 절차

1. 대상 DB와 스키마를 확인하고 백업합니다. `flyway_schema_history`가 이미 있으면 baseline하지 말고 Flyway 이력을 점검합니다.
2. 이력이 없는 기존 DB라면 실제 스키마가 `V3` 결과와 일치하는지 확인합니다. 최소한 `members`의 `role`, `suspended`, `boards`, `comments`, `reservations`와 각 migration에 정의된 제약 조건 및 인덱스가 모두 있어야 합니다. JPA의 `ddl-auto: validate`는 테이블을 생성하지 않으므로, 테이블 존재를 추정 근거로 사용하지 않습니다.
3. DB가 `V3`와 정확히 일치하는 경우에만 Flyway CLI를 프로젝트 루트에서 실행합니다. 연결 인자는 배포 환경의 `DB_URL`, `DB_USERNAME`, `DB_PASSWORD`에 해당하는 값을 사용하고, 비밀번호를 문서나 명령 이력에 직접 기록하지 않습니다.

```text
flyway -url=<DB_URL> -user=<DB_USERNAME> -password=<DB_PASSWORD> -locations=filesystem:src/main/resources/db/migration -baselineVersion=3 -baselineDescription="existing schema at V3" baseline
```

4. `flyway info`로 기준 버전 `3`이 기록됐는지 확인한 뒤 애플리케이션을 배포합니다. 이 일회성 baseline은 대상 DB마다 한 번만 수행합니다.

## 예외 처리

- 빈 DB에는 baseline 명령을 실행하지 않습니다. 애플리케이션이 `V0_1`부터 `V3`까지 순서대로 적용합니다.
- 기존 DB가 `V3`와 다르면 버전 `3`으로 baseline하지 않습니다. 누락된 migration을 적용하거나 스키마를 먼저 정합화한 뒤 다시 확인합니다. 일부 테이블만 있다는 이유로 기준 버전을 추정하지 않습니다.
- 기존 DB에 Flyway 이력이 이미 있으면 baseline하지 않습니다. 이력과 실제 스키마 불일치 원인을 먼저 해결합니다.