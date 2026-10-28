# NotificationRepository 학습 가이드

## 1. 역할·레이어·왜·언제

`NotificationRepository`는 알림 테이블에 접근하는 Spring Data JPA 인터페이스입니다. 기본 CRUD와 함께 회원별 최신 조회, 미읽음 집계, 소유자 조건 조회, 전체 읽음 처리를 제공합니다.

## 2. 현재 원본 코드

```java
public interface NotificationRepository extends JpaRepository<Notification, Long> {
	List<Notification> findTop3ByMemberIdOrderByCreatedAtDescIdDesc(Long memberId);
	Page<Notification> findByMemberIdOrderByCreatedAtDescIdDesc(Long memberId, Pageable pageable);
	long countByMemberIdAndReadAtIsNull(Long memberId);
	Optional<Notification> findByIdAndMemberId(Long id, Long memberId);

	@Modifying
	@Query("update Notification n set n.readAt = :readAt where n.member.id = :memberId and n.readAt is null")
	int markAllAsRead(@Param("memberId") Long memberId, @Param("readAt") LocalDateTime readAt);

	@Transactional
	void deleteByMemberId(Long memberId);
}
```

실제 원본에는 각 쿼리 의도를 설명하는 한국어 주석과 관련 import가 포함되어 있습니다.

## 3. 쿼리 메서드

- `findTop3ByMemberIdOrderByCreatedAtDescIdDesc`는 해당 회원의 알림 중 최신 세 건만 조회합니다.
- `findByMemberIdOrderByCreatedAtDescIdDesc`는 회원 알림만 대상으로 `Pageable` 크기만큼 최신순 페이지를 반환합니다.
- `countByMemberIdAndReadAtIsNull`은 회원의 미읽음 개수를 계산합니다.
- `findByIdAndMemberId`는 알림 ID와 소유 회원 ID를 함께 조건으로 사용합니다. 다른 회원의 ID를 입력해도 결과가 나오지 않습니다.
- `deleteByMemberId`는 지정 회원 알림만 삭제합니다. 회원 탈퇴의 전체 데이터 정리는 회원 삭제와 FK `ON DELETE CASCADE`에서도 보장됩니다.

## 4. 일괄 읽음 JPQL

`markAllAsRead`는 `@Modifying`으로 UPDATE JPQL을 실행합니다. 조건에 `n.readAt is null`이 있으므로 이미 읽은 알림의 최초 읽음 시각을 덮어쓰지 않습니다. 반환값은 변경된 행 수입니다. 메서드 호출은 서비스의 트랜잭션 안에서 이뤄집니다.

## 5. 실제 실행 흐름

서비스가 회원 ID를 repository 쿼리에 넘깁니다. Spring Data는 메서드 이름에서 조건과 정렬을 만들고, 선언된 `@Query`에는 `@Param` 값으로 입력을 연결합니다. 홈은 최신 세 건 쿼리와 미읽음 집계 쿼리를 함께 사용하고, 보관 페이지는 `Page` 메타데이터를 응답 DTO로 바꿉니다.

## 6. 관계: 확인된 사실과 추론

- **코드로 확인됨:** 관리 엔티티와 기본키는 [`Notification`](../../src/main/java/com/cleaner/be/notification/Notification.java), `Long`입니다.
- **코드로 확인됨:** 서비스는 회원 조건이 포함된 메서드로 알림을 조회합니다.
- **코드로 확인됨:** 테이블의 회원·예약 삭제 동작은 [`V7 마이그레이션`](../../src/main/resources/db/migration/V7__create_member_notifications.sql)에 정의됩니다.
- **추론:** 실제 데이터베이스에서 선택되는 실행 계획과 인덱스는 데이터 분포 및 DB 엔진에 따라 달라집니다.

## 7. 이 파일에만 적용되는 개념

Spring Data 파생 쿼리는 메서드 이름을 조건과 정렬로 해석합니다. `Pageable`은 페이지 위치와 크기를 전달하고 `Page`는 결과 목록과 전체 개수 같은 페이지 메타데이터를 함께 반환합니다. `@Modifying`은 조회가 아닌 JPQL 변경 쿼리임을 표시합니다.

## 8. 예시

```java
Page<Notification> firstPage = notificationRepository
	.findByMemberIdOrderByCreatedAtDescIdDesc(memberId, PageRequest.of(0, 10));
```

실제 서비스와 마찬가지로 회원별 첫 10개 알림을 최신순으로 요청하는 예시입니다.

## 9. 요약

Repository는 회원 범위의 조회와 읽음 업데이트를 선언합니다. 소유자 ID 조건은 서비스의 인증 회원 ID와 함께 사용되어 알림 간 데이터 경계를 유지합니다.
