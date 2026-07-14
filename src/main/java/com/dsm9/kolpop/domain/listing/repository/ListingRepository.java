package com.dsm9.kolpop.domain.listing.repository;

import java.math.BigDecimal;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.dsm9.kolpop.domain.listing.entity.Listing;
import com.dsm9.kolpop.domain.listing.entity.ListingStatus;

public interface ListingRepository extends JpaRepository<Listing, Long> {

    List<Listing> findAllByStatusOrderByCreatedAtDesc(ListingStatus status);

    List<Listing> findAllByLandlordIdOrderByCreatedAtDesc(Long landlordId);

    List<Listing> findAllByStatusAndLatitudeBetweenAndLongitudeBetweenOrderByCreatedAtDesc(
            ListingStatus status,
            BigDecimal minLatitude,
            BigDecimal maxLatitude,
            BigDecimal minLongitude,
            BigDecimal maxLongitude
    );
}
