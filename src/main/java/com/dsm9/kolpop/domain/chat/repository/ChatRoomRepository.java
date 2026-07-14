package com.dsm9.kolpop.domain.chat.repository;

import com.dsm9.kolpop.domain.chat.entity.ChatRoom;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ChatRoomRepository extends JpaRepository<ChatRoom, Long> {

    Optional<ChatRoom> findByFounderIdAndListingId(Long founderId, Long listingId);

    List<ChatRoom> findAllByFounderIdOrLandlordIdOrderByCreatedAtDesc(Long founderId, Long landlordId);

    List<ChatRoom> findAllByLandlordIdAndListingIdIn(Long landlordId, List<Long> listingIds);
}
