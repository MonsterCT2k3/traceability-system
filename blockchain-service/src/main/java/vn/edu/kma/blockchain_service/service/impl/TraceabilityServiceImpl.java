package vn.edu.kma.blockchain_service.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.web3j.crypto.Credentials;
import org.web3j.protocol.Web3j;
import org.web3j.protocol.core.methods.response.TransactionReceipt;
import org.web3j.tx.RawTransactionManager;
import org.web3j.tx.TransactionManager;
import org.web3j.tx.gas.DefaultGasProvider;
import org.web3j.tx.gas.StaticGasProvider;
import vn.edu.kma.blockchain_service.contract.Traceability;
import vn.edu.kma.blockchain_service.dto.response.BatchRecordResponse;
import vn.edu.kma.blockchain_service.dto.response.TransformedBatchRecordResponse;
import vn.edu.kma.blockchain_service.service.TraceabilityService;

import java.math.BigInteger;
import java.util.Collections;
import java.util.List;



@Service
@RequiredArgsConstructor
@Slf4j
public class TraceabilityServiceImpl implements TraceabilityService {

    private final Web3j web3j;

    @Value("${blockchain.wallet.private-key}")
    private String privateKey;

    @Value("${blockchain.contract.address}")
    private String contractAddress;

    @Override
    public String deployContract() throws Exception {
        log.info("Đang deploy contract lên Blockchain...");
        Credentials credentials = Credentials.create(privateKey);
        String systemWallet = credentials.getAddress();
        // Deploy contract
        Traceability contract = Traceability.deploy(
                web3j,
                credentials,
                new DefaultGasProvider(),
                systemWallet
        ).send();

        String address = contract.getContractAddress();
        log.info("Deploy thành công! Contract Address: {}", address);
        return address;
    }

    @Override
    public String recordBatch(String batchIdHex, String dataHashHex) throws Exception {
        log.info("recordBatch: batchId={}, dataHash={}", batchIdHex, dataHashHex);

        BigInteger gasPrice = BigInteger.valueOf(20_000_000_000L);
        BigInteger gasLimit = BigInteger.valueOf(12_000_000L);

        Credentials credentials = Credentials.create(privateKey);
        TransactionManager txManager = new RawTransactionManager(web3j, credentials);
        StaticGasProvider gasProvider = new StaticGasProvider(gasPrice, gasLimit);

        Traceability contract = Traceability.load(
                contractAddress,
                web3j,
                txManager,
                gasProvider
        );

        byte[] batchIdBytes = hexToBytes32(batchIdHex);
        byte[] dataHashBytes = hexToBytes32(dataHashHex);

        TransactionReceipt receipt = contract.recordBatch(batchIdBytes, dataHashBytes).send();
        log.info("recordBatch OK, txHash={}", receipt.getTransactionHash());
        return receipt.getTransactionHash();
    }

    @Override
    public String recordTransformedBatch(String batchIdHex, String dataHashHex, List<String> parentHashesHex) throws Exception {
        log.info("recordTransformedBatch: batchId={}, dataHash={}, parents={}", batchIdHex, dataHashHex, parentHashesHex);

        BigInteger gasPrice = BigInteger.valueOf(20_000_000_000L);
        BigInteger gasLimit = BigInteger.valueOf(12_000_000L);

        Credentials credentials = Credentials.create(privateKey);
        TransactionManager txManager = new RawTransactionManager(web3j, credentials);
        StaticGasProvider gasProvider = new StaticGasProvider(gasPrice, gasLimit);

        Traceability contract = Traceability.load(
                contractAddress,
                web3j,
                txManager,
                gasProvider
        );

        byte[] batchIdBytes = hexToBytes32(batchIdHex);
        byte[] dataHashBytes = hexToBytes32(dataHashHex);
        List<String> parents = parentHashesHex != null ? parentHashesHex : Collections.emptyList();
        List<byte[]> parentBytes = parents.stream()
                .map(this::hexToBytes32)
                .toList();

        TransactionReceipt receipt = contract.recordTransformedBatch(batchIdBytes, dataHashBytes, parentBytes).send();
        log.info("recordTransformedBatch OK, txHash={}", receipt.getTransactionHash());
        return receipt.getTransactionHash();
    }

