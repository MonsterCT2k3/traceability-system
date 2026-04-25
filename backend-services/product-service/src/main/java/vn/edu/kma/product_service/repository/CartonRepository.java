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

    List<Carton> findByPalletIdOrderByCreatedAtDesc(String palletId);
    List<Carton> findByOwnerIdOrderByCreatedAtDesc(String ownerId);

    long countByPalletId(String palletId);

    @Query(value = """
            SELECT
                c.product_id AS productId,
                COALESCE(p.name, c.product_id) AS productName,
                COUNT(DISTINCT c.id) AS cartonsCount,
                COUNT(u.id) AS unitsCount
            FROM cartons c
            LEFT JOIN product p ON p.id = c.product_id
            LEFT JOIN product_units u ON u.carton_id = c.id
            WHERE c.owner_id = :ownerId
            GROUP BY c.product_id, p.name
            ORDER BY COUNT(DISTINCT c.id) DESC, COALESCE(p.name, c.product_id) ASC
            """, nativeQuery = true)
    List<ProductPackingSummaryProjection> summarizePackingByOwner(@Param("ownerId") String ownerId);
}
