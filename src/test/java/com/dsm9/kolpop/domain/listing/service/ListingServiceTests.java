package com.dsm9.kolpop.domain.listing.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import com.dsm9.kolpop.domain.auth.repository.UserRepository;
import com.dsm9.kolpop.domain.listing.dto.CloseListingResponse;
import com.dsm9.kolpop.domain.listing.dto.CreateListingRequest;
import com.dsm9.kolpop.domain.listing.dto.CreateListingResponse;
import com.dsm9.kolpop.domain.listing.dto.ListingDiscoveryResponse;
import com.dsm9.kolpop.domain.listing.dto.ListingDetailResponse;
import com.dsm9.kolpop.domain.listing.dto.ListingListResponse;
import com.dsm9.kolpop.domain.listing.dto.ListingMapResponse;
import com.dsm9.kolpop.domain.listing.entity.ListingStatus;
import com.dsm9.kolpop.domain.listing.dto.UpdateListingResponse;
import com.dsm9.kolpop.domain.listing.entity.Listing;
import com.dsm9.kolpop.domain.listing.repository.ListingRepository;
import com.dsm9.kolpop.domain.user.entity.User;
import com.dsm9.kolpop.domain.user.entity.UserRole;
import com.dsm9.kolpop.global.exception.BusinessException;

class ListingServiceTests {

    @Test
    void landlordCanCreateListing() {
        ListingRepository listingRepository = mock(ListingRepository.class);
        UserRepository userRepository = mock(UserRepository.class);
        ListingService listingService = new ListingService(listingRepository, userRepository);

        when(userRepository.findById(1L)).thenReturn(Optional.of(createUser(1L, UserRole.LANDLORD)));
        when(listingRepository.save(any(Listing.class))).thenAnswer(invocation -> {
            Listing listing = invocation.getArgument(0);
            ReflectionTestUtils.setField(listing, "id", 10L);
            return listing;
        });

        CreateListingResponse response = listingService.createListing(1L, createRequest());

        assertEquals(10L, response.listingId());
        verify(listingRepository).save(any(Listing.class));
    }

    @Test
    void blankImageUrlsAreIgnoredOnCreate() {
        ListingRepository listingRepository = mock(ListingRepository.class);
        UserRepository userRepository = mock(UserRepository.class);
        ListingService listingService = new ListingService(listingRepository, userRepository);

        when(userRepository.findById(1L)).thenReturn(Optional.of(createUser(1L, UserRole.LANDLORD)));
        when(listingRepository.save(any(Listing.class))).thenAnswer(invocation -> {
            Listing listing = invocation.getArgument(0);
            ReflectionTestUtils.setField(listing, "id", 10L);
            return listing;
        });

        listingService.createListing(1L, createRequestWithBlankImageUrl());

        verify(listingRepository).save(any(Listing.class));
    }

    @Test
    void founderCannotCreateListing() {
        ListingRepository listingRepository = mock(ListingRepository.class);
        UserRepository userRepository = mock(UserRepository.class);
        ListingService listingService = new ListingService(listingRepository, userRepository);

        when(userRepository.findById(2L)).thenReturn(Optional.of(createUser(2L, UserRole.FOUNDER)));

        BusinessException exception = assertThrows(
                BusinessException.class,
                () -> listingService.createListing(2L, createRequest())
        );

        assertEquals("LANDLORD_ONLY", exception.getCode());
    }

    @Test
    void ownerCanUpdateListing() {
        ListingRepository listingRepository = mock(ListingRepository.class);
        UserRepository userRepository = mock(UserRepository.class);
        ListingService listingService = new ListingService(listingRepository, userRepository);
        User landlord = createUser(1L, UserRole.LANDLORD);
        Listing listing = createListing(landlord);
        ReflectionTestUtils.setField(listing, "id", 11L);

        when(userRepository.findById(1L)).thenReturn(Optional.of(landlord));
        when(listingRepository.findById(11L)).thenReturn(Optional.of(listing));

        UpdateListingResponse response = listingService.updateListing(1L, 11L, createUpdatedRequest());

        assertEquals(11L, response.listingId());
        assertEquals("성수동 전시 공간", listing.getTitle());
        assertEquals("서울 성동구 성수이로 87 1층", listing.getAddress() + " " + listing.getDetailAddress());
        assertEquals(220000L, listing.getDailyFee());
        assertEquals(2, listing.getImageUrls().size());
    }

