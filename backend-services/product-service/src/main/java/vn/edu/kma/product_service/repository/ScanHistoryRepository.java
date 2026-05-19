package vn.edu.kma.product_service.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import vn.edu.kma.product_service.entity.ScanHistory;

import java.util.Optional;

public interface ScanHistoryRepository extends JpaRepository<ScanHistory, String> {
    Optional<ScanHistory> findByUserIdAndUnitSerial(String userId, String unitSerial);
    Page<ScanHistory> findByUserIdOrderByScannedAtDesc(String userId, Pageable pageable);
}
