package com.dsm9.kolpop.domain.festival.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public record PublicFestivalItem(
        @JsonProperty("축제명")
        String name,

        @JsonProperty("개최장소")
        String place,

        @JsonProperty("축제시작일자")
        String startDate,

        @JsonProperty("축제종료일자")
        String endDate,

        @JsonProperty("축제내용")
        String content,

        @JsonProperty("주관기관명")
        String managingAgency,

        @JsonProperty("주최기관명")
        String hostAgency,

        @JsonProperty("후원기관명")
        String sponsorAgency,

        @JsonProperty("전화번호")
        String phoneNumber,

        @JsonProperty("홈페이지주소")
        String homepageUrl,

        @JsonProperty("관련정보")
        String relatedInfo,

        @JsonProperty("소재지도로명주소")
        String roadAddress,

        @JsonProperty("소재지지번주소")
        String lotAddress,

        @JsonProperty("위도")
        String latitude,

        @JsonProperty("경도")
        String longitude,

        @JsonProperty("데이터기준일자")
        String referenceDate
) {
}
