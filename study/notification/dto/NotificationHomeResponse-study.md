# NotificationHomeResponse 학습 가이드

## 1. 역할·레이어·왜·언제

`NotificationHomeResponse`는 홈 알림 창에 표시할 최신 알림과 전체 미읽음 개수를 함께 반환하는 응답 DTO입니다. 최신 미리보기는 세 건으로 제한되지만 배지 개수는 모든 미읽음 알림을 대상으로 합니다.

## 2. 현재 원본 코드

```java
package com.cleaner.be.notification.dto;

import java.util.List;

/** 홈 화면에 표시할 최신 알림 세 건과 전체 미읽음 개수를 함께 반환합니다. */
public record NotificationHomeResponse(List<NotificationResponse> notifications, long unreadCount) {
}
```

## 3. 필드 설명

- `notifications`는 최신순 알림 응답 목록입니다. 서비스가 최신 세 건 조회 결과를 넣습니다.
- `unreadCount`는 해당 회원 전체 알림 가운데 `readAt`이 null인 알림 개수입니다. 홈 목록의 길이와 반드시 같지는 않습니다.
- `record`는 두 응답 값을 불변으로 묶고 accessor를 자동 제공합니다.

## 4. 실제 실행 흐름

Controller의 `GET /api/notifications/home` 요청이 서비스의 `getHomeNotifications`를 호출합니다. 서비스는 최근 세 건과 전체 미읽음 개수를 각각 조회하고 이 record를 생성합니다. Controller는 record를 JSON으로 반환합니다.

## 5. 관계: 확인된 사실과 추론

- **코드로 확인됨:** [`NotificationService`](../../../src/main/java/com/cleaner/be/notification/NotificationService.java)가 응답을 구성합니다.
- **코드로 확인됨:** [`NotificationRepository`](../../../src/main/java/com/cleaner/be/notification/NotificationRepository.java)가 최근 세 건 및 전체 미읽음 개수를 별도로 조회합니다.
- **코드로 확인됨:** [`NotificationController`](../../../src/main/java/com/cleaner/be/notification/NotificationController.java)가 `/home` endpoint를 제공합니다.
- **추론:** 화면에서 알림을 읽은 직후 배지에 반영하는 시점은 프론트엔드가 API를 다시 조회하거나 로컬 상태를 갱신하는 방식에 달려 있습니다.

## 6. 이 파일에만 적용되는 개념

미리보기 항목과 집계 숫자를 같은 응답으로 묶으면 화면이 한 요청으로 표시 데이터를 얻을 수 있습니다. 집계 범위가 목록과 다를 수 있으므로 API 계약에 이를 명시하는 것이 중요합니다.

## 7. 예시

```java
new NotificationHomeResponse(latestThree, 8L);
```

홈에는 최신 세 개만 있지만 회원 전체에는 미읽음 알림이 여덟 개 있는 상황을 표현합니다.

## 8. 요약

홈 응답은 최신 3개 목록과 전체 미읽음 수의 조합입니다. `unreadCount`는 현재 페이지나 미리보기 목록만 센 값이 아닙니다.
