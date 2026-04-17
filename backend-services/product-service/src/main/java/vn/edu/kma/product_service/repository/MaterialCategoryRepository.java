package vn.edu.kma.product_service.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import vn.edu.kma.product_service.entity.MaterialCategory;

import java.util.List;
import java.util.Optional;

public interface MaterialCategoryRepository extends JpaRepository<MaterialCategory, String> {

    List<MaterialCategory> findAllByActiveTrueOrderBySortOrderAsc();

    Optional<MaterialCategory> findByLabelAndActiveTrue(String label);

    boolean existsByCode(String code);
}
