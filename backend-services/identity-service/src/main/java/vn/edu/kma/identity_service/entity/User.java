package vn.edu.kma.identity_service.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "users")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class User {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    @Column(unique = true, nullable = false)
    private String username;

    private String password;
    private String fullName;
    private String email;
    private String phone;
    private String avatarUrl;
    private String role;
    
    @Column(columnDefinition = "TEXT")
    private String description;
    
    private String location;
}