    @Override
    public String logOwnershipChange(String batchIdHex, String fromUserId, String toUserId) throws Exception {
        log.info("logOwnershipChange: batchId={}, from={}, to={}", batchIdHex, fromUserId, toUserId);

        BigInteger gasPrice = BigInteger.valueOf(20_000_000_000L);
        BigInteger gasLimit = BigInteger.valueOf(12_000_000L);

        Credentials credentials = Credentials.create(privateKey);
        TransactionManager txManager = new RawTransactionManager(web3j, credentials);
        StaticGasProvider gasProvider = new StaticGasProvider(gasPrice, gasLimit);

        Traceability contract = Traceability.load(
                contractAddress,
                web3j,
                txManager,
                gasProvider
        );

        byte[] batchIdBytes = hexToBytes32(batchIdHex);
        TransactionReceipt receipt = contract.logOwnershipChange(batchIdBytes, fromUserId, toUserId).send();
        log.info("logOwnershipChange OK, txHash={}", receipt.getTransactionHash());
        return receipt.getTransactionHash();
    }

    @Override
    public BatchRecordResponse getBatchRecord(String batchIdHex) throws Exception {
        Credentials credentials = Credentials.create(privateKey);
        Traceability contract = Traceability.load(
                contractAddress, web3j, credentials, new DefaultGasProvider()
        );

        byte[] batchIdBytes = hexToBytes32(batchIdHex);
        var tuple = contract.getBatchRecord(batchIdBytes).send();

        return BatchRecordResponse.builder()
                .batchIdHex(batchIdHex)
                .dataHashHex(bytes32ToHex(tuple.component1()))
                .actor(tuple.component2())
                .timestamp(tuple.component3().longValue())
                .build();
    }

    @Override
    public TransformedBatchRecordResponse getTransformedBatchRecord(String batchIdHex) throws Exception {
        Credentials credentials = Credentials.create(privateKey);
        Traceability contract = Traceability.load(
                contractAddress, web3j, credentials, new DefaultGasProvider()
        );

        byte[] batchIdBytes = hexToBytes32(batchIdHex);
        var tuple = contract.getTransformedBatchRecord(batchIdBytes).send();

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
        Credentials credentials = Credentials.create(privateKey);
        Traceability contract = Traceability.load(
                contractAddress, web3j, credentials, new DefaultGasProvider()
        );
        return contract.hasBatch(hexToBytes32(batchIdHex)).send();
    }

    @Override
    public boolean hasTransformedBatch(String batchIdHex) throws Exception {
        Credentials credentials = Credentials.create(privateKey);
        Traceability contract = Traceability.load(
                contractAddress, web3j, credentials, new DefaultGasProvider()
        );
        return contract.hasTransformedBatch(hexToBytes32(batchIdHex)).send();
    }


    private byte[] hexToBytes32(String hex) {
        // chấp nhận "0x..." hoặc không
        if (hex.startsWith("0x") || hex.startsWith("0X")) {
            hex = hex.substring(2);
        }
        if (hex.length() != 64) {
            throw new IllegalArgumentException("Hex length must be 64 for bytes32");
        }
        byte[] bytes = new byte[32];
        for (int i = 0; i < 32; i++) {
            int index = i * 2;
            bytes[i] = (byte) Integer.parseInt(hex.substring(index, index + 2), 16);
        }
        return bytes;
    }

    private String bytes32ToHex(byte[] bytes) {
        StringBuilder sb = new StringBuilder("0x");
        for (byte b : bytes) {
            sb.append(String.format("%02x", b));
        }
        return sb.toString();
    }
}
