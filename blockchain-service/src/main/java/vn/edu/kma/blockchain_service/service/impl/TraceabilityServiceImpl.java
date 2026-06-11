package vn.edu.kma.blockchain_service.service.impl;

import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.web3j.crypto.Credentials;
import org.web3j.crypto.Hash;
import org.web3j.utils.Numeric;
import org.web3j.protocol.Web3j;
import org.web3j.protocol.exceptions.TransactionException;
import org.web3j.protocol.core.methods.response.TransactionReceipt;
import org.web3j.tx.RawTransactionManager;
import org.web3j.tx.TransactionManager;
import org.web3j.tx.gas.StaticGasProvider;
import vn.edu.kma.blockchain_service.contract.Traceability;
import vn.edu.kma.blockchain_service.domain.GasUsageStatus;
import vn.edu.kma.blockchain_service.dto.BlockchainExecutionResult;
import vn.edu.kma.blockchain_service.dto.request.VerifyHashesRequest;
import vn.edu.kma.blockchain_service.dto.response.BatchRecordResponse;
import vn.edu.kma.blockchain_service.dto.response.TransformedBatchRecordResponse;
import vn.edu.kma.blockchain_service.dto.response.VerifyHashesResponse;
import vn.edu.kma.blockchain_service.dto.request.VerifyTransformedDirectRequest;
import vn.edu.kma.blockchain_service.dto.response.VerifyTransformedDirectResponse;
import vn.edu.kma.blockchain_service.service.TraceabilityService;

import java.math.BigInteger;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.regex.Pattern;
import java.io.ByteArrayOutputStream;
import java.util.Comparator;

@Service
@RequiredArgsConstructor
@Slf4j
public class TraceabilityServiceImpl implements TraceabilityService {

    private static final Pattern BYTES32_HEX_PATTERN = Pattern.compile("^(?:0[xX])?[0-9a-fA-F]{64}$");

    private final Web3j web3j;
    private final Object transactionLock = new Object();

    @Value("${blockchain.wallet.private-key}")
    private String privateKey;

    @Value("${blockchain.contract.address}")
    private String contractAddress;

    @Value("${blockchain.gas.price}")
    private BigInteger gasPrice;

    @Value("${blockchain.gas.limit.deploy}")
    private BigInteger deployGasLimit;

    @Value("${blockchain.gas.limit.record-batch}")
    private BigInteger recordBatchGasLimit;

    @Value("${blockchain.gas.limit.record-transformed-batch}")
    private BigInteger recordTransformedBatchGasLimit;

    @Value("${blockchain.gas.limit.ownership-change}")
    private BigInteger ownershipChangeGasLimit;

    private Credentials credentials;
    private TransactionManager transactionManager;
    private StaticGasProvider deployGasProvider;
    private Traceability recordBatchContract;
    private Traceability recordTransformedBatchContract;
    private Traceability ownershipChangeContract;
    private Traceability readContract;

    @PostConstruct
    void initializeContractClients() {
        credentials = Credentials.create(privateKey);
        transactionManager = new RawTransactionManager(web3j, credentials);
        deployGasProvider = gasProvider(deployGasLimit);
        recordBatchContract = Traceability.load(contractAddress, web3j, transactionManager, gasProvider(recordBatchGasLimit));
        recordTransformedBatchContract = Traceability.load(
                contractAddress, web3j, transactionManager, gasProvider(recordTransformedBatchGasLimit));
        ownershipChangeContract = Traceability.load(
                contractAddress, web3j, transactionManager, gasProvider(ownershipChangeGasLimit));
        readContract = Traceability.load(contractAddress, web3j, credentials, deployGasProvider);
        log.info("Initialized blockchain client for contract={}, wallet={}, gasPrice={}, gasLimits=[deploy={}, recordBatch={}, recordTransformedBatch={}, ownershipChange={}]",
                contractAddress, credentials.getAddress(), gasPrice, deployGasLimit, recordBatchGasLimit,
                recordTransformedBatchGasLimit, ownershipChangeGasLimit);
    }

