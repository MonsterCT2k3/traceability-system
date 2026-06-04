package vn.edu.kma.traceability_core_service.entity;

import jakarta.persistence.*;
import lombok.*;
import vn.edu.kma.traceability_core_service.domain.PalletInputType;

import java.time.LocalDateTime;

@Entity
@Table(name = "pallet_inputs")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PalletInput {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    @Column(name = "output_pallet_id", nullable = false, length = 36)
    private String outputPalletId;

    @Enumerated(EnumType.STRING)
    @Column(name = "input_type", nullable = false, length = 20)
    private PalletInputType inputType;

    @Column(name = "input_id", nullable = false, length = 36)
    private String inputId;

    @Column(name = "input_batch_id_hex", nullable = false, length = 66)
    private String inputBatchIdHex;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @PrePersist
    void prePersist() {
        if (createdAt == null) createdAt = LocalDateTime.now();
    }
}
