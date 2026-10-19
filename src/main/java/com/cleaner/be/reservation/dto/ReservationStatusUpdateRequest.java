package com.cleaner.be.reservation.dto;

import com.cleaner.be.reservation.ReservationStatus;

import jakarta.validation.constraints.NotNull;

/** 관리자가 예약 처리 결과를 전달하며, null 상태가 저장되어 처리 흐름이 깨지는 것을 막습니다. */
public record ReservationStatusUpdateRequest(@NotNull ReservationStatus status) {
}
