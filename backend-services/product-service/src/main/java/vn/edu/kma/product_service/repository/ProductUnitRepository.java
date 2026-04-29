package vn.edu.kma.product_service.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import vn.edu.kma.product_service.entity.ProductUnit;

import java.util.List;
import java.util.Optional;

public interface ProductUnitRepository extends JpaRepository<ProductUnit, String> {

    Optional<ProductUnit> findByUnitSerial(String unitSerial);

    List<ProductUnit> findByCartonIdOrderByCreatedAtAsc(String cartonId);

    long countByCartonId(String cartonId);

    @org.springframework.data.jpa.repository.Modifying
    @org.springframework.data.jpa.repository.Query("UPDATE ProductUnit u SET u.ownerId = :newOwnerId, u.status = :status WHERE u.cartonId = :cartonId")
    void updateOwnerAndStatusByCartonId(@org.springframework.data.repository.query.Param("cartonId") String cartonId, @org.springframework.data.repository.query.Param("newOwnerId") String newOwnerId, @org.springframework.data.repository.query.Param("status") String status);

    @org.springframework.data.jpa.repository.Modifying
    @org.springframework.data.jpa.repository.Query("UPDATE ProductUnit u SET u.status = :status WHERE u.cartonId = :cartonId")
    void updateStatusByCartonId(@org.springframework.data.repository.query.Param("cartonId") String cartonId, @org.springframework.data.repository.query.Param("status") String status);
}
