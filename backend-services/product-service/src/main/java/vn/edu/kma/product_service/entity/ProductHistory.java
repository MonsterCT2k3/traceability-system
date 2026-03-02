package vn.edu.kma.product_service.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "product_histories")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProductHistory {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    @Column(nullable = false)
    private String productId; // Liên kết với Product (Không cần @ManyToOne để giảm phức tạp Microservice)

    @Column(nullable = false)
    private String action; // Ví dụ: CREATE, UPDATE, HARVEST, TRANSPORT

    private String description; // Chi tiết: "Đã bón phân NPK", "Đang vận chuyển bằng xe 29C..."

    private String location; // Địa điểm thực hiện

    private String actorId; // Người thực hiện (Lấy từ Token)

    private LocalDateTime timestamp; // Thời gian thực hiện

    @PrePersist
    protected void onCreate() {
        timestamp = LocalDateTime.now();
    }
}
