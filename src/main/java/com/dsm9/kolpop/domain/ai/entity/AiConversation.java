package com.dsm9.kolpop.domain.ai.entity;

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

@Entity
@Table(name = "ai_conversations")
public class AiConversation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(nullable = false, length = 120)
    private String title;

    @Column(name = "user_message", nullable = false, length = 1000)
    private String userMessage;

    @Column(name = "ai_message", nullable = false, length = 4000)
    private String aiMessage;

    @Column(name = "request_payload", columnDefinition = "TEXT")
    private String requestPayload;

    @Column(name = "response_payload", columnDefinition = "TEXT")
    private String responsePayload;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    protected AiConversation() {
    }

    public AiConversation(
            User user,
            String title,
            String userMessage,
            String aiMessage,
            String requestPayload,
            String responsePayload,
            LocalDateTime createdAt
    ) {
        this.user = user;
        this.title = title;
        this.userMessage = userMessage;
        this.aiMessage = aiMessage;
        this.requestPayload = requestPayload;
        this.responsePayload = responsePayload;
        this.createdAt = createdAt;
    }

    public Long getId() {
        return id;
    }

    public User getUser() {
        return user;
    }

    public String getTitle() {
        return title;
    }

    public String getUserMessage() {
        return userMessage;
    }

    public String getAiMessage() {
        return aiMessage;
    }

    public String getRequestPayload() {
        return requestPayload;
    }

    public String getResponsePayload() {
        return responsePayload;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }
}
