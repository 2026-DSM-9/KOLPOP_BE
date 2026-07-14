package com.dsm9.kolpop.domain.listing.repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.dsm9.kolpop.domain.listing.entity.ListingLike;

public interface ListingLikeRepository extends JpaRepository<ListingLike, Long> {

    boolean existsByListingIdAndUserId(Long listingId, Long userId);

    Optional<ListingLike> findByListingIdAndUserId(Long listingId, Long userId);

    List<ListingLike> findAllByUserIdOrderByCreatedAtDesc(Long userId);

    long countByListingId(Long listingId);

    @Query("""
            select ll.listing.id as listingId, count(ll.id) as likeCount
            from ListingLike ll
            where ll.listing.id in :listingIds
            group by ll.listing.id
            """)
    List<ListingLikeCount> countByListingIds(@Param("listingIds") Collection<Long> listingIds);
}
