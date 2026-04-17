package vn.edu.kma.product_service.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import vn.edu.kma.product_service.entity.MaterialItem;

import java.util.List;

public interface MaterialItemRepository extends JpaRepository<MaterialItem, String> {

    List<MaterialItem> findAllByCategory_IdAndActiveTrueOrderBySortOrderAsc(String categoryId);

    boolean existsByCategory_IdAndNameAndActiveTrue(String categoryId, String name);
}
