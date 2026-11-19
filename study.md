# Cleaner 백엔드 학습 가이드

이 문서는 프로젝트의 `study/` 하위 문서와 현재 백엔드 코드·설정을 바탕으로 전체 구조를 한 흐름으로 정리합니다. 개별 클래스의 세부 설명은 `study/` 안의 파일을 참고하고, 이 문서는 요청이 들어와 데이터가 저장되고 응답되는 과정을 중심으로 읽습니다.

## 1. 공부 순서

1. 애플리케이션 시작과 환경 설정을 읽습니다.
2. Controller → Service → Repository → Entity의 요청 흐름을 따라갑니다.
3. 회원가입·로그인과 JWT 인증을 공부합니다.
4. 게시판과 예약에서 검증·권한·트랜잭션을 비교합니다.
5. 예약 상태 변경과 알림 저장·SSE 전달을 따라갑니다.
6. Flyway 마이그레이션과 테스트로 DB 및 동작을 확인합니다.

처음에는 기능을 하나 고른 뒤 Controller에서 시작해 Service, Repository, Entity, DTO, 예외 처리 순으로 파일을 열어보세요. 각 단계에서 “이 계층이 책임지는 일은 무엇인가?”를 스스로 설명하면 구조가 더 잘 보입니다.

## 2. 프로젝트와 실행 환경

- Java toolchain: 17
- 빌드·실행: Gradle Wrapper
- 웹 계층: Spring Boot Web MVC
- 데이터 접근: Spring Data JPA / Hibernate
- 인증·인가: Spring Security와 JWT
- 입력 검증: Jakarta Validation
- DB 변경 이력: Flyway
- 운영 DB 드라이버: PostgreSQL
- 테스트: JUnit 기반 Spring Boot Test, 테스트용 H2

주요 시작 설정은 `src/main/java/com/cleaner/be/BeApplication.java`와 `src/main/resources/application.yaml`에 있습니다. `@SpringBootApplication`이 앱 설정과 컴포넌트 검색의 시작점이며, JPA auditing을 활성화해 공통 시간 필드가 기록되도록 합니다.

### 로컬 실행 전 확인

`application.yaml`은 DB와 JWT 설정을 환경 변수로 받습니다. 실행 환경에 다음 값이 준비되어 있어야 합니다.

- `DB_URL`, `DB_USERNAME`, `DB_PASSWORD`
- `JWT_SECRET`: Base64 형식이며 디코딩 결과가 최소 32바이트여야 합니다.
- `JWT_REFRESH_COOKIE_SECURE`, `JWT_REFRESH_COOKIE_SAME_SITE`

설정 파일은 루트 또는 백엔드 디렉터리의 선택적 `.env` 파일을 가져올 수 있습니다. 실제 비밀번호나 토큰 값을 문서·명령 기록·Git에 넣지 마세요. 프로젝트 루트에서 백엔드를 실행할 때와 `cleaner-be` 안에서 실행할 때는 `.env` 탐색 경로가 다를 수 있으니 설정 파일의 `spring.config.import`를 확인합니다.

```powershell
cd cleaner-be
.\gradlew.bat test
.\gradlew.bat bootRun
```

개발 환경에 따라 DB 연결 정보와 JWT 환경 변수가 먼저 필요합니다. `spring.jpa.hibernate.ddl-auto: validate`는 스키마를 생성하지 않고 현재 엔티티와 DB 스키마가 맞는지 검사합니다. 새 DB의 테이블 생성은 Flyway migration이 담당합니다.

## 3. 백엔드의 계층 구조

```text
HTTP 요청
  → Security Filter Chain / JWT 인증
  → Controller: HTTP 입력·출력, 요청 경계 검증
  → Service: 업무 규칙, 권한 확인, 트랜잭션
  → Repository: DB 조회·저장
  → Entity: 영속 데이터와 상태
  → DTO: API 입력·출력 형식
HTTP 응답
```

### 계층별 책임

