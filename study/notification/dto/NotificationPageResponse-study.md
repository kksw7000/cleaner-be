# NotificationPageResponse 학습 가이드

## 1. 역할·레이어·왜·언제

`NotificationPageResponse`는 알림 보관 화면에 현재 페이지 항목과 전체 페이지 이동 정보를 전달합니다. Spring Data의 `Page`를 API 타입으로 그대로 노출하지 않고 필요한 값만 응답 계약으로 정의합니다.

## 2. 현재 원본 코드

```java
package com.cleaner.be.notification.dto;

import java.util.List;

/** 알림 보관 페이지의 현재 항목과 페이지 이동 정보를 반환합니다. */
public record NotificationPageResponse(
	List<NotificationResponse> notifications,
	int page,
	int size,
	long totalElements,
	int totalPages,
	boolean first,
	boolean last
) {
}
```

## 3. 필드 설명

- `notifications`는 현재 페이지의 알림 목록입니다.
- `page`는 0부터 시작하는 현재 페이지 번호이고, `size`는 페이지 크기입니다.
- `totalElements`는 회원의 전체 알림 수, `totalPages`는 페이지 총 개수입니다.
- `first`, `last`는 현재 페이지의 첫 페이지·마지막 페이지 여부입니다.

## 4. 실제 실행 흐름

`GET /api/notifications` 요청에서 Controller는 `page`와 `size` 입력을 검증하고 최신순 `PageRequest`를 생성합니다. 서비스는 회원 조건 페이지 조회 결과를 `NotificationResponse`로 변환하고 `Page` 메타데이터를 이 record에 복사합니다.

## 5. 관계: 확인된 사실과 추론

- **코드로 확인됨:** [`NotificationController`](../../../src/main/java/com/cleaner/be/notification/NotificationController.java)는 페이지 기본값 0, 크기 기본값 10, 최대 크기 100을 적용합니다.
- **코드로 확인됨:** [`NotificationService`](../../../src/main/java/com/cleaner/be/notification/NotificationService.java)가 Spring `Page`에서 응답값을 복사합니다.
- **코드로 확인됨:** 목록 항목 타입은 [`NotificationResponse`](../../../src/main/java/com/cleaner/be/notification/dto/NotificationResponse.java)입니다.
- **추론:** 페이지 번호를 사용자에게 1부터 보여줄지는 프론트엔드 표시 정책이며 이 DTO는 0 기반 번호를 전달합니다.

## 6. 이 파일에만 적용되는 개념

페이지 응답은 한 번에 전송하는 항목 수를 제한하면서 전체 항목 수와 다음/이전 페이지 판단에 필요한 정보를 제공합니다. `long totalElements`는 큰 전체 건수도 표현합니다.

## 7. 예시

```java
new NotificationPageResponse(items, 0, 10, 23L, 3, true, false);
```

전체 23개 알림 가운데 첫 페이지 10개를 반환하는 예시입니다.

## 8. 요약

이 DTO는 현재 알림 페이지와 전체 목록의 위치 정보를 함께 전달합니다. 페이지 조회의 회원 소유 범위와 정렬은 Controller·Service·Repository 계층에서 적용합니다.
