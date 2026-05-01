package com.genesisbrands.demo.visual;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.genesisbrands.demo.brand.BrandDNA;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.image.ImageModel;
import org.springframework.ai.image.ImagePrompt;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class VisualGeneratorService {

    private static final String VISUAL_SYSTEM_PROMPT = """
            You are an expert brand designer. Based on brand signals, recommend a colour palette and typography.
            Reply with valid JSON only — no markdown, no explanation:

            {
              "colourPalette": [
                { "hex": "#RRGGBB", "name": "Colour Name", "role": "primary", "rationale": "why this colour suits the brand" },
                { "hex": "#RRGGBB", "name": "Colour Name", "role": "secondary", "rationale": "..." },
                { "hex": "#RRGGBB", "name": "Colour Name", "role": "accent", "rationale": "..." },
                { "hex": "#RRGGBB", "name": "Colour Name", "role": "neutral", "rationale": "..." },
                { "hex": "#RRGGBB", "name": "Colour Name", "role": "background", "rationale": "..." }
              ],
              "typography": {
                "primaryFont": "Font Name (Google Fonts)",
                "primaryUsage": "Headlines, key messaging, CTAs",
                "secondaryFont": "Font Name (Google Fonts)",
                "secondaryUsage": "Body text, captions, supporting copy"
              }
            }

            Always recommend exactly 5 colours. Use only Google Fonts (free to use).
            Choose fonts and colours that are cohesive and reflect the brand personality.
            """;

    private final ChatClient.Builder chatClientBuilder;
    private final ObjectMapper objectMapper;

    @Autowired(required = false)
    private ImageModel imageModel;

    public void generateVisual(BrandDNA dna) {
        generateColoursAndTypography(dna);
        generateLogo(dna);
    }

    private void generateColoursAndTypography(BrandDNA dna) {
        String signalJson = toJson(dna);

        String raw = chatClientBuilder.build()
                .prompt()
                .system(VISUAL_SYSTEM_PROMPT)
                .user("Brand signals:\n" + signalJson)
                .call()
                .content();

        try {
            JsonNode root = objectMapper.readTree(raw);

            List<BrandDNA.ColourSwatch> palette = objectMapper.convertValue(
                    root.get("colourPalette"),
                    new TypeReference<>() {}
            );
            dna.setColourPalette(palette);

            BrandDNA.Typography typography = objectMapper.treeToValue(
                    root.get("typography"),
                    BrandDNA.Typography.class
            );
            dna.setTypography(typography);

        } catch (Exception e) {
            log.error("Failed to parse visual generation response", e);
            throw new RuntimeException("Visual generation failed", e);
        }
    }

    private void generateLogo(BrandDNA dna) {
        if (imageModel == null) {
            log.info("No image model configured — skipping logo generation");
            dna.setLogoImageUrl("https://placehold.co/512x512/1a1a2e/ffffff?text=Logo");
            return;
        }

        String prompt = buildLogoPrompt(dna);
        try {
            var response = imageModel.call(new ImagePrompt(prompt));
            String url = response.getResult().getOutput().getUrl();
            dna.setLogoImageUrl(url);
        } catch (Exception e) {
            log.error("Logo image generation failed", e);
            dna.setLogoImageUrl("https://placehold.co/512x512/1a1a2e/ffffff?text=Logo");
        }
    }

    private String buildLogoPrompt(BrandDNA dna) {
        return String.format(
                "A minimal, modern brand logo concept. Business: %s. " +
                "Personality: %s. Style: clean, geometric, professional. " +
                "White background, no text or words, single mark only.",
                dna.getBusinessDescription(),
                dna.getPersonality()
        );
    }

    private String toJson(BrandDNA dna) {
        try {
            return objectMapper.writeValueAsString(dna);
        } catch (Exception e) {
            return dna.toString();
        }
    }
}
