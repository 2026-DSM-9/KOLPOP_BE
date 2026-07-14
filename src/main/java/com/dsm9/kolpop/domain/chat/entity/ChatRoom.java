package com.dsm9.kolpop.domain.chat.entity;

import com.dsm9.kolpop.domain.user.entity.User;
import com.dsm9.kolpop.domain.listing.entity.Listing;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

import java.time.LocalDateTime;

@Entity
@Table(
        name = "chat_rooms",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_chat_rooms_founder_landlord_listing",
                columnNames = {"founder_id", "landlord_id", "listing_id"}
        )
)
public class ChatRoom {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "founder_id", nullable = false)
    private User founder;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "landlord_id", nullable = false)
    private User landlord;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "listing_id")
    private Listing listing;

    @Column(nullable = false)
    private LocalDateTime createdAt;

    @Enumerated(EnumType.STRING)
    @Column(length = 20)
    private ChatRoomStatus status;

    private LocalDateTime acceptedAt;

    protected ChatRoom() {
    }

    public ChatRoom(User founder, User landlord, Listing listing) {
        this.founder = founder;
        this.landlord = landlord;
        this.listing = listing;
        this.createdAt = LocalDateTime.now();
        this.status = ChatRoomStatus.PENDING;
    }

    public Long getId() {
        return id;
    }

    public User getFounder() {
        return founder;
    }

    public User getLandlord() {
        return landlord;
    }

    public Listing getListing() {
        return listing;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public ChatRoomStatus getStatus() {
        return status == null ? ChatRoomStatus.ACCEPTED : status;
    }

    public LocalDateTime getAcceptedAt() {
        return acceptedAt;
    }

    public boolean isPending() {
        return getStatus() == ChatRoomStatus.PENDING;
    }

    public boolean isAccepted() {
        return getStatus() == ChatRoomStatus.ACCEPTED;
    }

    public void accept(LocalDateTime acceptedAt) {
        this.status = ChatRoomStatus.ACCEPTED;
        this.acceptedAt = acceptedAt;
    }

    public boolean hasParticipant(Long userId) {
        return founder.getId().equals(userId) || landlord.getId().equals(userId);
    }
}