- **Controller**: URL, HTTP 메서드, 요청 본문·파라미터를 다룹니다. 입력 DTO의 `@Valid` 검증을 요청하고, 인증 회원 ID를 읽어 Service에 전달합니다.
- **Service**: 여러 Repository 작업을 묶고 업무 규칙·권한·트랜잭션을 적용합니다. 핵심 규칙은 HTTP 응답 형식과 분리합니다.
- **Repository**: `JpaRepository` 기반으로 DB 작업을 수행합니다. 메서드 이름으로 조건을 표현하거나 필요한 쿼리를 정의합니다.
- **Entity**: DB 테이블과 연결된 객체입니다. 상태 변경 메서드로 도메인 데이터를 변경합니다.
- **DTO**: 외부 API에서 받거나 반환할 데이터 모양입니다. Entity를 그대로 외부에 노출하지 않아 내부 DB 구조와 API 계약을 분리합니다.
- **예외**: 도메인에서 의미 있는 오류를 발생시키고, 공통 예외 처리기에서 HTTP 상태와 메시지로 변환합니다.

## 4. 요청 하나가 처리되는 과정

예를 들어 로그인한 회원이 게시글을 작성하면 다음 순서로 처리됩니다.

1. 요청이 Spring Security Filter Chain에 도착합니다.
2. JWT 필터가 `Authorization` 헤더의 JWT를 확인합니다.
3. 유효한 토큰이면 회원을 DB에서 확인하고, 회원 ID와 `ROLE_...` 권한을 `SecurityContext`에 넣습니다.
4. 보안 규칙이 해당 URL에 필요한 인증·권한을 확인합니다.
5. `BoardController`가 JSON을 요청 DTO로 변환하고 `@Valid` 검증을 적용합니다.
6. Controller가 `SecurityContext`의 회원 ID와 DTO를 `BoardService`에 전달합니다.
7. Service가 게시글·회원 상태와 작성 권한을 확인하고 Repository를 통해 Entity를 저장합니다.
8. 결과를 응답 DTO로 변환하고 HTTP 응답으로 돌려줍니다.
9. 업무 예외가 발생하면 `GlobalExceptionHandler`가 적절한 HTTP 오류 응답으로 바꿉니다.

인증이 필요한 API에서 회원 ID를 요청 본문이나 URL로 받지 않는 이유는 클라이언트가 임의로 다른 회원 ID를 제출하는 일을 막기 위해서입니다.

## 5. 기능별 도메인

| 기능 | 주요 패키지 | 공부할 내용 |
|---|---|---|
| 인증·회원 | `auth/` | 가입, 비밀번호 검증, OAuth, JWT, 역할, 필터, 예외 |
| 게시판·댓글 | `board/` | 공개 조회, 회원별 작성 권한, 검색·페이지 조회 |
| 예약 | `reservation/` | 날짜 정책, 활성 상태, 동시성, 관리자 상태 변경 |
| 알림 | `notification/` | DB 알림, 읽음 처리, 도메인 이벤트, SSE |
| 관리자 | `admin/` | 역할 인가, 회원 정지, 공지·예약 관리 |
| 마이페이지 | `mypage/` | 본인 정보만 조회·수정, 비밀번호 재확인 |
| 공통 시간 | `common/entity/` | JPA auditing 기반 생성·수정 시각 |

### 인증 및 JWT

- `AuthController`는 회원가입, 로그인, OAuth 로그인, 토큰 갱신, 로그아웃, 회원 탈퇴 요청을 받습니다.
- `AuthService`는 회원 가입·자격 증명 등 내부 인증 규칙을 처리하고, `OAuthService`는 외부 제공자 정보를 내부 회원 및 토큰 흐름으로 연결합니다.
- Access Token은 응답 본문으로 전달되고 Refresh Token은 `HttpOnly` 쿠키로 전달됩니다. 쿠키의 `Secure`, `SameSite`, 만료 시간은 설정값을 따릅니다.
- 서버는 세션을 사용하지 않는 `STATELESS` 방식입니다. Access Token은 매 요청에서 검증됩니다.
- `JwtAuthenticationFilter`는 회원이 실제 존재하는지와 정지 여부도 확인합니다. 이미 발급된 토큰이라도 정지 회원이면 접근을 거부합니다.
- `SecurityConfig`에서 `/admin/**`는 `ROLE_ADMIN`을 요구하고, 인증 관련 공개 경로와 게시판의 GET 조회를 허용합니다. 그 외 요청은 인증이 필요합니다.
- 인증되지 않은 요청은 보통 401, 인증은 되었지만 권한이 부족한 요청은 403으로 구분합니다.
- 로그아웃은 서버 세션을 폐기하는 방식이 아닙니다. Refresh Token 쿠키를 만료시키며, 클라이언트도 보관 중인 Access Token을 지워야 합니다.