    @Test
    void otherLandlordCannotUpdateListing() {
        ListingRepository listingRepository = mock(ListingRepository.class);
        UserRepository userRepository = mock(UserRepository.class);
        ListingService listingService = new ListingService(listingRepository, userRepository);
        User owner = createUser(1L, UserRole.LANDLORD);
        User anotherLandlord = createUser(2L, UserRole.LANDLORD);
        Listing listing = createListing(owner);
        ReflectionTestUtils.setField(listing, "id", 12L);

        when(userRepository.findById(2L)).thenReturn(Optional.of(anotherLandlord));
        when(listingRepository.findById(12L)).thenReturn(Optional.of(listing));

        BusinessException exception = assertThrows(
                BusinessException.class,
                () -> listingService.updateListing(2L, 12L, createUpdatedRequest())
        );

        assertEquals("LISTING_UPDATE_FORBIDDEN", exception.getCode());
    }

    @Test
    void ownerCanCloseListing() {
        ListingRepository listingRepository = mock(ListingRepository.class);
        UserRepository userRepository = mock(UserRepository.class);
        ListingService listingService = new ListingService(listingRepository, userRepository);
        User landlord = createUser(1L, UserRole.LANDLORD);
        Listing listing = createListing(landlord);
        ReflectionTestUtils.setField(listing, "id", 13L);

        when(userRepository.findById(1L)).thenReturn(Optional.of(landlord));
        when(listingRepository.findById(13L)).thenReturn(Optional.of(listing));

        CloseListingResponse response = listingService.closeListing(1L, 13L);

        assertEquals(13L, response.listingId());
        assertEquals(ListingStatus.CLOSED, listing.getStatus());
        assertEquals("모집종료", response.status().label());
    }

    @Test
    void otherLandlordCannotCloseListing() {
        ListingRepository listingRepository = mock(ListingRepository.class);
        UserRepository userRepository = mock(UserRepository.class);
        ListingService listingService = new ListingService(listingRepository, userRepository);
        User owner = createUser(1L, UserRole.LANDLORD);
        User anotherLandlord = createUser(2L, UserRole.LANDLORD);
        Listing listing = createListing(owner);
        ReflectionTestUtils.setField(listing, "id", 14L);

        when(userRepository.findById(2L)).thenReturn(Optional.of(anotherLandlord));
        when(listingRepository.findById(14L)).thenReturn(Optional.of(listing));

        BusinessException exception = assertThrows(
                BusinessException.class,
                () -> listingService.closeListing(2L, 14L)
        );

        assertEquals("LISTING_CLOSE_FORBIDDEN", exception.getCode());
    }

    @Test
    void ownerCanDeleteListing() {
        ListingRepository listingRepository = mock(ListingRepository.class);
        UserRepository userRepository = mock(UserRepository.class);
        ListingService listingService = new ListingService(listingRepository, userRepository);
        User landlord = createUser(1L, UserRole.LANDLORD);
        Listing listing = createListing(landlord);
        ReflectionTestUtils.setField(listing, "id", 20L);

        when(userRepository.findById(1L)).thenReturn(Optional.of(landlord));
        when(listingRepository.findById(20L)).thenReturn(Optional.of(listing));

        listingService.deleteListing(1L, 20L);

        verify(listingRepository).delete(listing);
    }

