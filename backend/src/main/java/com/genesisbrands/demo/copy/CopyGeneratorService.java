package com.genesisbrands.demo.copy;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.genesisbrands.demo.brand.BrandDNA;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class CopyGeneratorService {

    private static final String SYSTEM_PROMPT = """
            You are an expert brand copywriter. Given brand signals, generate compelling, distinctive brand copy.
            Reply with valid JSON only — no markdown, no explanation:

            {
              "tagline": "under 8 words, punchy and memorable",
              "missionStatement": "one sentence — what you do, for whom, and why it matters",
              "brandStory": "2-3 sentences — the human story behind the brand",
              "elevatorPitch": "30-second spoken pitch, natural and conversational",
              "toneGuide": "3-4 sentences describing how the brand communicates, with specific dos and don'ts"
            }

            Make the copy distinctive and specific to this brand — avoid generic phrases like 'innovative solutions'.
            """;

    private final ChatClient.Builder chatClientBuilder;
    private final ObjectMapper objectMapper;

    public void generateCopy(BrandDNA dna) {
        String signalJson = toJson(dna);

        String raw = chatClientBuilder.build()
                .prompt()
                .system(SYSTEM_PROMPT)
                .user("Brand signals:\n" + signalJson)
                .call()
                .content();

        try {
            CopyOutput output = objectMapper.readValue(raw, CopyOutput.class);
            dna.setTagline(output.tagline());
            dna.setMissionStatement(output.missionStatement());
            dna.setBrandStory(output.brandStory());
            dna.setElevatorPitch(output.elevatorPitch());
            dna.setToneGuide(output.toneGuide());
        } catch (Exception e) {
            log.error("Failed to parse copy generation response", e);
            throw new RuntimeException("Copy generation failed", e);
        }
    }

    private String toJson(BrandDNA dna) {
        try {
            return objectMapper.writeValueAsString(dna);
        } catch (Exception e) {
            return dna.toString();
        }
    }

    record CopyOutput(
            String tagline,
            String missionStatement,
            String brandStory,
            String elevatorPitch,
            String toneGuide
    ) {}
}
