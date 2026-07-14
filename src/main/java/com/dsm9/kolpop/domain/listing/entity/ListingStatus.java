package com.dsm9.kolpop.domain.listing.entity;

public enum ListingStatus {
    RECRUITING("RECRUITING", "모집중"),
    CLOSED("CLOSED", "모집종료");

    private final String code;
    private final String label;

    ListingStatus(String code, String label) {
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
