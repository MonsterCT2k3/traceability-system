package vn.edu.kma.blockchain_service.schedule;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.web3j.protocol.Web3j;
import org.web3j.protocol.core.methods.response.TransactionReceipt;
import vn.edu.kma.blockchain_service.domain.GasUsageStatus;
import vn.edu.kma.blockchain_service.entity.BlockchainGasUsage;
import vn.edu.kma.blockchain_service.repository.BlockchainGasUsageRepository;

import java.math.BigInteger;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class GasUsageReconciliationJob {

    private final BlockchainGasUsageRepository repository;
    private final Web3j web3j;

    @Value("${blockchain.gas.price}")
    private BigInteger configuredGasPrice;

    @Scheduled(fixedDelayString = "${blockchain.gas.reconcile-delay-ms:60000}")
    @Transactional
    public void reconcileUnknownReceipts() {
        Instant before = Instant.now().minus(30, ChronoUnit.SECONDS);
        List<BlockchainGasUsage> usages = repository.findTop100ByStatusInAndUpdatedAtBeforeOrderByUpdatedAtAsc(
                List.of(GasUsageStatus.PENDING, GasUsageStatus.RECEIPT_UNKNOWN),
                before);

        for (BlockchainGasUsage usage : usages) {
            if (usage.getTxHash() == null || usage.getTxHash().isBlank()) {
                continue;
            }
            try {
                web3j.ethGetTransactionReceipt(usage.getTxHash())
                        .send()
                        .getTransactionReceipt()
                        .ifPresent(receipt -> updateFromReceipt(usage, receipt));
            } catch (Exception e) {
                log.warn("Cannot reconcile gas usage requestId={}, txHash={}: {}",
                        usage.getRequestId(), usage.getTxHash(), e.getMessage());
            }
        }
    }

    private void updateFromReceipt(BlockchainGasUsage usage, TransactionReceipt receipt) {
        BigInteger gasPrice = resolveGasPrice(receipt.getTransactionHash());
        BigInteger gasUsed = receipt.getGasUsed();
        usage.setStatus(receipt.isStatusOK() ? GasUsageStatus.SUCCESS : GasUsageStatus.FAILED_ON_CHAIN);
        usage.setGasUsed(gasUsed);
        usage.setEffectiveGasPriceWei(gasPrice);
        usage.setFeeWei(gasUsed == null || gasPrice == null ? null : gasUsed.multiply(gasPrice));
        usage.setBlockNumber(receipt.getBlockNumber());
        usage.setMinedAt(Instant.now());
        usage.setErrorCode(receipt.isStatusOK() ? null : "RECONCILED_FAILED_ON_CHAIN");
        usage.setErrorMessage(receipt.isStatusOK() ? null : "Transaction receipt status is failed");
        repository.save(usage);
    }

    private BigInteger resolveGasPrice(String txHash) {
        try {
            return web3j.ethGetTransactionByHash(txHash)
                    .send()
                    .getTransaction()
                    .map(tx -> tx.getGasPrice())
                    .orElse(configuredGasPrice);
        } catch (Exception e) {
            return configuredGasPrice;
        }
    }
}
