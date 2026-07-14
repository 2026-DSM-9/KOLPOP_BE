package com.dsm9.kolpop.domain.festival.client;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import com.dsm9.kolpop.domain.festival.config.PublicFestivalProperties;
import com.dsm9.kolpop.domain.festival.dto.PublicFestivalItem;

class PublicFestivalClientTests {

    @Test
    void fetchAllMapsStandardFestivalApiFieldsAndEncodesDecodedServiceKey() {
        RestClient.Builder restClientBuilder = RestClient.builder();
        MockRestServiceServer server = MockRestServiceServer.bindTo(restClientBuilder).build();
        PublicFestivalProperties properties = new PublicFestivalProperties();
        properties.setEndpoint("https://api.data.go.kr/openapi/tn_pubr_public_cltur_fstvl_api");
        properties.setServiceKey("abc+def/ghi=");
        properties.setPerPage(1000);
        properties.setMaxPages(1);

        PublicFestivalClient client = new PublicFestivalClient(restClientBuilder, properties);

        server.expect(requestTo("https://api.data.go.kr/openapi/tn_pubr_public_cltur_fstvl_api?pageNo=1&numOfRows=1000&type=json&serviceKey=abc%2Bdef%2Fghi%3D"))
                .andRespond(withSuccess("""
                        {
                          "response": {
                            "header": {
                              "resultCode": "00",
                              "resultMsg": "NORMAL SERVICE."
                            },
                            "body": {
                              "items": [
                                {
                                  "fstvlNm": "보령머드축제",
                                  "opar": "대천해수욕장",
                                  "fstvlStartDate": "2026-07-24",
                                  "fstvlEndDate": "2026-08-09",
                                  "fstvlCo": "축제 내용",
                                  "mnnstNm": "재단법인 보령축제관광재단",
                                  "auspcInsttNm": "충청남도 보령시",
                                  "suprtInsttNm": "한국관광공사",
                                  "phoneNumber": "041-000-0000",
                                  "homepageUrl": "festival.example.com",
                                  "relateInfo": "관련 정보",
                                  "rdnmadr": "충남 보령시 대천해수욕장",
                                  "lnmadr": "",
                                  "latitude": "36.3050000",
                                  "longitude": "126.5130000",
                                  "referenceDate": "2026-07-14"
                                }
                              ],
                              "numOfRows": 1000,
                              "pageNo": 1,
                              "totalCount": 1
                            }
                          }
                        }
                        """, MediaType.APPLICATION_JSON));

        List<PublicFestivalItem> items = client.fetchAll();

        assertEquals(1, items.size());
        assertEquals("보령머드축제", items.getFirst().name());
        assertEquals("대천해수욕장", items.getFirst().place());
        assertEquals("2026-07-24", items.getFirst().startDate());
        assertEquals("2026-08-09", items.getFirst().endDate());
        assertEquals("36.3050000", items.getFirst().latitude());
        assertEquals("126.5130000", items.getFirst().longitude());
        server.verify();
    }
}
