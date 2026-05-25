package vn.edu.kma.catalog_service.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import vn.edu.kma.catalog_service.entity.MaterialItem;

import java.util.List;

public interface MaterialItemRepository extends JpaRepository<MaterialItem, String> {

    List<MaterialItem> findAllByCategory_IdAndActiveTrueOrderBySortOrderAsc(String categoryId);

    @Query("SELECT i FROM MaterialItem i WHERE i.category.id = :categoryId AND i.active = true AND (i.ownerId IS NULL OR i.ownerId = :ownerId) ORDER BY i.sortOrder ASC")
    List<MaterialItem> findAllGlobalAndOwnerByCategory(@Param("categoryId") String categoryId, @Param("ownerId") String ownerId);

    boolean existsByCategory_IdAndNameAndActiveTrue(String categoryId, String name);
}
