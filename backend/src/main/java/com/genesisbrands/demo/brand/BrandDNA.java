package com.genesisbrands.demo.brand;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BrandDNA {

    // ── Signals (extracted from questionnaire) ────────────────────────────────
    private String businessDescription;
    private String targetAudience;
    private String personality;
    private String toneOfVoice;
    private List<String> differentiators;
    private String aspirations;

    // ── Generated copy ────────────────────────────────────────────────────────
    private String tagline;
    private String missionStatement;
    private String brandStory;
    private String elevatorPitch;
    private String toneGuide;

    // ── Generated visual ─────────────────────────────────────────────────────
    private List<ColourSwatch> colourPalette;
    private Typography typography;
    private String logoImageUrl;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ColourSwatch {
        private String hex;
        private String name;
        private String role;        // primary | secondary | accent | neutral | background
        private String rationale;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Typography {
        private String primaryFont;
        private String primaryUsage;
        private String secondaryFont;
        private String secondaryUsage;
    }
}
