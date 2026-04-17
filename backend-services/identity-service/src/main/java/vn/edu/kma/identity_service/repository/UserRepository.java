package vn.edu.kma.identity_service.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import vn.edu.kma.identity_service.entity.User;

import java.util.List;
import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, String> {
    boolean existsByUsername(String username);
    Optional<User> findByUsername(String username);

    Page<User> findByRoleOrderByFullNameAsc(String role, Pageable pageable);

    @Query("SELECT u FROM User u WHERE u.role = :role AND (" +
            "LOWER(u.username) LIKE LOWER(:like) OR " +
            "LOWER(COALESCE(u.fullName, '')) LIKE LOWER(:like) OR " +
            "(u.description IS NOT NULL AND LOWER(u.description) LIKE LOWER(:like)))")
    List<User> searchUsersByRoleAndLike(@Param("like") String like, @Param("role") String role, Pageable pageable);
}
