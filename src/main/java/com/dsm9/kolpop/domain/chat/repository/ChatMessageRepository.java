package com.dsm9.kolpop.domain.chat.repository;

import com.dsm9.kolpop.domain.chat.entity.ChatMessage;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ChatMessageRepository extends JpaRepository<ChatMessage, Long> {

    List<ChatMessage> findAllByRoomIdOrderByCreatedAtAsc(Long roomId);
}
