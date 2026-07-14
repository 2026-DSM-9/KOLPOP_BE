package com.dsm9.kolpop.domain.festival.dto;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonProperty;

public record PublicFestivalItem(
        @JsonProperty("축제명")
        @JsonAlias("fstvlNm")
        String name,

        @JsonProperty("개최장소")
        @JsonAlias("opar")
        String place,

        @JsonProperty("축제시작일자")
        @JsonAlias("fstvlStartDate")
        String startDate,

        @JsonProperty("축제종료일자")
        @JsonAlias("fstvlEndDate")
        String endDate,

        @JsonProperty("축제내용")
        @JsonAlias("fstvlCo")
        String content,

        @JsonProperty("주관기관명")
        @JsonAlias("mnnstNm")
        String managingAgency,

        @JsonProperty("주최기관명")
        @JsonAlias("auspcInsttNm")
        String hostAgency,

        @JsonProperty("후원기관명")
        @JsonAlias("suprtInsttNm")
        String sponsorAgency,

        @JsonProperty("전화번호")
        @JsonAlias("phoneNumber")
        String phoneNumber,

        @JsonProperty("홈페이지주소")
        @JsonAlias("homepageUrl")
        String homepageUrl,

        @JsonProperty("관련정보")
        @JsonAlias("relateInfo")
        String relatedInfo,

        @JsonProperty("소재지도로명주소")
        @JsonAlias("rdnmadr")
        String roadAddress,

        @JsonProperty("소재지지번주소")
        @JsonAlias("lnmadr")
        String lotAddress,

        @JsonProperty("위도")
        @JsonAlias("latitude")
        String latitude,

        @JsonProperty("경도")
        @JsonAlias("longitude")
        String longitude,

        @JsonProperty("데이터기준일자")
        @JsonAlias("referenceDate")
        String referenceDate
) {
}
