package com.dsm9.kolpop.domain.ai.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.dsm9.kolpop.domain.ai.entity.AiConversation;

public interface AiConversationRepository extends JpaRepository<AiConversation, Long> {

    List<AiConversation> findAllByUserIdOrderByCreatedAtDesc(Long userId);

    Optional<AiConversation> findByIdAndUserId(Long id, Long userId);
}
