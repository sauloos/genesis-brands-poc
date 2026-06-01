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

import java.nio.charset.StandardCharsets;
import java.util.Base64;
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

            IMPORTANT: If the brand has existing colours (existingColors field), use them as inspiration:
            - You may incorporate 1-2 of the existing colours if they suit the brand
            - Or evolve them into a more refined palette
            - Explain how the new palette relates to or improves upon their current colours
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

        String userMessage = "Brand signals:\n" + signalJson;
        if (dna.getRegenerationFeedback() != null && !dna.getRegenerationFeedback().isBlank()) {
            userMessage += "\n\nUser feedback on the previous version:\n" + dna.getRegenerationFeedback()
                    + "\n\nPlease revise the colours and typography to address this feedback.";
        }

        String raw = chatClientBuilder.build()
                .prompt()
                .system(VISUAL_SYSTEM_PROMPT)
                .user(userMessage)
                .call()
                .content();

        try {
            JsonNode root = objectMapper.readTree(stripFences(raw));

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
        if (imageModel != null) {
            String prompt = buildLogoPrompt(dna);
            try {
                var response = imageModel.call(new ImagePrompt(prompt));
                String url = response.getResult().getOutput().getUrl();
                dna.setLogoImageUrl(url);
                return;
            } catch (Exception e) {
                log.warn("Image model failed, falling back to SVG logo: {}", e.getMessage());
            }
        }
        generateSvgLogo(dna);
    }

    private void generateSvgLogo(BrandDNA dna) {
        String primaryColor = (dna.getColourPalette() != null && !dna.getColourPalette().isEmpty())
                ? dna.getColourPalette().get(0).getHex()
                : "#1a1a2e";
        String accentColor = (dna.getColourPalette() != null && dna.getColourPalette().size() > 2)
                ? dna.getColourPalette().get(2).getHex()
                : "#ffffff";

        String svgPrompt = String.format("""
                Create a minimal SVG logo mark for this brand:
                Business: %s
                Personality: %s
                Primary colour: %s
                Accent colour: %s

                Rules:
                - Return ONLY valid SVG code, no explanation or markdown fences
                - The opening tag MUST be: <svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 100 100">
                - No width/height attributes on the svg element
                - Minimal geometric shapes (circles, triangles, lines, arcs)
                - Use the provided colours
                - No text or letters
                - Keep it under 20 SVG elements
                """,
                dna.getBusinessDescription(),
                dna.getPersonality() != null ? dna.getPersonality() : "professional",
                primaryColor,
                accentColor
        );

        try {
            String svg = chatClientBuilder.build()
                    .prompt()
                    .user(svgPrompt)
                    .call()
                    .content();

            if (svg != null && svg.contains("<svg")) {
                int start = svg.indexOf("<svg");
                int end = svg.lastIndexOf("</svg>") + 6;
                if (end > start) {
                    svg = svg.substring(start, end);
                    // Ensure xmlns is present — required for <img> src rendering
                    if (!svg.contains("xmlns=")) {
                        svg = svg.replace("<svg ", "<svg xmlns=\"http://www.w3.org/2000/svg\" ");
                    }
                    String dataUrl = "data:image/svg+xml;base64," +
                            Base64.getEncoder().encodeToString(svg.getBytes(StandardCharsets.UTF_8));
                    dna.setLogoImageUrl(dataUrl);
                    return;
                }
            }
        } catch (Exception e) {
            log.error("SVG logo generation failed", e);
        }

        dna.setLogoImageUrl("https://placehold.co/512x512/1a1a2e/ffffff?text=Logo");
    }

    private String buildLogoPrompt(BrandDNA dna) {
        String basePrompt = String.format(
                "A minimal, modern brand logo concept. Business: %s. " +
                "Personality: %s. Style: clean, geometric, professional. " +
                "White background, no text or words, single mark only.",
                dna.getBusinessDescription(),
                dna.getPersonality()
        );

        // Add color context if we have existing brand colors
        if (dna.getExistingColors() != null && !dna.getExistingColors().isEmpty()) {
            String primaryColor = dna.getExistingColors().get(0);
            basePrompt += " Use color scheme inspired by " + primaryColor + ".";
        }

        return basePrompt;
    }

    private static String stripFences(String s) {
        if (s == null) return null;
        String t = s.strip();
        if (t.startsWith("```")) {
            t = t.replaceFirst("^```[a-zA-Z]*\\s*", "");
            int end = t.lastIndexOf("```");
            if (end >= 0) t = t.substring(0, end);
        }
        return t.strip();
    }

    private String toJson(BrandDNA dna) {
        try {
            return objectMapper.writeValueAsString(dna);
        } catch (Exception e) {
            return dna.toString();
        }
    }
}
