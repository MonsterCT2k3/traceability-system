package vn.edu.kma.product_service.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import vn.edu.kma.product_service.entity.TradeOrder;

import java.util.List;
import java.util.Optional;

public interface TradeOrderRepository extends JpaRepository<TradeOrder, String> {

    @Query("SELECT DISTINCT o FROM TradeOrder o LEFT JOIN FETCH o.lines WHERE o.id = :id")
    Optional<TradeOrder> findByIdWithLines(@Param("id") String id);

    List<TradeOrder> findByBuyerIdOrderByCreatedAtDesc(String buyerId);

    List<TradeOrder> findBySellerIdOrderByCreatedAtDesc(String sellerId);

    List<TradeOrder> findByCarrierIdOrderByCreatedAtDesc(String carrierId);
}
