package vn.edu.kma.blockchain_service.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import vn.edu.kma.blockchain_service.domain.GasUsageStatus;
import vn.edu.kma.blockchain_service.entity.BlockchainGasUsage;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface BlockchainGasUsageRepository
        extends JpaRepository<BlockchainGasUsage, UUID>, JpaSpecificationExecutor<BlockchainGasUsage> {

    Optional<BlockchainGasUsage> findByRequestId(String requestId);

    List<BlockchainGasUsage> findTop100ByStatusInAndUpdatedAtBeforeOrderByUpdatedAtAsc(
            List<GasUsageStatus> statuses,
            Instant before);

    @Query(value = """
            select
                coalesce(sum(case when status in ('SUCCESS', 'FAILED_ON_CHAIN') then fee_wei else 0 end), 0),
                coalesce(sum(case when status = 'SUCCESS' then fee_wei else 0 end), 0),
                coalesce(sum(case when status = 'FAILED_ON_CHAIN' then fee_wei else 0 end), 0),
                coalesce(sum(case when status = 'SUCCESS' then 1 else 0 end), 0),
                coalesce(sum(case when status = 'FAILED_ON_CHAIN' then 1 else 0 end), 0),
                coalesce(sum(case when status = 'SUBMISSION_FAILED' then 1 else 0 end), 0),
                coalesce(sum(case when status = 'RECEIPT_UNKNOWN' then 1 else 0 end), 0),
                coalesce(count(*), 0)
            from blockchain_gas_usage
            where (:actorId is null or billing_actor_id = :actorId)
              and (:role is null or billing_role = :role)
              and (:fromTime is null or created_at >= :fromTime)
              and (:toTime is null or created_at <= :toTime)
            """, nativeQuery = true)
    Object[] summarize(
            @Param("actorId") String actorId,
            @Param("role") String role,
            @Param("fromTime") Instant fromTime,
            @Param("toTime") Instant toTime);

    @Query(value = """
            select operation, status, count(*), coalesce(sum(fee_wei), 0)
            from blockchain_gas_usage
            where (:actorId is null or billing_actor_id = :actorId)
              and (:role is null or billing_role = :role)
              and (:fromTime is null or created_at >= :fromTime)
              and (:toTime is null or created_at <= :toTime)
            group by operation, status
            """, nativeQuery = true)
    List<Object[]> breakdown(
            @Param("actorId") String actorId,
            @Param("role") String role,
            @Param("fromTime") Instant fromTime,
            @Param("toTime") Instant toTime);
}
