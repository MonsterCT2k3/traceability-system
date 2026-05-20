package vn.edu.kma.product_service.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import vn.edu.kma.product_service.entity.ScanHistory;

import java.util.Optional;

public interface ScanHistoryRepository extends JpaRepository<ScanHistory, String> {

    @Query("SELECT s FROM ScanHistory s WHERE s.userId = :userId AND s.productUnit.unitSerial = :unitSerial")
    Optional<ScanHistory> findByUserIdAndUnitSerial(@Param("userId") String userId, @Param("unitSerial") String unitSerial);

    Page<ScanHistory> findByUserIdOrderByScannedAtDesc(String userId, Pageable pageable);
}
