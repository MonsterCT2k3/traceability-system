package vn.edu.kma.product_service.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import vn.edu.kma.product_service.entity.Carton;
import vn.edu.kma.product_service.repository.projection.ProductPackingSummaryProjection;

import java.util.List;
import java.util.Optional;

public interface CartonRepository extends JpaRepository<Carton, String> {

    Optional<Carton> findByCartonCode(String cartonCode);

    @Query("SELECT c FROM Carton c WHERE c.pallet.id = :palletId ORDER BY c.createdAt DESC")
    List<Carton> findByPalletIdOrderByCreatedAtDesc(@Param("palletId") String palletId);

    @Query("SELECT c FROM Carton c WHERE c.manufacturerId = :manufacturerId OR (c.manufacturerId IS NULL AND (c.ownerId = :manufacturerId OR EXISTS (SELECT 1 FROM TransferRecord tr WHERE tr.targetId = CAST(c.id AS string) AND tr.targetType = 'CARTON' AND tr.fromUserId = :manufacturerId))) ORDER BY c.createdAt DESC")
    List<Carton> findByManufacturerIdWithFallback(@Param("manufacturerId") String manufacturerId);

    @Query("SELECT c FROM Carton c WHERE c.product.id = :productId AND c.ownerId = :ownerId AND c.status = 'SHIPPING' ORDER BY c.createdAt ASC")
    List<Carton> findAvailableForDelivery(@Param("productId") String productId, @Param("ownerId") String ownerId, org.springframework.data.domain.Pageable pageable);

    @Query("SELECT c FROM Carton c WHERE c.product.id = :productId AND c.ownerId = :ownerId AND (c.status = 'IN_STOCK' OR c.status IS NULL) ORDER BY c.createdAt ASC")
    List<Carton> findAvailableForShipping(@Param("productId") String productId, @Param("ownerId") String ownerId, org.springframework.data.domain.Pageable pageable);

    @Query("SELECT c FROM Carton c WHERE c.product.id = :productId AND c.ownerId = :ownerId ORDER BY c.createdAt ASC")
    List<Carton> findByProductIdAndOwnerIdOrderByCreatedAtAsc(@Param("productId") String productId, @Param("ownerId") String ownerId, org.springframework.data.domain.Pageable pageable);

    @Query("SELECT COUNT(c) FROM Carton c WHERE c.pallet.id = :palletId")
    long countByPalletId(@Param("palletId") String palletId);

    @Query(value = """
            SELECT
                c.product_id AS productId,
                COALESCE(p.name, c.product_id) AS productName,
                COUNT(DISTINCT c.id) AS cartonsCount,
                COUNT(u.id) AS unitsCount
            FROM cartons c
            LEFT JOIN product p ON p.id = c.product_id
            LEFT JOIN product_units u ON u.carton_id = c.id
            WHERE c.manufacturer_id = :manufacturerId 
               OR (c.manufacturer_id IS NULL AND (c.owner_id = :manufacturerId OR EXISTS (
                   SELECT 1 FROM transfer_records tr 
                   WHERE tr.target_id = CAST(c.id AS VARCHAR) AND tr.target_type = 'CARTON' AND tr.from_user_id = :manufacturerId
               )))
            GROUP BY c.product_id, p.name
            ORDER BY COUNT(DISTINCT c.id) DESC, COALESCE(p.name, c.product_id) ASC
            """, nativeQuery = true)
    List<ProductPackingSummaryProjection> summarizePackingByManufacturer(@Param("manufacturerId") String manufacturerId);
}
