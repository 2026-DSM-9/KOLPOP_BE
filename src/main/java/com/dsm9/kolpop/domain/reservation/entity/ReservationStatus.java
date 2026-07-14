package com.dsm9.kolpop.domain.reservation.entity;

public enum ReservationStatus {
    PENDING("PENDING", "승인 대기"),
    APPROVED("APPROVED", "승인 완료"),
    REJECTED("REJECTED", "거절");

    private final String code;
    private final String label;

    ReservationStatus(String code, String label) {
        this.code = code;
        this.label = label;
    }

    public String getCode() {
        return code;
    }

    public String getLabel() {
        return label;
    }
}
