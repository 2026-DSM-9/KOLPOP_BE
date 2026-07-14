package com.dsm9.kolpop.domain.chat.entity;

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

import java.time.LocalDateTime;

@Entity
@Table(
        name = "chat_rooms",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_chat_rooms_founder_landlord",
                columnNames = {"founder_id", "landlord_id"}
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

    @Column(nullable = false)
    private LocalDateTime createdAt;

    protected ChatRoom() {
    }

    public ChatRoom(User founder, User landlord) {
        this.founder = founder;
        this.landlord = landlord;
        this.createdAt = LocalDateTime.now();
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

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public boolean hasParticipant(Long userId) {
        return founder.getId().equals(userId) || landlord.getId().equals(userId);
    }
}
