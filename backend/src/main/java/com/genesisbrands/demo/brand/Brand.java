package com.genesisbrands.demo.brand;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "brands")
@Getter
@Setter
public class Brand {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Enumerated(EnumType.STRING)
    @Column(columnDefinition = "brand_status")
    private BrandStatus status = BrandStatus.DRAFT;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "brand_dna", columnDefinition = "jsonb")
    private BrandDNA brandDna;

    private String error;

    private Instant createdAt = Instant.now();
    private Instant updatedAt = Instant.now();

    @PreUpdate
    void onUpdate() {
        updatedAt = Instant.now();
    }
}
