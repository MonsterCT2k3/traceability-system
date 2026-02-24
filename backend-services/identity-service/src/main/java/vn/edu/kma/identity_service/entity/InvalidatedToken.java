package vn.edu.kma.identity_service.entity;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import lombok.*;

import java.util.Date;

@Entity
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class InvalidatedToken {
    @Id
    private String id; // Chính là JTI (JWT ID) của Token
    private Date expiryTime;
}
