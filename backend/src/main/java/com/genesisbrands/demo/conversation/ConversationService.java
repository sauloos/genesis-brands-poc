package com.genesisbrands.demo.conversation;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.genesisbrands.demo.brand.Brand;
import com.genesisbrands.demo.brand.BrandDNA;
import com.genesisbrands.demo.brand.BrandRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class ConversationService {

    private static final String SYSTEM_PROMPT = """
            You are a brand strategist conducting an initial discovery session with a startup founder.
            Your job is to understand their business well enough to generate a distinctive brand identity.

            After each founder response, extract brand signals and decide what to ask next.
            Always reply with valid JSON and nothing else — no markdown, no explanation, just JSON:

            {
              "extractedSignals": {
                "businessDescription": "concise description of what they do and who they help (null if not yet known)",
                "targetAudience": "their ideal customer (null if not yet known)",
                "personality": "2-3 brand personality keywords, e.g. bold, nurturing, technical (null if not yet known)",
                "toneOfVoice": "how they communicate, e.g. conversational, authoritative, playful (null if not yet known)",
                "differentiators": ["what makes them different from alternatives"],
                "aspirations": "where they want to be in 3-5 years (null if not yet known)"
              },
              "isComplete": false,
              "nextQuestion": "Your next focused, open-ended question for the founder"
            }

            Rules:
            - Set isComplete to true only when businessDescription, targetAudience, personality, and toneOfVoice are all filled in.
            - Ask one question at a time. Be conversational, not clinical.
            - Typically 3-5 exchanges is enough. Do not drag it out.
            - When asking follow-ups, briefly acknowledge what they said first.
            """;

    private final ChatClient.Builder chatClientBuilder;
    private final BrandRepository brandRepository;
    private final ConversationSessionRepository sessionRepository;
    private final ObjectMapper objectMapper;

    @Transactional
    public StartResponse start(UUID brandId) {
        Brand brand = brandRepository.findById(brandId)
                .orElseThrow(() -> new IllegalArgumentException("Brand not found: " + brandId));

        ConversationSession session = new ConversationSession();
        session.setBrand(brand);

        String firstQuestion = "Tell me about your business — what do you do, and who do you help?";

        ConversationTurn turn = new ConversationTurn();
        turn.setSession(session);
        turn.setSequenceNum(1);
        turn.setQuestion(firstQuestion);
        session.getTurns().add(turn);

        sessionRepository.save(session);

        return new StartResponse(session.getId(), firstQuestion);
    }

    @Transactional
    public AnswerResponse answer(UUID sessionId, String answer) {
        ConversationSession session = sessionRepository.findById(sessionId)
                .orElseThrow(() -> new IllegalArgumentException("Session not found: " + sessionId));

        // Record the answer on the latest unanswered turn
        ConversationTurn latest = session.getTurns().getLast();
        latest.setAnswer(answer);

        // Build conversation history for the LLM
        String history = buildHistory(session.getTurns());

        // Call LLM
        LlmResponse llmResponse = callLlm(history);

        // Update BrandDNA signals on the brand
        Brand brand = session.getBrand();
        BrandDNA dna = brand.getBrandDna() != null ? brand.getBrandDna() : new BrandDNA();
        applySignals(dna, llmResponse.extractedSignals());
        brand.setBrandDna(dna);
        brandRepository.save(brand);

        if (llmResponse.isComplete()) {
            session.setStatus(ConversationSession.SessionStatus.COMPLETE);
            sessionRepository.save(session);
            return new AnswerResponse(null, true, session.getBrand().getId());
        }

        // Add next question as a new turn
        ConversationTurn nextTurn = new ConversationTurn();
        nextTurn.setSession(session);
        nextTurn.setSequenceNum(latest.getSequenceNum() + 1);
        nextTurn.setQuestion(llmResponse.nextQuestion());
        session.getTurns().add(nextTurn);
        sessionRepository.save(session);

        return new AnswerResponse(llmResponse.nextQuestion(), false, null);
    }

    @Transactional(readOnly = true)
    public BrandDNA getSignals(UUID sessionId) {
        ConversationSession session = sessionRepository.findById(sessionId)
                .orElseThrow(() -> new IllegalArgumentException("Session not found: " + sessionId));
        Brand brand = session.getBrand();
        return brand.getBrandDna() != null ? brand.getBrandDna() : new BrandDNA();
    }

    private String buildHistory(List<ConversationTurn> turns) {
        return turns.stream()
                .filter(t -> t.getAnswer() != null)
                .map(t -> "Q: " + t.getQuestion() + "\nA: " + t.getAnswer())
                .collect(Collectors.joining("\n\n"));
    }

    private LlmResponse callLlm(String history) {
        ChatClient client = chatClientBuilder.build();
        String raw = client.prompt()
                .system(SYSTEM_PROMPT)
                .user("Conversation so far:\n\n" + history + "\n\nExtract updated signals and provide the next question.")
                .call()
                .content();

        try {
            return objectMapper.readValue(raw, LlmResponse.class);
        } catch (Exception e) {
            log.warn("Failed to parse LLM response as JSON, using fallback. Response: {}", raw);
            return new LlmResponse(new ExtractedSignals(null, null, null, null, List.of(), null),
                    false, "Can you tell me more about your target customers?");
        }
    }

    private void applySignals(BrandDNA dna, ExtractedSignals signals) {
        if (signals.businessDescription() != null) dna.setBusinessDescription(signals.businessDescription());
        if (signals.targetAudience() != null)      dna.setTargetAudience(signals.targetAudience());
        if (signals.personality() != null)         dna.setPersonality(signals.personality());
        if (signals.toneOfVoice() != null)         dna.setToneOfVoice(signals.toneOfVoice());
        if (signals.differentiators() != null && !signals.differentiators().isEmpty())
            dna.setDifferentiators(signals.differentiators());
        if (signals.aspirations() != null)         dna.setAspirations(signals.aspirations());
    }

    // ── Internal DTOs (LLM JSON shape) ───────────────────────────────────────

    record LlmResponse(ExtractedSignals extractedSignals, boolean isComplete, String nextQuestion) {}

    record ExtractedSignals(
            String businessDescription,
            String targetAudience,
            String personality,
            String toneOfVoice,
            List<String> differentiators,
            String aspirations
    ) {}

    // ── Public response DTOs ──────────────────────────────────────────────────

    public record StartResponse(UUID sessionId, String firstQuestion) {}

    public record AnswerResponse(String nextQuestion, boolean complete, UUID brandId) {}
}
