package com.dsm9.kolpop.domain.listing.repository;

import java.math.BigDecimal;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.dsm9.kolpop.domain.listing.entity.Listing;
import com.dsm9.kolpop.domain.listing.entity.ListingStatus;

public interface ListingRepository extends JpaRepository<Listing, Long> {

    List<Listing> findAllByStatusOrderByCreatedAtDesc(ListingStatus status);

    @Query("""
            select l
            from Listing l
            left join ListingLike ll on ll.listing = l
            where l.status = :status
            group by l
            order by count(ll.id) desc, l.createdAt desc
            """)
    List<Listing> findAllByStatusOrderByLikeCountDesc(@Param("status") ListingStatus status);

    List<Listing> findAllByLandlordIdOrderByCreatedAtDesc(Long landlordId);

    List<Listing> findAllByStatusAndLatitudeBetweenAndLongitudeBetweenOrderByCreatedAtDesc(
            ListingStatus status,
            BigDecimal minLatitude,
            BigDecimal maxLatitude,
            BigDecimal minLongitude,
            BigDecimal maxLongitude
    );

    @Query("""
            select l
            from Listing l
            where l.status = :status
              and (
                lower(l.address) like lower(concat('%', :keyword, '%'))
                or lower(coalesce(l.detailAddress, '')) like lower(concat('%', :keyword, '%'))
                or lower(l.title) like lower(concat('%', :keyword, '%'))
              )
            order by l.createdAt desc
            """)
    List<Listing> findAllByStatusAndKeywordOrderByCreatedAtDesc(
            @Param("status") ListingStatus status,
            @Param("keyword") String keyword
    );

    @Query("""
            select l
            from Listing l
            where l.status = :status
              and l.latitude between :minLatitude and :maxLatitude
              and l.longitude between :minLongitude and :maxLongitude
              and (
                lower(l.address) like lower(concat('%', :keyword, '%'))
                or lower(coalesce(l.detailAddress, '')) like lower(concat('%', :keyword, '%'))
                or lower(l.title) like lower(concat('%', :keyword, '%'))
              )
            order by l.createdAt desc
            """)
    List<Listing> findAllByStatusAndBoundsAndKeywordOrderByCreatedAtDesc(
            @Param("status") ListingStatus status,
            @Param("minLatitude") BigDecimal minLatitude,
            @Param("maxLatitude") BigDecimal maxLatitude,
            @Param("minLongitude") BigDecimal minLongitude,
            @Param("maxLongitude") BigDecimal maxLongitude,
            @Param("keyword") String keyword
    );

    @Query("""
            select l
            from Listing l
            left join ListingLike ll on ll.listing = l
            where l.status = :status
              and l.latitude between :minLatitude and :maxLatitude
              and l.longitude between :minLongitude and :maxLongitude
            group by l
            order by count(ll.id) desc, l.createdAt desc
            """)
    List<Listing> findAllByStatusAndBoundsOrderByLikeCountDesc(
            @Param("status") ListingStatus status,
            @Param("minLatitude") BigDecimal minLatitude,
            @Param("maxLatitude") BigDecimal maxLatitude,
            @Param("minLongitude") BigDecimal minLongitude,
            @Param("maxLongitude") BigDecimal maxLongitude
    );
}
