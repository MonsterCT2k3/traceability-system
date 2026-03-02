package vn.edu.kma.product_service.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import vn.edu.kma.product_service.entity.ProductHistory;

import java.util.List;

@Repository
public interface ProductHistoryRepository extends JpaRepository<ProductHistory, String> {
    // Tìm tất cả lịch sử của 1 sản phẩm, sắp xếp mới nhất lên đầu
    List<ProductHistory> findByProductIdOrderByTimestampDesc(String productId);
}
