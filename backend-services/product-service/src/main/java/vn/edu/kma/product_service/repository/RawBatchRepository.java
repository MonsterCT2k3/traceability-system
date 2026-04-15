package vn.edu.kma.product_service.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import vn.edu.kma.product_service.entity.RawBatch;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface RawBatchRepository extends JpaRepository<RawBatch, String> {
    Optional<RawBatch> findByBatchIdHex(String batchIdHex);

    List<RawBatch> findAllByOwnerIdOrderByCreatedAtDesc(String ownerId);

    List<RawBatch> findAllByIdIn(Collection<String> ids);
}

