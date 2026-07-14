package com.dsm9.kolpop.domain.listing.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import com.dsm9.kolpop.domain.auth.repository.UserRepository;
import com.dsm9.kolpop.domain.listing.dto.ListingAddressSuggestionResponse;
import com.dsm9.kolpop.domain.listing.dto.ListingMapResponse;
import com.dsm9.kolpop.domain.listing.entity.Listing;
import com.dsm9.kolpop.domain.listing.entity.ListingStatus;
import com.dsm9.kolpop.domain.listing.repository.ListingRepository;
import com.dsm9.kolpop.domain.user.entity.User;
import com.dsm9.kolpop.domain.user.entity.UserRole;

class ListingSearchServiceTests {

    @Test
    void mapSearchReturnsListingsByRegionOrAddressKeyword() {
        ListingRepository listingRepository = mock(ListingRepository.class);
        UserRepository userRepository = mock(UserRepository.class);
        ListingService listingService = new ListingService(listingRepository, userRepository);
        Listing listing = createListing(10L, "Daejeon Seo-gu", "1F");

        when(listingRepository.findAllByStatusAndKeywordOrderByCreatedAtDesc(
                ListingStatus.RECRUITING,
                "Daejeon"
        )).thenReturn(List.of(listing));

        ListingMapResponse response = listingService.getListingsForMap(null, null, null, null, " Daejeon ");

        assertEquals(1, response.count());
        assertEquals(10L, response.listings().getFirst().listingId());
        assertEquals(new BigDecimal("36.3500000"), response.listings().getFirst().latitude());
        verify(listingRepository).findAllByStatusAndKeywordOrderByCreatedAtDesc(ListingStatus.RECRUITING, "Daejeon");
    }

    @Test
    void addressSuggestionsReturnUniqueFullAddresses() {
        ListingRepository listingRepository = mock(ListingRepository.class);
        UserRepository userRepository = mock(UserRepository.class);
        ListingService listingService = new ListingService(listingRepository, userRepository);
        Listing firstListing = createListing(10L, "Daejeon Seo-gu", "1F");
        Listing duplicateAddressListing = createListing(11L, "Daejeon Seo-gu", "1F");
        Listing secondListing = createListing(12L, "Daejeon Yuseong-gu", null);

        when(listingRepository.findAllByStatusAndKeywordOrderByCreatedAtDesc(
                ListingStatus.RECRUITING,
                "Daejeon"
        )).thenReturn(List.of(firstListing, duplicateAddressListing, secondListing));

        List<ListingAddressSuggestionResponse> response = listingService.getAddressSuggestions("Daejeon", 5);

        assertEquals(2, response.size());
        assertEquals("Daejeon Seo-gu 1F", response.get(0).fullAddress());
        assertEquals("Daejeon Yuseong-gu", response.get(1).fullAddress());
    }

    private Listing createListing(Long id, String address, String detailAddress) {
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
                "Pop-up space",
                List.of("https://cdn.example.com/listing.jpg"),
                address,
                detailAddress,
                new BigDecimal("36.3500000"),
                new BigDecimal("127.3845000"),
                80000L,
                1000000L,
                new BigDecimal("40.0"),
                List.of(),
                List.of(),
                List.of(),
                LocalDate.of(2026, 7, 20),
                LocalDate.of(2026, 8, 10),
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