### 게시판 및 댓글

`BoardController`가 게시글·댓글 API를 받고 `BoardService`가 작성자 권한과 도메인 규칙을 처리합니다. 게시글 목록은 페이지와 선택적 검색어를 지원하고, GET 조회는 공개지만 생성·수정·삭제 및 댓글 쓰기는 인증이 필요합니다. 작성자와 관리자 권한을 확인하는 로직을 Service에서 찾아보세요.

페이지 번호는 0부터 시작하고, 페이지 크기는 Controller의 검증 범위(기본 10, 최대 100)를 따릅니다. 페이지 응답 DTO가 콘텐츠뿐 아니라 전체 건수·페이지 정보도 반환하는 이유를 살펴보세요.

### 예약

`Reservation`은 예약 종류, 예약 시각과 날짜, 주소·연락처, 상태, 예약 회원을 보관합니다. 새 예약은 `PENDING`으로 시작하며 상태 변경은 관리자 흐름에서 수행합니다.

예약 생성 흐름은 다음과 같습니다.

1. 로그인 회원을 조회합니다.
2. 대한민국 기준 현재 날짜와 비교해 과거 예약인지 확인합니다.
3. 날짜에 `PENDING` 또는 `APPROVED` 상태의 예약이 있는지 확인합니다.
4. 입력 문자열을 정리하고 예약을 저장합니다.
5. 예약 접수 알림을 같은 업무 흐름에서 저장합니다.

가용성 조회는 선택한 월의 날짜별 예약 가능 여부를 반환합니다. 달력 조회의 사전 확인만으로 동시 요청을 막을 수는 없으므로 DB의 활성 예약 날짜 고유 인덱스가 최종 방어선입니다. 충돌 예외는 예약 날짜 중복일 때만 사용자에게 의미 있는 충돌 오류로 바꾸고, 다른 데이터 무결성 오류는 원래 오류로 전달합니다.

현재 migration 흐름은 날짜별 전체 고유 제약을 만들었다가, 취소·거절된 예약 날짜의 재예약을 허용하도록 `PENDING`·`APPROVED` 상태에만 부분 고유 인덱스를 적용합니다. 이후 기존 전체 고유 제약을 제거합니다. 예약 상태 변경으로 비활성 예약을 다시 활성화할 때도 날짜 중복을 확인해야 합니다.

### 알림과 SSE

`NotificationService`는 알림을 DB에 저장하고, 회원 본인의 홈 미리보기·페이지 조회·읽음 처리를 제공합니다. 알림 저장 시 도메인 이벤트를 발행하며, `NotificationStreamService`는 트랜잭션 커밋 이후 이벤트를 SSE 연결로 전달합니다. 커밋 전에 브라우저에 알림을 보내면 DB 저장이 롤백된 경우 실제 상태와 알림이 달라질 수 있기 때문입니다.

SSE 연결은 회원별로 관리되고 한 회원의 여러 브라우저 탭을 지원합니다. 현재 연결 집합은 애플리케이션 프로세스 메모리에 있으므로 여러 백엔드 인스턴스를 운영할 때 인스턴스 간 전파는 별도 메시지 브로커 같은 구조가 필요합니다. 실시간 연결이 끊겨도 저장 알림 조회 API가 복구 경로가 됩니다.

### 관리자와 마이페이지

