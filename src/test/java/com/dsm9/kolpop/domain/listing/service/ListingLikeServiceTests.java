package com.dsm9.kolpop.domain.listing.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
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
import com.dsm9.kolpop.domain.listing.dto.LikeListingResponse;
import com.dsm9.kolpop.domain.listing.dto.ListingListResponse;
import com.dsm9.kolpop.domain.listing.entity.Listing;
import com.dsm9.kolpop.domain.listing.entity.ListingLike;
import com.dsm9.kolpop.domain.listing.entity.ListingStatus;
import com.dsm9.kolpop.domain.listing.repository.ListingLikeCount;
import com.dsm9.kolpop.domain.listing.repository.ListingLikeRepository;
import com.dsm9.kolpop.domain.listing.repository.ListingRepository;
import com.dsm9.kolpop.domain.user.entity.User;
import com.dsm9.kolpop.domain.user.entity.UserRole;

class ListingLikeServiceTests {

    @Test
    void popularListingQueryReturnsLikeCounts() {
        ListingRepository listingRepository = mock(ListingRepository.class);
        ListingLikeRepository listingLikeRepository = mock(ListingLikeRepository.class);
        UserRepository userRepository = mock(UserRepository.class);
        ListingService listingService = new ListingService(listingRepository, listingLikeRepository, userRepository);
        Listing firstListing = createListing(1L, createUser(1L, UserRole.LANDLORD));
        Listing secondListing = createListing(2L, createUser(2L, UserRole.LANDLORD));

        when(listingRepository.findAllByStatusOrderByLikeCountDesc(ListingStatus.RECRUITING))
                .thenReturn(List.of(firstListing, secondListing));
        when(listingLikeRepository.countByListingIds(List.of(1L, 2L)))
                .thenReturn(List.of(likeCount(1L, 5L), likeCount(2L, 2L)));

        ListingListResponse response = listingService.getListings(null, null, null, null, "popular");

        assertEquals(2, response.count());
        assertEquals(5L, response.listings().get(0).likeCount());
        assertEquals(2L, response.listings().get(1).likeCount());
        verify(listingRepository).findAllByStatusOrderByLikeCountDesc(ListingStatus.RECRUITING);
    }

    @Test
    void userCanLikeListing() {
        ListingRepository listingRepository = mock(ListingRepository.class);
        ListingLikeRepository listingLikeRepository = mock(ListingLikeRepository.class);
        UserRepository userRepository = mock(UserRepository.class);
        ListingService listingService = new ListingService(listingRepository, listingLikeRepository, userRepository);
        User user = createUser(3L, UserRole.FOUNDER);
        Listing listing = createListing(10L, createUser(1L, UserRole.LANDLORD));

        when(userRepository.findById(3L)).thenReturn(Optional.of(user));
        when(listingRepository.findById(10L)).thenReturn(Optional.of(listing));
        when(listingLikeRepository.existsByListingIdAndUserId(10L, 3L)).thenReturn(false);
        when(listingLikeRepository.countByListingId(10L)).thenReturn(1L);

        LikeListingResponse response = listingService.likeListing(3L, 10L);

        assertEquals(10L, response.listingId());
        assertEquals(1L, response.likeCount());
        verify(listingLikeRepository).save(any(ListingLike.class));
    }

    private ListingLikeCount likeCount(Long listingId, Long likeCount) {
        return new ListingLikeCount() {
            @Override
            public Long getListingId() {
                return listingId;
            }

            @Override
            public Long getLikeCount() {
                return likeCount;
            }
        };
    }

    private Listing createListing(Long id, User landlord) {
        Listing listing = new Listing(
                landlord,
                "Test listing",
                List.of("https://cdn.example.com/listing.jpg"),
                "Seoul",
                "1F",
                new BigDecimal("37.5551000"),
                new BigDecimal("126.9235000"),
                150000L,
                1000000L,
                new BigDecimal("66.1"),
                List.of("Wi-Fi"),
                List.of(),
                List.of(),
                LocalDate.of(2026, 7, 20),
                LocalDate.of(2026, 7, 26),
                1,
                7,
                "Test description",
                List.of("#test"),
                LocalDateTime.of(2026, 7, 14, 9, 30)
        );
        ReflectionTestUtils.setField(listing, "id", id);
        return listing;
    }

    private User createUser(Long id, UserRole role) {
        User user = new User(
                "login_" + id,
                "User " + id,
                "user" + id + "@kolpop.test",
                "encodedPassword",
                "0101234" + String.format("%04d", id),
                role
        );
        ReflectionTestUtils.setField(user, "id", id);
        return user;
    }
}
