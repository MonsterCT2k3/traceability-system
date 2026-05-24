package vn.edu.kma.traceability_core_service.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import vn.edu.kma.traceability_core_service.entity.Carton;
import vn.edu.kma.traceability_core_service.repository.projection.ProductPackingSummaryProjection;

import java.util.List;
import java.util.Optional;

public interface CartonRepository extends JpaRepository<Carton, String> {

    Optional<Carton> findByCartonCode(String cartonCode);

    @Query("SELECT c FROM Carton c WHERE c.pallet.id = :palletId ORDER BY c.createdAt DESC")
    List<Carton> findByPalletIdOrderByCreatedAtDesc(@Param("palletId") String palletId);

    @Query("SELECT c FROM Carton c WHERE c.manufacturerId = :manufacturerId ORDER BY c.createdAt DESC")
    List<Carton> findByManufacturerIdWithFallback(@Param("manufacturerId") String manufacturerId);

    @Query("SELECT c FROM Carton c WHERE c.productId = :productId AND c.ownerId = :ownerId AND c.status = 'SHIPPING' ORDER BY c.createdAt ASC")
    List<Carton> findAvailableForDelivery(@Param("productId") String productId, @Param("ownerId") String ownerId, org.springframework.data.domain.Pageable pageable);

    @Query("SELECT c FROM Carton c WHERE c.productId = :productId AND c.ownerId = :ownerId AND (c.status = 'IN_STOCK' OR c.status IS NULL) ORDER BY c.createdAt ASC")
    List<Carton> findAvailableForShipping(@Param("productId") String productId, @Param("ownerId") String ownerId, org.springframework.data.domain.Pageable pageable);

    @Query("SELECT COUNT(c) FROM Carton c WHERE c.productId = :productId AND c.ownerId = :ownerId AND (c.status = 'IN_STOCK' OR c.status IS NULL)")
    long countAvailableForShipping(@Param("productId") String productId, @Param("ownerId") String ownerId);

    @Query("SELECT c FROM Carton c WHERE c.productId = :productId AND c.ownerId = :ownerId ORDER BY c.createdAt ASC")
    List<Carton> findByProductIdAndOwnerIdOrderByCreatedAtAsc(@Param("productId") String productId, @Param("ownerId") String ownerId, org.springframework.data.domain.Pageable pageable);

    @Query("SELECT COUNT(c) FROM Carton c WHERE c.pallet.id = :palletId")
    long countByPalletId(@Param("palletId") String palletId);

    @Query(value = """
            SELECT
                c.product_id AS productId,
                c.product_id AS productName,
                COUNT(DISTINCT c.id) AS cartonsCount,
                COUNT(u.id) AS unitsCount
            FROM cartons c
            LEFT JOIN product_units u ON u.carton_id = c.id
            WHERE c.manufacturer_id = :manufacturerId 
            GROUP BY c.product_id
            ORDER BY COUNT(DISTINCT c.id) DESC, c.product_id ASC
            """, nativeQuery = true)
    List<ProductPackingSummaryProjection> summarizePackingByManufacturer(@Param("manufacturerId") String manufacturerId);
}

