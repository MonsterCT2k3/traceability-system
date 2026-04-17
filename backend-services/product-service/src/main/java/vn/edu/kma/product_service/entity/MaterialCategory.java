package vn.edu.kma.product_service.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "material_categories")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MaterialCategory {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    /** Mã ổn định (seed / tham chiếu kỹ thuật). */
    @Column(nullable = false, unique = true, length = 64)
    private String code;

    /** Nhãn hiển thị — trùng với RawBatch.materialType khi chọn từ danh mục. */
    @Column(nullable = false, length = 128)
    private String label;

    @Column(nullable = false)
    private Integer sortOrder;

    @Column(nullable = false)
    private boolean active;
}
