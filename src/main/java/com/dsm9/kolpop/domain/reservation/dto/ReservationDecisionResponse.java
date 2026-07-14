package com.dsm9.kolpop.domain.reservation.dto;

public record ReservationDecisionResponse(
        Long reservationId,
        ReservationStatusResponse status,
        Long chatRoomId
) {
}
