package vn.edu.kma.identity_service.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import vn.edu.kma.identity_service.entity.RoleRequest;

import java.util.List;
import java.util.Optional;

@Repository
public interface RoleRequestRepository extends JpaRepository<RoleRequest, String> {
    List<RoleRequest> findByStatusOrderByCreatedAtDesc(String status);
    List<RoleRequest> findByUserIdOrderByCreatedAtDesc(String userId);
    Optional<RoleRequest> findByUserIdAndStatus(String userId, String status);
}
