package vn.edu.kma.product_service.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import vn.edu.kma.product_service.entity.ProductUnit;

import java.util.List;
import java.util.Optional;

public interface ProductUnitRepository extends JpaRepository<ProductUnit, String> {

    Optional<ProductUnit> findByUnitSerial(String unitSerial);

    @Query("SELECT u FROM ProductUnit u WHERE u.carton.id = :cartonId ORDER BY u.createdAt ASC")
    List<ProductUnit> findByCartonIdOrderByCreatedAtAsc(@Param("cartonId") String cartonId);

    @Query("SELECT COUNT(u) FROM ProductUnit u WHERE u.carton.id = :cartonId")
    long countByCartonId(@Param("cartonId") String cartonId);

    @Modifying
    @Query("UPDATE ProductUnit u SET u.ownerId = :newOwnerId, u.status = :status WHERE u.carton.id = :cartonId")
    void updateOwnerAndStatusByCartonId(@Param("cartonId") String cartonId, @Param("newOwnerId") String newOwnerId, @Param("status") String status);

    @Modifying
    @Query("UPDATE ProductUnit u SET u.status = :status WHERE u.carton.id = :cartonId")
    void updateStatusByCartonId(@Param("cartonId") String cartonId, @Param("status") String status);
}
