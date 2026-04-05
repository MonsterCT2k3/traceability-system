package vn.edu.kma.product_service.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import vn.edu.kma.product_service.entity.ProductUnit;

import java.util.List;
import java.util.Optional;

public interface ProductUnitRepository extends JpaRepository<ProductUnit, String> {

    Optional<ProductUnit> findByUnitSerial(String unitSerial);

    Optional<ProductUnit> findBySecretHash(String secretHash);

    List<ProductUnit> findByCartonIdOrderByCreatedAtAsc(String cartonId);

    long countByCartonId(String cartonId);
}
