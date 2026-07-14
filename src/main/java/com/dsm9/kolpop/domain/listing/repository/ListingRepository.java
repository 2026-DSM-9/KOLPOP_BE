package com.dsm9.kolpop.domain.listing.repository;

import java.math.BigDecimal;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.dsm9.kolpop.domain.listing.entity.Listing;

public interface ListingRepository extends JpaRepository<Listing, Long> {

    List<Listing> findAllByOrderByCreatedAtDesc();

    List<Listing> findAllByLandlordIdOrderByCreatedAtDesc(Long landlordId);

    List<Listing> findAllByLatitudeBetweenAndLongitudeBetweenOrderByCreatedAtDesc(
            BigDecimal minLatitude,
            BigDecimal maxLatitude,
            BigDecimal minLongitude,
            BigDecimal maxLongitude
    );
}
