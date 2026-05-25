package vn.edu.kma.catalog_service.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import vn.edu.kma.catalog_service.entity.MaterialCategory;

import java.util.List;
import java.util.Optional;

public interface MaterialCategoryRepository extends JpaRepository<MaterialCategory, String> {

    List<MaterialCategory> findAllByActiveTrueOrderBySortOrderAsc();

    @Query("SELECT c FROM MaterialCategory c WHERE c.active = true AND (c.ownerId IS NULL OR c.ownerId = :ownerId) ORDER BY c.sortOrder ASC")
    List<MaterialCategory> findAllGlobalAndOwner(@Param("ownerId") String ownerId);

    Optional<MaterialCategory> findByLabelAndActiveTrue(String label);

    boolean existsByCode(String code);
}