    @Override
    public String deployContract() throws Exception {
        synchronized (transactionLock) {
            Traceability contract = Traceability.deploy(
                    web3j,
                    transactionManager,
                    deployGasProvider,
                    credentials.getAddress()
            ).send();
            log.info("Contract deployed, contractAddress={}", contract.getContractAddress());
            return contract.getContractAddress();
        }
    }

    @Override
    public BlockchainExecutionResult recordBatch(String batchIdHex, String dataHashHex) throws Exception {
        log.info("recordBatch: batchId={}, dataHash={}", batchIdHex, dataHashHex);
        byte[] batchIdBytes = hexToBytes32(batchIdHex, "batchIdHex");
        byte[] dataHashBytes = hexToBytes32(dataHashHex, "dataHashHex");

        return submitTransaction("recordBatch", () -> {
            TransactionReceipt receipt = recordBatchContract.recordBatch(batchIdBytes, dataHashBytes).send();
            log.info("recordBatch done, txHash={}, status={}", receipt.getTransactionHash(), receipt.getStatus());
            return receipt;
        });
    }

    @Override
    public BlockchainExecutionResult recordTransformedBatch(String batchIdHex, String dataHashHex, List<String> parentHashesHex) throws Exception {
        log.info("recordTransformedBatch: batchId={}, dataHash={}, parents={}", batchIdHex, dataHashHex, parentHashesHex);
        byte[] batchIdBytes = hexToBytes32(batchIdHex, "batchIdHex");
        byte[] dataHashBytes = hexToBytes32(dataHashHex, "dataHashHex");
        List<String> parents = normalizeAndSortHexes(parentHashesHex);
        List<byte[]> parentBytes = new ArrayList<>(parents.size());
        for (int i = 0; i < parents.size(); i++) {
            parentBytes.add(hexToBytes32(parents.get(i), "parentHashesHex[" + i + "]"));
        }

        return submitTransaction("recordTransformedBatch", () -> {
            TransactionReceipt receipt = recordTransformedBatchContract.recordTransformedBatch(batchIdBytes, dataHashBytes, parentBytes).send();
            log.info("recordTransformedBatch done, txHash={}, status={}", receipt.getTransactionHash(), receipt.getStatus());
            return receipt;
        });
    }

    @Override
    public BlockchainExecutionResult logOwnershipChange(String batchIdHex, String fromUserId, String toUserId) throws Exception {
        log.info("logOwnershipChange: batchId={}, from={}, to={}", batchIdHex, fromUserId, toUserId);
        byte[] batchIdBytes = hexToBytes32(batchIdHex, "batchIdHex");
        requireNonBlank(fromUserId, "fromUserId");
        requireNonBlank(toUserId, "toUserId");

        return submitTransaction("logOwnershipChange", () -> {
            TransactionReceipt receipt = ownershipChangeContract.logOwnershipChange(batchIdBytes, fromUserId, toUserId).send();
            log.info("logOwnershipChange done, txHash={}, status={}", receipt.getTransactionHash(), receipt.getStatus());
            return receipt;
        });
    }

    @Override
    public VerifyHashesResponse verifyHashes(VerifyHashesRequest request) {
        if (request == null || request.getItems() == null) {
            throw new IllegalArgumentException("items must not be null");
        }

        List<VerifyHashesResponse.VerifyResult> results = new ArrayList<>();
        for (VerifyHashesRequest.HashItem item : request.getItems()) {
            boolean isMatch = false;
            try {
                byte[] batchIdBytes = hexToBytes32(item.getBatchIdHex(), "batchIdHex");
                String onChainHash = null;
                if ("RAW".equalsIgnoreCase(item.getType())) {
                    onChainHash = bytes32ToHex(readContract.getBatchRecord(batchIdBytes).send().component1());
                } else if ("TRANSFORMED".equalsIgnoreCase(item.getType())) {
                    onChainHash = bytes32ToHex(readContract.getTransformedBatchRecord(batchIdBytes).send().component1());
                }

                if (onChainHash != null) {
                    byte[] inputHashBytes = hexToBytes32(item.getDataHashHex(), "dataHashHex");
                    isMatch = onChainHash.equalsIgnoreCase(bytes32ToHex(inputHashBytes));
                }
            } catch (Exception e) {
                log.error("Error verifying batch {}: {}", item.getBatchIdHex(), e.getMessage());
            }

            results.add(VerifyHashesResponse.VerifyResult.builder()
                    .batchIdHex(item.getBatchIdHex())
                    .isMatch(isMatch)
                    .build());
        }

        return VerifyHashesResponse.builder().results(results).build();
    }

