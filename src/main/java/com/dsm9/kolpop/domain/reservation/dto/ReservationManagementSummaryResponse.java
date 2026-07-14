package com.dsm9.kolpop.domain.reservation.dto;

public record ReservationManagementSummaryResponse(
        long pendingCount,
        long approvedCount,
        long totalCount
) {
}
