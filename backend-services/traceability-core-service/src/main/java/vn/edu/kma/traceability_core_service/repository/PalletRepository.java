package vn.edu.kma.traceability_core_service.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import jakarta.persistence.LockModeType;
import vn.edu.kma.traceability_core_service.domain.PalletInputStatus;
import vn.edu.kma.traceability_core_service.entity.Pallet;

import java.util.List;
import java.util.Optional;

public interface PalletRepository extends JpaRepository<Pallet, String> {
    Optional<Pallet> findByChainBatchIdHex(String chainBatchIdHex);
    Optional<Pallet> findTopByProductIdOrderByCreatedAtDesc(String productId);
    List<Pallet> findAllByOwnerIdOrderByCreatedAtDesc(String ownerId);
    List<Pallet> findAllByOwnerIdAndInputStatusOrderByCreatedAtDesc(String ownerId, PalletInputStatus inputStatus);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT p FROM Pallet p WHERE p.id = :id")
    Optional<Pallet> findByIdForUpdate(@Param("id") String id);
}


