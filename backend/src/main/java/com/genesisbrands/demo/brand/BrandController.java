package com.genesisbrands.demo.brand;

import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.UUID;

@RestController
@RequestMapping("/api/brands")
@RequiredArgsConstructor
public class BrandController {

    private final BrandService brandService;

    /** Create a new brand (DRAFT state). Called at the start of the flow. */
    @PostMapping
    public ResponseEntity<BrandResponse> create() {
        Brand brand = brandService.create();
        return ResponseEntity.status(HttpStatus.CREATED).body(toResponse(brand));
    }

    /** Get brand status + metadata. Frontend polls this during generation. */
    @GetMapping("/{id}/status")
    public ResponseEntity<StatusResponse> status(@PathVariable UUID id) {
        Brand brand = brandService.get(id);
        return ResponseEntity.ok(new StatusResponse(brand.getId(), brand.getStatus(), brand.getError()));
    }

    /** Get the full brand output once generation is READY. */
    @GetMapping("/{id}/output")
    public ResponseEntity<BrandDNA> output(@PathVariable UUID id) {
        Brand brand = brandService.get(id);
        if (brand.getStatus() != BrandStatus.READY) {
            return ResponseEntity.status(HttpStatus.CONFLICT).build();
        }
        return ResponseEntity.ok(brand.getBrandDna());
    }

    /** Trigger async brand generation. Returns 202 immediately. */
    @PostMapping("/{id}/generate")
    public ResponseEntity<Void> generate(@PathVariable UUID id) {
        brandService.triggerGeneration(id);
        return ResponseEntity.accepted().build();
    }

    private BrandResponse toResponse(Brand brand) {
        return new BrandResponse(brand.getId(), brand.getStatus(), brand.getCreatedAt());
    }

    record BrandResponse(UUID id, BrandStatus status, Instant createdAt) {}
    record StatusResponse(UUID id, BrandStatus status, String error) {}
}
