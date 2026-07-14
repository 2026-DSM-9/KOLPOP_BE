package com.dsm9.kolpop.domain.reservation.dto;

import java.util.List;

public record ReservationManagementResponse(
        ReservationManagementSummaryResponse summary,
        List<ReservationManagementItemResponse> reservations
) {
}
