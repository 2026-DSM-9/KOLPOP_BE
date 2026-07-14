package com.dsm9.kolpop.domain.festival.dto;

import java.util.Collections;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonProperty;

public record PublicFestivalApiResponse(
        Integer currentCount,
        @JsonProperty("data")
        List<PublicFestivalItem> odCloudData,
        Integer matchCount,
        Integer page,
        Integer perPage,
        @JsonProperty("totalCount")
        Integer odCloudTotalCount,
        Response response
) {

    public List<PublicFestivalItem> data() {
        if (odCloudData != null) {
            return odCloudData;
        }
        if (response != null && response.body() != null && response.body().items() != null) {
            return response.body().items();
        }
        return Collections.emptyList();
    }

    public int totalCount() {
        if (odCloudTotalCount != null) {
            return odCloudTotalCount;
        }
        if (response != null && response.body() != null && response.body().totalCount() != null) {
            return response.body().totalCount();
        }
        return data().size();
    }

    public record Response(
            Header header,
            Body body
    ) {
    }

    public record Header(
            String resultCode,
            String resultMsg
    ) {
    }

    public record Body(
            List<PublicFestivalItem> items,
            Integer numOfRows,
            Integer pageNo,
            Integer totalCount
    ) {
    }
}