    @Test
    void otherLandlordCannotDeleteListing() {
        ListingRepository listingRepository = mock(ListingRepository.class);
        UserRepository userRepository = mock(UserRepository.class);
        ListingService listingService = new ListingService(listingRepository, userRepository);
        User owner = createUser(1L, UserRole.LANDLORD);
        User anotherLandlord = createUser(2L, UserRole.LANDLORD);
        Listing listing = createListing(owner);
        ReflectionTestUtils.setField(listing, "id", 20L);

        when(userRepository.findById(2L)).thenReturn(Optional.of(anotherLandlord));
        when(listingRepository.findById(20L)).thenReturn(Optional.of(listing));

        BusinessException exception = assertThrows(
                BusinessException.class,
                () -> listingService.deleteListing(2L, 20L)
        );

        assertEquals("LISTING_DELETE_FORBIDDEN", exception.getCode());
    }

    @Test
    void invalidOperatingDaysAreRejected() {
        ListingRepository listingRepository = mock(ListingRepository.class);
        UserRepository userRepository = mock(UserRepository.class);
        ListingService listingService = new ListingService(listingRepository, userRepository);

        when(userRepository.findById(1L)).thenReturn(Optional.of(createUser(1L, UserRole.LANDLORD)));

        CreateListingRequest invalidRequest = new CreateListingRequest(
                "홍대 팝업 공간",
                List.of("https://cdn.example.com/listings/a.jpg"),
                "서울 마포구 와우산로 00",
                "1층",
                new BigDecimal("37.5551000"),
                new BigDecimal("126.9235000"),
                150000L,
                1000000L,
                new BigDecimal("66.1"),
                List.of("와이파이"),
                List.of("없음"),
                List.of(),
                LocalDate.of(2026, 7, 20),
                LocalDate.of(2026, 7, 25),
                10,
                7,
                "설명이 들어갑니다.",
                List.of("#홍대")
        );

        BusinessException exception = assertThrows(
                BusinessException.class,
                () -> listingService.createListing(1L, invalidRequest)
        );

        assertEquals("INVALID_OPERATING_DAYS", exception.getCode());
    }

    @Test
    void blankOnlyImageUrlsAreRejected() {
        ListingRepository listingRepository = mock(ListingRepository.class);
        UserRepository userRepository = mock(UserRepository.class);
        ListingService listingService = new ListingService(listingRepository, userRepository);

        when(userRepository.findById(1L)).thenReturn(Optional.of(createUser(1L, UserRole.LANDLORD)));

        BusinessException exception = assertThrows(
                BusinessException.class,
                () -> listingService.createListing(1L, createRequestWithOnlyBlankImageUrls())
        );

        assertEquals("IMAGE_URL_REQUIRED", exception.getCode());
    }

    @Test
    void mapQueryReturnsListingsWithinBounds() {
        ListingRepository listingRepository = mock(ListingRepository.class);
        UserRepository userRepository = mock(UserRepository.class);
        ListingService listingService = new ListingService(listingRepository, userRepository);
        Listing listing = createListing(createUser(1L, UserRole.LANDLORD));
        ReflectionTestUtils.setField(listing, "id", 30L);

        when(listingRepository.findAllByStatusAndLatitudeBetweenAndLongitudeBetweenOrderByCreatedAtDesc(
                ListingStatus.RECRUITING,
                new BigDecimal("37.5000"),
                new BigDecimal("37.6000"),
                new BigDecimal("126.9000"),
                new BigDecimal("127.0000")
        )).thenReturn(List.of(listing));

        ListingMapResponse response = listingService.getListingsForMap(
                new BigDecimal("37.5000"),
                new BigDecimal("37.6000"),
                new BigDecimal("126.9000"),
                new BigDecimal("127.0000")
        );

        assertEquals(1, response.count());
        assertEquals(30L, response.listings().getFirst().listingId());
        assertEquals("모집중", response.listings().getFirst().status().label());
    }

