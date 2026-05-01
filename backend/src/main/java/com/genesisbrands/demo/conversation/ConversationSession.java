package com.genesisbrands.demo.conversation;

import com.genesisbrands.demo.brand.Brand;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "conversation_sessions")
@Getter
@Setter
public class ConversationSession {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "brand_id", nullable = false)
    private Brand brand;

    @Enumerated(EnumType.STRING)
    private SessionStatus status = SessionStatus.ACTIVE;

    @OneToMany(mappedBy = "session", cascade = CascadeType.ALL, fetch = FetchType.EAGER)
    @OrderBy("sequenceNum ASC")
    private List<ConversationTurn> turns = new ArrayList<>();

    private Instant createdAt = Instant.now();

    public enum SessionStatus { ACTIVE, COMPLETE }
}
