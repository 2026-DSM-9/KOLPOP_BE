package com.dsm9.kolpop.domain.reservation.dto;

import java.time.LocalDate;
import java.time.LocalDateTime;

public record ReservationManagementItemResponse(
        Long reservationId,
        Long listingId,
        String listingTitle,
        Long founderId,
        String founderName,
        LocalDate reservationStartDate,
        LocalDate reservationEndDate,
        Integer usageDays,
        LocalDateTime appliedAt,
        String message,
        ReservationStatusResponse status,
        Long chatRoomId,
        boolean canApprove,
        boolean canReject,
        boolean canChat
) {
}
