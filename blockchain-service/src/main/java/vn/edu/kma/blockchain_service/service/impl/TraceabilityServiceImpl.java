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
import vn.edu.kma.blockchain_service.dto.response.BlockchainHistoryResponse;
import vn.edu.kma.blockchain_service.service.TraceabilityService;

import java.math.BigInteger;
import java.util.ArrayList;
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

        // Deploy contract
        Traceability contract = Traceability.deploy(
                web3j,
                credentials,
                new DefaultGasProvider()
        ).send();

        String address = contract.getContractAddress();
        log.info("Deploy thành công! Contract Address: {}", address);
        return address;
    }

    @Override
    public String addHistory(String productId, String action, String description) throws Exception {
        log.info("Ghi nhật ký lên Blockchain: ProductID={}, Action={}", productId, action);


        BigInteger gasPrice = BigInteger.valueOf(20_000_000_000L);
        BigInteger gasLimit = BigInteger.valueOf(12_000_000L);

        Credentials credentials = Credentials.create(privateKey);

        TransactionManager txManager = new RawTransactionManager(web3j, credentials);
        StaticGasProvider gasProvider = new StaticGasProvider(
                gasPrice,
                gasLimit
        );

        Traceability contract = Traceability.load(
                contractAddress,
                web3j,
                txManager,
                gasProvider
        );

        // 3. Quan trọng: Sử dụng hàm send() bình thường, bỏ try-catch "hack" đi
        TransactionReceipt receipt = contract.addHistory(productId, action, description).send();
        log.info("Ghi thành công! TxHash: {}", receipt.getTransactionHash());
        return receipt.getTransactionHash();


    }

    @Override
    public int getHistoryCount(String productId) throws Exception {
        Credentials credentials = Credentials.create(privateKey);

        Traceability contract = Traceability.load(
                contractAddress, web3j, credentials, new DefaultGasProvider()
        );
        BigInteger count = contract.getHistoryCount(productId).send();
        return count.intValue();
    }

    @Override
    public List<BlockchainHistoryResponse> getHistoryByProductId(String productId) throws Exception {
        Credentials credentials = Credentials.create(privateKey);

        Traceability contract = Traceability.load(
                contractAddress, web3j, credentials, new DefaultGasProvider()
        );

        int count = contract.getHistoryCount(productId).send().intValue();

        List<BlockchainHistoryResponse> histories = new ArrayList<>();

        for (int i = 0; i < count; i++) {
            var record = contract.getHistory(productId, BigInteger.valueOf(i)).send();
            histories.add(BlockchainHistoryResponse.builder()
                    .productId(record.component1())
                    .action(record.component2())
                    .description(record.component3())
                    .timestamp(record.component4().longValue())
                    .actor(record.component5())
                    .build()
            );
        }
        return histories;

    }
}