- 관리자 API는 `/admin/**` 경로이며 Security 설정에서 ADMIN 역할을 요구합니다. 일부 Service에서도 관리자 역할을 확인해 업무 경계에서 권한을 보강합니다.
- 관리자는 회원 정지 상태, 게시글·공지, 예약 상태를 관리합니다. 정지 상태는 JWT 필터가 DB에서 확인하므로 이후 요청에도 반영됩니다.
- `/api/mypage`는 현재 로그인 회원 정보를 조회·수정합니다. 회원 ID를 요청에서 받지 않고 인증 정보에서 가져와 타인 정보 변경을 제한합니다.
- 비밀번호 변경은 현재 비밀번호를 다시 검증한 다음 처리합니다. 비밀번호 원문이나 해시가 응답에 포함되지 않는지 DTO를 확인해 보세요.

## 6. 검증, 오류 응답, HTTP 상태

요청 DTO의 필드 조건은 Jakarta Validation 애너테이션으로 선언하고 Controller에서 `@Valid`를 적용합니다. 페이지·월 같은 파라미터 범위는 Controller의 `@Validated`, `@Min`, `@Max` 등을 확인합니다.

도메인 오류는 `BoardNotFoundException`, `ReservationDateUnavailableException`처럼 의미 있는 예외로 표현합니다. `GlobalExceptionHandler`의 `@RestControllerAdvice`가 이를 JSON 메시지와 상태 코드로 변환합니다.

| 상황 | 흔한 HTTP 상태 |
|---|---:|
| 잘못된 입력·범위 초과 | 400 |
| 인증되지 않음·유효하지 않은 자격 증명 | 401 |
| 권한 부족·정지 회원 | 403 |
| 대상 리소스 없음 | 404 |
| 중복 이메일·예약 날짜 충돌 | 409 |
| 생성 성공 | 201 |
| 본문 없는 성공 | 204 |

실제 응답은 해당 Controller, Security 설정, 예외 처리기의 구체 구현이 기준입니다. 특히 Spring Security 필터 단계 오류는 Controller에 도달하기 전 응답되므로 Controller 예외 처리와 경로가 다를 수 있습니다.

## 7. JPA, 트랜잭션, DB 스키마

- `@Entity` 객체는 테이블 데이터에 대응합니다. `@Id`가 기본 키, `@ManyToOne`이 다른 Entity와의 관계를 나타냅니다.
- `FetchType.LAZY`는 연관 Entity를 처음부터 모두 읽지 않고 실제 접근 시 로드하도록 합니다. 응답 DTO 변환 시 트랜잭션 범위와 필요한 조회를 확인합니다.
- `@Transactional`은 Service 메서드의 데이터 변경을 하나의 트랜잭션으로 묶습니다. 조회 전용 메서드는 `readOnly = true`로 의도를 나타냅니다.
- 영속 Entity의 필드를 바꾸면 트랜잭션 커밋 시 변경 감지(Dirty Checking)로 UPDATE될 수 있습니다.
- 공통 `BaseTimeEntity`와 JPA Auditing은 생성·수정 시각 기록을 공유합니다.
- 응답 DTO로 변환할 때 필요한 데이터만 반환해 내부 Entity를 직접 노출하지 않습니다.
- `ddl-auto: validate`이므로 엔티티와 실제 스키마가 맞지 않으면 시작 단계에서 오류가 날 수 있습니다.

### Flyway migrations

마이그레이션은 `src/main/resources/db/migration/`에 있습니다. 파일명 버전 순서대로 적용되며 이미 적용된 migration 파일을 수정하는 대신 새로운 버전 파일을 추가합니다.

- `V0_1` 회원 기본 구조
- `V1` 게시판·댓글 및 회원 역할
- `V2` 회원 정지 상태
- `V3` 예약 도메인
- `V4` 예약 날짜와 날짜 조회 인덱스
- `V5` 활성 상태 예약에 한정한 날짜 고유 인덱스
- `V6` 전체 날짜 고유 제약 제거
- `V7` 알림 테이블

기존 DB를 연결할 때는 `docs/flyway-baseline.md` 및 `docs/flyway-reservation-migration-recovery.md`를 먼저 읽습니다. Baseline은 스키마를 생성·수정하지 않으므로, 실제 DB 구조가 지정 버전과 일치하는지 확인하고 백업한 뒤 수행해야 합니다. `baseline-on-migrate`는 현재 자동 활성화되지 않습니다.

#### 프로필별 시드 데이터

