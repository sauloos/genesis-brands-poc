package com.genesisbrands.demo.conversation;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface ConversationTurnRepository extends JpaRepository<ConversationTurn, UUID> {
}
