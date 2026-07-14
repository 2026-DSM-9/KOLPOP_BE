package com.dsm9.kolpop.domain.reservation.entity;

import java.time.LocalDate;
import java.time.LocalDateTime;

import com.dsm9.kolpop.domain.listing.entity.Listing;
import com.dsm9.kolpop.domain.user.entity.User;

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

@Entity
@Table(name = "reservations")
public class Reservation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "listing_id", nullable = false)
    private Listing listing;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "founder_id", nullable = false)
    private User founder;

    @Column(name = "start_date", nullable = false)
    private LocalDate startDate;

    @Column(name = "end_date", nullable = false)
    private LocalDate endDate;

    @Column(name = "usage_days", nullable = false)
    private Integer usageDays;

    @Column(nullable = false, length = 500)
    private String message;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ReservationStatus status = ReservationStatus.PENDING;

    @Column(name = "applied_at", nullable = false)
    private LocalDateTime appliedAt;

    @Column(name = "decided_at")
    private LocalDateTime decidedAt;

    protected Reservation() {
    }

    public Reservation(
            Listing listing,
            User founder,
            LocalDate startDate,
            LocalDate endDate,
            Integer usageDays,
            String message,
            LocalDateTime appliedAt
    ) {
        this.listing = listing;
        this.founder = founder;
        this.startDate = startDate;
        this.endDate = endDate;
        this.usageDays = usageDays;
        this.message = message;
        this.status = ReservationStatus.PENDING;
        this.appliedAt = appliedAt;
    }

    public Long getId() {
        return id;
    }

    public Listing getListing() {
        return listing;
    }

    public User getFounder() {
        return founder;
    }

    public LocalDate getStartDate() {
        return startDate;
    }

    public LocalDate getEndDate() {
        return endDate;
    }

    public Integer getUsageDays() {
        return usageDays;
    }

    public String getMessage() {
        return message;
    }

    public ReservationStatus getStatus() {
        return status;
    }

    public LocalDateTime getAppliedAt() {
        return appliedAt;
    }

    public LocalDateTime getDecidedAt() {
        return decidedAt;
    }

    public boolean isPending() {
        return status == ReservationStatus.PENDING;
    }

    public boolean isApproved() {
        return status == ReservationStatus.APPROVED;
    }

    public void approve(LocalDateTime decidedAt) {
        this.status = ReservationStatus.APPROVED;
        this.decidedAt = decidedAt;
    }

    public void reject(LocalDateTime decidedAt) {
        this.status = ReservationStatus.REJECTED;
        this.decidedAt = decidedAt;
    }
}
