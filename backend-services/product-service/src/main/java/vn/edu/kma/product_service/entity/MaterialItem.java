package vn.edu.kma.product_service.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "material_items")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MaterialItem {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "category_id", nullable = false)
    private MaterialCategory category;

    /** Trùng với RawBatch.materialName khi chọn từ danh mục. */
    @Column(nullable = false, length = 256)
    private String name;

    @Column(nullable = false)
    private Integer sortOrder;

    @Column(nullable = false)
    private boolean active;
}