`R__test_seed_data.sql`은 `db/seed`에 있으며 기본 프로필의 Flyway 경로에는 포함되지 않습니다. `application-seed.yaml`에서만 `db/seed`를 추가하므로 개발용 시드를 적용할 때에만 `seed` 프로필을 사용합니다. 기본 프로필의 `repeatable:missing` 예외는 이전 실행에서 기록된 시드 이력 때문에 앱이 중단되지 않게 하지만, 다른 누락된 반복 마이그레이션도 무시하므로 DB 이력을 별도로 확인해야 합니다. 이 설정은 기존 시드 행이나 Flyway 이력을 삭제하지 않습니다.

## 8. 테스트로 동작 확인하기

테스트 파일은 `src/test/java/com/cleaner/be/` 아래 기능별로 있습니다. 테스트 이름을 먼저 읽고 어떤 규칙을 보장하는지 예상한 뒤 실행해 보세요.

```powershell
.\gradlew.bat test
```

찾아볼 테스트 사례:

- `AuthServiceTest`: 가입·로그인과 인증 서비스 규칙
- `AuthControllerSessionTest`: 토큰·인증 관련 컨트롤러 동작
- `AdminAuthorizationTest`: 관리자 인가
- `ReservationDatePolicyTest`: 날짜 정책
- `ReservationAvailabilityValidationTest`: 가용성 입력 검증
- `ReservationServiceConstraintTest`: DB 제약과 충돌 처리
- `ReservationServiceNotificationTest`: 예약 처리와 알림 연결
- `NotificationServiceTest`: 알림 저장·읽음 처리
- `NotificationControllerIntegrationTest`: HTTP 인증과 SSE 통합 동작
- `MyPageServiceTest`: 본인 정보·비밀번호 규칙

테스트는 구현을 읽는 보조 자료이자 회귀 방지 계약입니다. 테스트가 통과해도 운영 PostgreSQL 설정이나 분산 환경의 모든 특성이 검증된 것은 아니므로, H2 테스트와 실제 DB migration은 별도로 이해해야 합니다.

## 9. 작은 실습 과제

1. `GET /api/boards` 요청이 보안 필터에서 공개되는 이유와 POST 요청의 차이를 설명합니다.
2. Access Token과 Refresh Token을 서로 다른 전달 방식으로 사용하는 이유를 설명합니다.
3. 예약 날짜를 Service에서 확인하는 것과 DB 고유 인덱스가 각각 필요한 이유를 설명합니다.
4. `@Transactional`이 예약 저장과 알림 저장의 일관성에 어떤 도움을 주는지 추적합니다.
5. `GlobalExceptionHandler`의 예외 하나를 골라 발생 지점부터 HTTP 응답까지 따라갑니다.
6. 알림 이벤트가 커밋 후 SSE로 전달되는 과정과 현재 구조의 다중 인스턴스 한계를 설명합니다.
7. 기존 migration 파일을 수정하지 않고 새 컬럼을 추가하는 migration을 설계합니다.
8. Service의 업무 규칙을 검증하는 단위 테스트 하나를 작성하고, 입력·기대 결과·경계 조건을 설명합니다.

## 10. 파일 찾아보기

- 시작 클래스: `src/main/java/com/cleaner/be/BeApplication.java`
- 인증 설정·필터: `src/main/java/com/cleaner/be/auth/SecurityConfig.java`, `JwtAuthenticationFilter.java`, `JwtTokenProvider.java`
- 공통 오류 변환: `src/main/java/com/cleaner/be/auth/GlobalExceptionHandler.java`
- 예약 핵심 로직: `src/main/java/com/cleaner/be/reservation/ReservationService.java`
- 알림 및 실시간 전달: `src/main/java/com/cleaner/be/notification/NotificationService.java`, `NotificationStreamService.java`
- DB migration: `src/main/resources/db/migration/`
- 개별 학습 노트: `study/`
- 기존 DB 및 복구 절차: `docs/`

개별 학습 노트는 클래스 단위 세부 설명을 담고 있으므로 이 통합 문서와 함께 읽으면 좋습니다. 이 문서는 코드 전체를 복제한 목록이 아니라 기능 간 연결과 공부 순서를 정리한 안내서입니다.
