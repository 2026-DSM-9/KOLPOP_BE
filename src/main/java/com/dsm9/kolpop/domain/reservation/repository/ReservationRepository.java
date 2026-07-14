package com.dsm9.kolpop.domain.reservation.repository;

import java.time.LocalDate;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.dsm9.kolpop.domain.reservation.entity.Reservation;
import com.dsm9.kolpop.domain.reservation.entity.ReservationStatus;

public interface ReservationRepository extends JpaRepository<Reservation, Long> {

    List<Reservation> findAllByListingLandlordIdOrderByAppliedAtDesc(Long landlordId);

    long countByListingId(Long listingId);

    boolean existsByFounderIdAndListingLandlordIdAndStatus(
            Long founderId,
            Long landlordId,
            ReservationStatus status
    );

    boolean existsByFounderIdAndListingIdAndStatus(
            Long founderId,
            Long listingId,
            ReservationStatus status
    );

    @Query("""
            select r.listing.id as listingId, count(r.id) as reservationCount
            from Reservation r
            where r.listing.id in :listingIds
            group by r.listing.id
            """)
    List<ListingReservationCount> countByListingIds(@Param("listingIds") List<Long> listingIds);

    @Query("""
            select count(r.id)
            from Reservation r
            where r.listing.id = :listingId
              and r.status = :status
              and r.startDate <= :endDate
              and r.endDate >= :startDate
              and (:excludeReservationId is null or r.id <> :excludeReservationId)
            """)
    long countOverlappingReservations(
            @Param("listingId") Long listingId,
            @Param("status") ReservationStatus status,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate,
            @Param("excludeReservationId") Long excludeReservationId
    );
}
