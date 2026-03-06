package vn.edu.kma.blockchain_service.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.web3j.protocol.Web3j;
import org.web3j.protocol.http.HttpService;

@Configuration
public class Web3Config {

    @Value("${web3j.client-address}")
    private String clientAddress;

    @Bean
    public Web3j web3j() {
        // Kết nối tới Ganache hoặc Node bất kỳ
        return Web3j.build(new HttpService(clientAddress));
    }
}
