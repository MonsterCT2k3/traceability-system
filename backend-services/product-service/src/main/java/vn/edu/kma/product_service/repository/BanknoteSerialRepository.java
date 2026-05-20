package vn.edu.kma.product_service.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import vn.edu.kma.product_service.entity.BanknoteSerial;

import java.util.Collection;
import java.util.List;

public interface BanknoteSerialRepository extends JpaRepository<BanknoteSerial, String> {

    List<BanknoteSerial> findBySerialValueIn(Collection<String> serialValues);

    List<BanknoteSerial> findBySerialValueInAndRegisteredByUserId(Collection<String> serialValues, String registeredByUserId);

    long countByRegisteredByUserId(String registeredByUserId);

    // Đơn giản, nhanh, có thể đánh index: WHERE is_used = false
    long countByRegisteredByUserIdAndIsUsedFalse(String registeredByUserId);

    // Thay thế NOT EXISTS bằng is_used = false — nhanh hơn và rõ ràng hơn về mặt nghiệp vụ
    @Query(value = """
            SELECT b.serial_value
            FROM banknote_serial b
            WHERE b.registered_by_user_id = :userId
              AND b.is_used = false
            ORDER BY b.created_at ASC
            LIMIT :limitValue
            """, nativeQuery = true)
    List<String> findAvailableSerialValuesForUser(
            @Param("userId") String userId,
            @Param("limitValue") int limitValue
    );

    // Đánh dấu hàng loạt serial là đã sử dụng sau khi gán vào ProductUnit
    @Modifying
    @Query("UPDATE BanknoteSerial b SET b.isUsed = true, b.assignedUnit = :unit WHERE b.serialValue = :serialValue")
    void markAsUsed(@Param("serialValue") String serialValue,
                    @Param("unit") vn.edu.kma.product_service.entity.ProductUnit unit);
}
