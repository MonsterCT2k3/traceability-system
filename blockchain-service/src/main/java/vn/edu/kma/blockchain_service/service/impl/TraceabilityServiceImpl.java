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
import vn.edu.kma.blockchain_service.dto.request.VerifyHashesRequest;
import vn.edu.kma.blockchain_service.dto.response.BatchRecordResponse;
import vn.edu.kma.blockchain_service.dto.response.TransformedBatchRecordResponse;
import vn.edu.kma.blockchain_service.dto.response.VerifyHashesResponse;
import vn.edu.kma.blockchain_service.service.TraceabilityService;

import java.math.BigInteger;
import java.util.ArrayList;
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
    public VerifyHashesResponse verifyHashes(VerifyHashesRequest request) throws Exception {
        List<VerifyHashesResponse.VerifyResult> results = new ArrayList<>();
        
        // 1. Tải Smart Contract từ Blockchain
        // Dùng credentials (tài khoản hệ thống) và contractAddress để tạo đối tượng giao tiếp với Blockchain
        Credentials credentials = Credentials.create(privateKey);
        Traceability contract = Traceability.load(
                contractAddress, web3j, credentials, new DefaultGasProvider()
        );

        // 2. Duyệt qua từng mã Hash mà Product-Service gửi sang để nhờ xác thực
        for (VerifyHashesRequest.HashItem item : request.getItems()) {
            boolean isMatch = false; // Mặc định là không khớp (dữ liệu bị sai lệch)
            try {
                // Chuyển ID Lô hàng từ chuỗi String (Hex) sang mảng byte (Bytes32) cho đúng chuẩn Solidity
                byte[] batchIdBytes = hexToBytes32(item.getBatchIdHex());
                String onChainHash = null;

                // 3. Đọc dữ liệu Gốc từ Blockchain (Chỉ gọi hàm read, không tốn phí Gas)
                if ("RAW".equalsIgnoreCase(item.getType())) {
                    // Nếu là Nguyên liệu gốc
                    var tuple = contract.getBatchRecord(batchIdBytes).send();
                    onChainHash = bytes32ToHex(tuple.component1()); // Lấy Hash đã lưu lúc khai báo
                } else if ("TRANSFORMED".equalsIgnoreCase(item.getType())) {
                    // Nếu là Lô hàng chế biến (Pallet)
                    var tuple = contract.getTransformedBatchRecord(batchIdBytes).send();
                    onChainHash = bytes32ToHex(tuple.component1()); // Lấy Hash đã lưu lúc sản xuất
                }

                // 4. Đối chiếu (So sánh)
                if (onChainHash != null) {
                    String inputHash = item.getDataHashHex(); // Mã Hash tính bằng dữ liệu hiện tại ở DB
                    if (inputHash != null) {
                        // Thêm tiền tố 0x nếu chưa có để đồng nhất format
                        if (!inputHash.startsWith("0x")) inputHash = "0x" + inputHash;
                        
                        // CHỐT HẠ: So sánh Mã Hash lưu trên mạng lưới và Mã Hash gửi lên
                        // Nếu 2 mã giống hệt nhau -> Dữ liệu ở DB chưa hề bị sửa đổi
                        isMatch = onChainHash.equalsIgnoreCase(inputHash);
                    }
                }
            } catch (Exception e) {
                log.error("Error verifying batch {}: {}", item.getBatchIdHex(), e.getMessage());
            }

            // 5. Ghi nhận kết quả của Lô này vào danh sách
            results.add(VerifyHashesResponse.VerifyResult.builder()
                    .batchIdHex(item.getBatchIdHex())
                    .isMatch(isMatch)
                    .build());
        }

        // 6. Trả toàn bộ danh sách kết quả (true/false) về cho Product-Service
        return VerifyHashesResponse.builder().results(results).build();
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
