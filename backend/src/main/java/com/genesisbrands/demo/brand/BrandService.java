package com.genesisbrands.demo.brand;

import com.genesisbrands.demo.copy.CopyGeneratorService;
import com.genesisbrands.demo.visual.VisualGeneratorService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class BrandService {

    private final BrandRepository brandRepository;
    private final CopyGeneratorService copyGenerator;
    private final VisualGeneratorService visualGenerator;

    @Transactional
    public Brand create() {
        Brand brand = new Brand();
        brand.setBrandDna(new BrandDNA());
        return brandRepository.save(brand);
    }

    @Transactional(readOnly = true)
    public Brand get(UUID id) {
        return brandRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Brand not found: " + id));
    }

    @Transactional
    public void triggerGeneration(UUID brandId) {
        Brand brand = get(brandId);
        brand.setStatus(BrandStatus.GENERATING);
        brandRepository.save(brand);
        runGenerationAsync(brandId);
    }

    @Async("generationExecutor")
    public void runGenerationAsync(UUID brandId) {
        log.info("Starting generation for brand {}", brandId);
        try {
            // Load brand in a fresh transaction context
            Brand brand = get(brandId);
            BrandDNA dna = brand.getBrandDna();

            // Copy and visual run sequentially in the demo (async in production)
            copyGenerator.generateCopy(dna);
            visualGenerator.generateVisual(dna);

            brand.setBrandDna(dna);
            brand.setStatus(BrandStatus.READY);
            brandRepository.save(brand);
            log.info("Generation complete for brand {}", brandId);

        } catch (Exception e) {
            log.error("Generation failed for brand {}", brandId, e);
            brandRepository.findById(brandId).ifPresent(b -> {
                b.setStatus(BrandStatus.FAILED);
                b.setError(e.getMessage());
                brandRepository.save(b);
            });
        }
    }
}