    @Override
    public VerifyTransformedDirectResponse verifyTransformedDirect(VerifyTransformedDirectRequest request) throws Exception {
        if (request == null) {
            throw new IllegalArgumentException("request must not be null");
        }
        byte[] batchIdBytes = hexToBytes32(request.getBatchIdHex(), "batchIdHex");
        byte[] expectedDataHash = hexToBytes32(request.getDataHashHex(), "dataHashHex");
        var record = readContract.getTransformedBatchRecord(batchIdBytes).send();

        String onChainDataHash = bytes32ToHex(record.component1());
        String onChainParentRoot = bytes32ToHex(record.component2());
        String calculatedParentRoot = calculateParentRootHex(request.getParentBatchIdHexes());

        return VerifyTransformedDirectResponse.builder()
                .dataHashMatch(onChainDataHash.equalsIgnoreCase(bytes32ToHex(expectedDataHash)))
                .parentRootMatch(onChainParentRoot.equalsIgnoreCase(calculatedParentRoot))
                .onChainParentRootHex(onChainParentRoot)
                .calculatedParentRootHex(calculatedParentRoot)
                .build();
    }

    @Override
    public BatchRecordResponse getBatchRecord(String batchIdHex) throws Exception {
        var tuple = readContract.getBatchRecord(hexToBytes32(batchIdHex, "batchIdHex")).send();
        return BatchRecordResponse.builder()
                .batchIdHex(batchIdHex)
                .dataHashHex(bytes32ToHex(tuple.component1()))
                .actor(tuple.component2())
                .timestamp(tuple.component3().longValue())
                .build();
    }

    @Override
    public TransformedBatchRecordResponse getTransformedBatchRecord(String batchIdHex) throws Exception {
        var tuple = readContract.getTransformedBatchRecord(hexToBytes32(batchIdHex, "batchIdHex")).send();
        return TransformedBatchRecordResponse.builder()
                .batchIdHex(batchIdHex)
                .dataHashHex(bytes32ToHex(tuple.component1()))
                .parentRootHex(bytes32ToHex(tuple.component2()))
                .actor(tuple.component3())
                .timestamp(tuple.component4().longValue())
                .build();
    }

    @Override
    public boolean hasBatch(String batchIdHex) throws Exception {
        return readContract.hasBatch(hexToBytes32(batchIdHex, "batchIdHex")).send();
    }

    @Override
    public boolean hasTransformedBatch(String batchIdHex) throws Exception {
        return readContract.hasTransformedBatch(hexToBytes32(batchIdHex, "batchIdHex")).send();
    }

    private BlockchainExecutionResult submitTransaction(String operation, BlockchainTransaction transaction) throws Exception {
        synchronized (transactionLock) {
            log.debug("Submitting serialized blockchain transaction: {}", operation);
            Instant submittedAt = Instant.now();
            try {
                TransactionReceipt receipt = transaction.send();
                return toExecutionResult(receipt, submittedAt, null);
            } catch (TransactionException e) {
                TransactionReceipt receipt = e.getTransactionReceipt().orElse(null);
                if (receipt != null) {
                    log.warn("{} failed on-chain, txHash={}, status={}", operation, receipt.getTransactionHash(), receipt.getStatus());
                    return toExecutionResult(receipt, submittedAt, e);
                }
                String txHash = e.getTransactionHash().orElse(null);
                return BlockchainExecutionResult.builder()
                        .status(txHash == null ? GasUsageStatus.SUBMISSION_FAILED : GasUsageStatus.RECEIPT_UNKNOWN)
                        .txHash(txHash)
                        .submittedAt(submittedAt)
                        .errorCode(e.getClass().getSimpleName())
                        .errorMessage(e.getMessage())
                        .build();
            } catch (Exception e) {
                return BlockchainExecutionResult.builder()
                        .status(GasUsageStatus.SUBMISSION_FAILED)
                        .submittedAt(submittedAt)
                        .errorCode(e.getClass().getSimpleName())
                        .errorMessage(e.getMessage())
                        .build();
            }
        }
    }

