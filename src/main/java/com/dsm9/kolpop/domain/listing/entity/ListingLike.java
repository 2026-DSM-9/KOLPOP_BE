package com.dsm9.kolpop.domain.listing.entity;

import java.time.LocalDateTime;

import com.dsm9.kolpop.domain.user.entity.User;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

@Entity
@Table(
        name = "listing_likes",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_listing_likes_listing_user",
                columnNames = {"listing_id", "user_id"}
        )
)
public class ListingLike {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "listing_id", nullable = false)
    private Listing listing;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    protected ListingLike() {
    }

    public ListingLike(Listing listing, User user, LocalDateTime createdAt) {
        this.listing = listing;
        this.user = user;
        this.createdAt = createdAt;
    }

    public Long getId() {
        return id;
    }

    public Listing getListing() {
        return listing;
    }

    public User getUser() {
        return user;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }
}
