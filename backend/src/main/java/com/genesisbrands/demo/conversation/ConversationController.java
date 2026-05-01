package com.genesisbrands.demo.conversation;

import com.genesisbrands.demo.brand.BrandDNA;
import jakarta.validation.constraints.NotBlank;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/conversation")
@RequiredArgsConstructor
public class ConversationController {

    private final ConversationService conversationService;

    @PostMapping("/start")
    public ResponseEntity<ConversationService.StartResponse> start(@RequestBody StartRequest body) {
        return ResponseEntity.ok(conversationService.start(body.brandId()));
    }

    @PostMapping("/{sessionId}/answer")
    public ResponseEntity<ConversationService.AnswerResponse> answer(
            @PathVariable UUID sessionId,
            @RequestBody AnswerRequest body) {
        return ResponseEntity.ok(conversationService.answer(sessionId, body.answer()));
    }

    @GetMapping("/{sessionId}/signals")
    public ResponseEntity<BrandDNA> signals(@PathVariable UUID sessionId) {
        return ResponseEntity.ok(conversationService.getSignals(sessionId));
    }

    record StartRequest(UUID brandId) {}
    record AnswerRequest(@NotBlank String answer) {}
}