    @Test
    void publicListingQueryReturnsSummaries() {
        ListingRepository listingRepository = mock(ListingRepository.class);
        UserRepository userRepository = mock(UserRepository.class);
        ListingService listingService = new ListingService(listingRepository, userRepository);
        Listing listing = createListing(createUser(1L, UserRole.LANDLORD));
        ReflectionTestUtils.setField(listing, "id", 31L);

        when(listingRepository.findAllByStatusOrderByCreatedAtDesc(ListingStatus.RECRUITING)).thenReturn(List.of(listing));

        ListingListResponse response = listingService.getListings(null, null, null, null);

        assertEquals(1, response.count());
        assertEquals(31L, response.listings().getFirst().listingId());
        assertEquals("https://cdn.example.com/listings/1.jpg", response.listings().getFirst().thumbnailUrl());
    }

    @Test
    void discoveryQueryReturnsMapAndNearbyListingsTogether() {
        ListingRepository listingRepository = mock(ListingRepository.class);
        UserRepository userRepository = mock(UserRepository.class);
        ListingService listingService = new ListingService(listingRepository, userRepository);
        Listing listing = createListing(createUser(1L, UserRole.LANDLORD));
        ReflectionTestUtils.setField(listing, "id", 34L);

        when(listingRepository.findAllByStatusAndLatitudeBetweenAndLongitudeBetweenOrderByCreatedAtDesc(
                ListingStatus.RECRUITING,
                new BigDecimal("37.5000"),
                new BigDecimal("37.6000"),
                new BigDecimal("126.9000"),
                new BigDecimal("127.0000")
        )).thenReturn(List.of(listing));

        ListingDiscoveryResponse response = listingService.getListingsForDiscovery(
                new BigDecimal("37.5000"),
                new BigDecimal("37.6000"),
                new BigDecimal("126.9000"),
                new BigDecimal("127.0000"),
                null,
                null
        );

        assertEquals(1, response.map().count());
        assertEquals(1, response.nearbyListings().count());
        assertEquals(34L, response.map().listings().getFirst().listingId());
        assertEquals("https://cdn.example.com/listings/1.jpg", response.nearbyListings().listings().getFirst().thumbnailUrl());
    }

    @Test
    void myListingsQueryReturnsOwnersListings() {
        ListingRepository listingRepository = mock(ListingRepository.class);
        UserRepository userRepository = mock(UserRepository.class);
        ListingService listingService = new ListingService(listingRepository, userRepository);
        User landlord = createUser(1L, UserRole.LANDLORD);
        Listing listing = createListing(landlord);
        ReflectionTestUtils.setField(listing, "id", 32L);

        when(userRepository.findById(1L)).thenReturn(Optional.of(landlord));
        when(listingRepository.findAllByLandlordIdOrderByCreatedAtDesc(1L)).thenReturn(List.of(listing));

        ListingListResponse response = listingService.getMyListings(1L);

        assertEquals(1, response.count());
        assertEquals(32L, response.listings().getFirst().listingId());
    }

    @Test
    void detailQueryIncreasesViewCount() {
        ListingRepository listingRepository = mock(ListingRepository.class);
        UserRepository userRepository = mock(UserRepository.class);
        ListingService listingService = new ListingService(listingRepository, userRepository);
        User landlord = createUser(1L, UserRole.LANDLORD);
        Listing listing = createListing(landlord);
        ReflectionTestUtils.setField(listing, "id", 33L);

        when(listingRepository.findById(33L)).thenReturn(Optional.of(listing));

        ListingDetailResponse response = listingService.getListingDetail(33L);

        assertEquals(1L, response.viewCount());
        assertEquals("사용자1", response.landlordName());
        assertFalse(response.imageUrls().isEmpty());
    }

    private CreateListingRequest createRequest() {
        return new CreateListingRequest(
                "홍대입구역 1층 팝업 공간",
                List.of(
                        "https://cdn.example.com/listings/1.jpg",
                        "https://cdn.example.com/listings/2.jpg"
                ),
                "서울 마포구 양화로 123",
                "1층",
                new BigDecimal("37.5551000"),
                new BigDecimal("126.9235000"),
                150000L,
                1000000L,
                new BigDecimal("66.1"),
                List.of("와이파이", "테이블", "에어컨"),
                List.of("주류 판매 불가"),
                List.of("외부 간판 설치 불가"),
                LocalDate.of(2026, 7, 20),
                LocalDate.of(2026, 7, 26),
                1,
                7,
                "홍대 메인 거리 인근에 위치한 1층 팝업 공간입니다.",
                List.of("#홍대", "#1층", "#유동인구많음")
        );
    }

