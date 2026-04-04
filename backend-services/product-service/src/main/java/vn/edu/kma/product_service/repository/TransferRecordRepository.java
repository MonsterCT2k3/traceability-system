package vn.edu.kma.product_service.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import vn.edu.kma.product_service.entity.TransferRecord;
import java.util.List;
import java.util.Optional;

@Repository
public interface TransferRecordRepository extends JpaRepository<TransferRecord, String> {
    // Tìm yêu cầu chuyển giao đang chờ xử lý cho người nhận
    List<TransferRecord> findByToUserIdAndStatus(String toUserId, String status);

    // Tìm yêu cầu chuyển giao đang chờ cho pallet cụ thể
    Optional<TransferRecord> findByPalletIdAndStatus(String palletId, String status);

    // Tìm yêu cầu chuyển giao đang chờ theo target tổng quát
    Optional<TransferRecord> findByTargetTypeAndTargetIdAndStatus(String targetType, String targetId, String status);

    // Legacy: Tìm yêu cầu chuyển giao đang chờ cho sản phẩm cụ thể
    Optional<TransferRecord> findByProductIdAndStatus(String productId, String status);
}
