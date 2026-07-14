package com.dsm9.kolpop.domain.festival.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import com.dsm9.kolpop.domain.festival.client.PublicFestivalClient;
import com.dsm9.kolpop.domain.festival.dto.FestivalDetailResponse;
import com.dsm9.kolpop.domain.festival.dto.FestivalListResponse;
import com.dsm9.kolpop.domain.festival.dto.PublicFestivalItem;
import com.dsm9.kolpop.domain.listing.entity.Listing;
import com.dsm9.kolpop.domain.listing.entity.ListingStatus;
import com.dsm9.kolpop.domain.listing.repository.ListingLikeRepository;
import com.dsm9.kolpop.domain.listing.repository.ListingRepository;
import com.dsm9.kolpop.domain.user.entity.User;
import com.dsm9.kolpop.domain.user.entity.UserRole;

class FestivalServiceTests {

    @Test
    void festivalListIncludesNearbyListingCount() {
        PublicFestivalClient publicFestivalClient = mock(PublicFestivalClient.class);
        ListingRepository listingRepository = mock(ListingRepository.class);
        ListingLikeRepository listingLikeRepository = mock(ListingLikeRepository.class);
        FestivalService festivalService = new FestivalService(publicFestivalClient, listingRepository, listingLikeRepository);
        Listing nearbyListing = createListing(10L);

        when(publicFestivalClient.fetchAll()).thenReturn(List.of(createFestival()));
        when(listingRepository.findAllByStatusAndLatitudeBetweenAndLongitudeBetweenOrderByCreatedAtDesc(
                eq(ListingStatus.RECRUITING),
                any(BigDecimal.class),
                any(BigDecimal.class),
                any(BigDecimal.class),
                any(BigDecimal.class)
        )).thenReturn(List.of(nearbyListing));

        FestivalListResponse response = festivalService.getFestivals(null, null, null, null, null, null);

        assertEquals(1, response.totalCount());
        assertEquals(1, response.festivals().getFirst().nearbyListingCount());
    }

    @Test
    void festivalDetailIncludesNearbyListings() {
        PublicFestivalClient publicFestivalClient = mock(PublicFestivalClient.class);
        ListingRepository listingRepository = mock(ListingRepository.class);
        ListingLikeRepository listingLikeRepository = mock(ListingLikeRepository.class);
        FestivalService festivalService = new FestivalService(publicFestivalClient, listingRepository, listingLikeRepository);
        Listing nearbyListing = createListing(10L);

        when(publicFestivalClient.fetchAll()).thenReturn(List.of(createFestival()));
        when(listingRepository.findAllByStatusAndLatitudeBetweenAndLongitudeBetweenOrderByCreatedAtDesc(
                eq(ListingStatus.RECRUITING),
                any(BigDecimal.class),
                any(BigDecimal.class),
                any(BigDecimal.class),
                any(BigDecimal.class)
        )).thenReturn(List.of(nearbyListing));

        String festivalId = festivalService.getFestivals(null, null, null, null, null, null)
                .festivals()
                .getFirst()
                .id();
        FestivalDetailResponse response = festivalService.getFestivalDetail(festivalId);

        assertEquals(1, response.nearbyListings().size());
        assertEquals(10L, response.nearbyListings().getFirst().listingId());
    }

    private PublicFestivalItem createFestival() {
        return new PublicFestivalItem(
                "Boryeong Mud Festival",
                "Daecheon Beach",
                "2026-07-24",
                "2026-08-09",
                "Festival content",
                "Boryeong-si",
                "Host",
                "Sponsor",
                "041-000-0000",
                "festival.example.com",
                "Related info",
                "Chungnam Boryeong-si Daecheon Beach",
                null,
                "36.3050000",
                "126.5130000",
                "2026-07-14"
        );
    }

    private Listing createListing(Long id) {
        User landlord = new User(
                "landlord01",
                "Landlord",
                "landlord@kolpop.kr",
                "encodedPassword",
                "01012345678",
                UserRole.LANDLORD
        );
        ReflectionTestUtils.setField(landlord, "id", 1L);

        Listing listing = new Listing(
                landlord,
                "Nearby listing",
                List.of("https://cdn.example.com/listing.jpg"),
                "Chungnam Boryeong-si",
                "1F",
                new BigDecimal("36.3060000"),
                new BigDecimal("126.5140000"),
                80000L,
                1000000L,
                new BigDecimal("40.0"),
                List.of(),
                List.of(),
                List.of(),
                java.time.LocalDate.of(2026, 7, 20),
                java.time.LocalDate.of(2026, 8, 10),
                1,
                7,
                "Description",
                List.of(),
                LocalDateTime.of(2026, 7, 14, 9, 0)
        );
        ReflectionTestUtils.setField(listing, "id", id);
        return listing;
    }
}
