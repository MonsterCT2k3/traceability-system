package vn.edu.kma.traceability_core_service.repository;

import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import vn.edu.kma.traceability_core_service.entity.ProductUnitClaim;
import java.util.Optional;

public interface ProductUnitClaimRepository extends JpaRepository<ProductUnitClaim, String> {
    Optional<ProductUnitClaim> findByClaimTokenHash(String hash);
    Optional<ProductUnitClaim> findByProductUnitId(String unitId);
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select c from ProductUnitClaim c join fetch c.productUnit where c.claimTokenHash = :hash")
    Optional<ProductUnitClaim> findByHashForUpdate(@Param("hash") String hash);
}