    private BlockchainExecutionResult toExecutionResult(
            TransactionReceipt receipt,
            Instant submittedAt,
            Exception error) throws Exception {
        BigInteger effectiveGasPrice = resolveEffectiveGasPrice(receipt.getTransactionHash());
        BigInteger gasUsed = receipt.getGasUsed();
        BigInteger feeWei = gasUsed == null || effectiveGasPrice == null ? null : gasUsed.multiply(effectiveGasPrice);
        boolean ok = receipt.isStatusOK();
        return BlockchainExecutionResult.builder()
                .txHash(receipt.getTransactionHash())
                .status(ok ? GasUsageStatus.SUCCESS : GasUsageStatus.FAILED_ON_CHAIN)
                .gasUsed(gasUsed)
                .effectiveGasPriceWei(effectiveGasPrice)
                .feeWei(feeWei)
                .blockNumber(receipt.getBlockNumber())
                .submittedAt(submittedAt)
                .minedAt(Instant.now())
                .errorCode(ok || error == null ? null : error.getClass().getSimpleName())
                .errorMessage(ok || error == null ? null : error.getMessage())
                .build();
    }

    private BigInteger resolveEffectiveGasPrice(String txHash) {
        if (txHash == null || txHash.isBlank()) {
            return gasPrice;
        }
        try {
            return web3j.ethGetTransactionByHash(txHash)
                    .send()
                    .getTransaction()
                    .map(tx -> tx.getGasPrice())
                    .orElse(gasPrice);
        } catch (Exception e) {
            log.warn("Cannot resolve gas price for txHash={}, fallback configured gasPrice={}", txHash, gasPrice);
            return gasPrice;
        }
    }

    private StaticGasProvider gasProvider(BigInteger gasLimit) {
        return new StaticGasProvider(gasPrice, gasLimit);
    }

    private byte[] hexToBytes32(String hex, String fieldName) {
        if (hex == null || !BYTES32_HEX_PATTERN.matcher(hex).matches()) {
            throw new IllegalArgumentException(fieldName + " must be a 32-byte hexadecimal value");
        }

        String normalized = hex.startsWith("0x") || hex.startsWith("0X") ? hex.substring(2) : hex;
        byte[] bytes = new byte[32];
        for (int i = 0; i < 32; i++) {
            int index = i * 2;
            bytes[i] = (byte) Integer.parseInt(normalized.substring(index, index + 2), 16);
        }
        return bytes;
    }

    private List<String> normalizeAndSortHexes(List<String> values) {
        if (values == null || values.isEmpty()) {
            return Collections.emptyList();
        }
        List<String> normalized = new ArrayList<>(values.size());
        for (int i = 0; i < values.size(); i++) {
            normalized.add(bytes32ToHex(hexToBytes32(values.get(i), "parentBatchIdHexes[" + i + "]")));
        }
        normalized.sort(Comparator.comparing(String::toLowerCase));
        return normalized;
    }

    private String calculateParentRootHex(List<String> parentHexes) {
        List<String> parents = normalizeAndSortHexes(parentHexes);
        ByteArrayOutputStream packed = new ByteArrayOutputStream();
        if (parents.isEmpty()) {
            packed.writeBytes(new byte[32]);
        } else {
            for (String parent : parents) {
                packed.writeBytes(hexToBytes32(parent, "parentBatchIdHex"));
            }
        }
        return Numeric.toHexString(Hash.sha3(packed.toByteArray()));
    }

    private void requireNonBlank(String value, String fieldName) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(fieldName + " must not be blank");
        }
    }

    private String bytes32ToHex(byte[] bytes) {
        StringBuilder sb = new StringBuilder("0x");
        for (byte b : bytes) {
            sb.append(String.format("%02x", b));
        }
        return sb.toString();
    }

    @FunctionalInterface
    private interface BlockchainTransaction {
        TransactionReceipt send() throws Exception;
    }
}
