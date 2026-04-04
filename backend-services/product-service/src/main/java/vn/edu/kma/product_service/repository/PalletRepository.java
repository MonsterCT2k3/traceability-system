package vn.edu.kma.product_service.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import vn.edu.kma.product_service.entity.Pallet;

import java.util.Optional;

public interface PalletRepository extends JpaRepository<Pallet, String> {
    Optional<Pallet> findByChainBatchIdHex(String chainBatchIdHex);
    Optional<Pallet> findTopByProductIdOrderByCreatedAtDesc(String productId);
}