    private CreateListingRequest createUpdatedRequest() {
        return new CreateListingRequest(
                "성수동 전시 공간",
                List.of(
                        "https://cdn.example.com/listings/10.jpg",
                        "https://cdn.example.com/listings/11.jpg"
                ),
                "서울 성동구 성수이로 87",
                "1층",
                new BigDecimal("37.5442000"),
                new BigDecimal("127.0557000"),
                220000L,
                1500000L,
                new BigDecimal("82.6"),
                List.of("조명", "피팅룸", "대형거울"),
                List.of("소음 발생 업종 제한"),
                List.of("벽면 타공 불가"),
                LocalDate.of(2026, 8, 1),
                LocalDate.of(2026, 8, 31),
                2,
                10,
                "성수동 메인 골목 인근 전시 공간입니다.",
                List.of("#성수", "#전시", "#팝업")
        );
    }

    private CreateListingRequest createRequestWithBlankImageUrl() {
        return new CreateListingRequest(
                "홍대입구역 1층 팝업 공간",
                List.of(
                        "https://cdn.example.com/listings/1.jpg",
                        "   "
                ),
                "서울 마포구 양화로 123",
                "1층",
                new BigDecimal("37.5551000"),
                new BigDecimal("126.9235000"),
                150000L,
                1000000L,
                new BigDecimal("66.1"),
                List.of("와이파이", "테이블", "에어컨"),
                List.of("주류 판매 불가"),
                List.of("외부 간판 설치 불가"),
                LocalDate.of(2026, 7, 20),
                LocalDate.of(2026, 7, 26),
                1,
                7,
                "홍대 메인 거리 인근에 위치한 1층 팝업 공간입니다.",
                List.of("#홍대", "#1층", "#유동인구많음")
        );
    }

    private CreateListingRequest createRequestWithOnlyBlankImageUrls() {
        return new CreateListingRequest(
                "홍대입구역 1층 팝업 공간",
                List.of("   ", ""),
                "서울 마포구 양화로 123",
                "1층",
                new BigDecimal("37.5551000"),
                new BigDecimal("126.9235000"),
                150000L,
                1000000L,
                new BigDecimal("66.1"),
                List.of("와이파이", "테이블", "에어컨"),
                List.of("주류 판매 불가"),
                List.of("외부 간판 설치 불가"),
                LocalDate.of(2026, 7, 20),
                LocalDate.of(2026, 7, 26),
                1,
                7,
                "홍대 메인 거리 인근에 위치한 1층 팝업 공간입니다.",
                List.of("#홍대", "#1층", "#유동인구많음")
        );
    }

    private Listing createListing(User landlord) {
        return new Listing(
                landlord,
                "홍대입구역 1층 팝업 공간",
                List.of("https://cdn.example.com/listings/1.jpg"),
                "서울 마포구 양화로 123",
                "1층",
                new BigDecimal("37.5551000"),
                new BigDecimal("126.9235000"),
                150000L,
                1000000L,
                new BigDecimal("66.1"),
                List.of("와이파이"),
                List.of("없음"),
                List.of(),
                LocalDate.of(2026, 7, 20),
                LocalDate.of(2026, 7, 26),
                1,
                7,
                "홍대 메인 거리 인근에 위치한 1층 팝업 공간입니다.",
                List.of("#홍대"),
                LocalDateTime.of(2026, 7, 14, 9, 30)
        );
    }

    private User createUser(Long id, UserRole role) {
        User user = new User(
                "login_" + id,
                "사용자" + id,
                "user" + id + "@kolpop.test",
                "encodedPassword",
                "0101234" + String.format("%04d", id),
                role
        );
        ReflectionTestUtils.setField(user, "id", id);
        return user;
    }
}
