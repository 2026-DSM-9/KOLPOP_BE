package com.dsm9.kolpop.domain.reservation.dto;

import java.time.LocalDateTime;

public record CreateReservationResponse(
        Long reservationId,
        ReservationStatusResponse status,
        LocalDateTime appliedAt
) {
}
