package vn.edu.kma.product_service.service.impl;

import com.nimbusds.jwt.SignedJWT;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.web3j.crypto.Hash;
import org.web3j.utils.Numeric;
import vn.edu.kma.common.dto.response.ApiResponse;
import vn.edu.kma.product_service.dto.request.RawBatchCreateRequest;
import vn.edu.kma.product_service.entity.RawBatch;
import vn.edu.kma.product_service.repository.RawBatchRepository;
import vn.edu.kma.product_service.service.RawBatchService;

import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class RawBatchServiceImpl implements RawBatchService {

    private static final String SCHEMA_VERSION = "1";

    @Value("${spring.url.blockchain-service}")
    private String blockchainBaseUrl;

    private final RestTemplate restTemplate;
    private final RawBatchRepository rawBatchRepository;

    @Override
    public Map<String, String> createRawBatch(RawBatchCreateRequest request, String token) {
        try {
            String producerId = extractUserIdFromToken(token);
            String rawBatchCode = "RAW-" + generateShortCode();
            String batchIdHex = randomBytes32Hex();

            String payload = rawBatchCode + "|" +
                    normalizeString(request.getMaterialType()) + "|" +
                    normalizeString(request.getMaterialName()) + "|" +
                    producerId + "|" +
                    normalizeDate(request.getHarvestedAt()) + "|" +
                    normalizeQuantity(request.getQuantity()) + "|" +
                    normalizeString(request.getUnit()) + "|" +
                    normalizeString(request.getLocation()) + "|" +
                    normalizeString(request.getNote()) + "|" +
                    SCHEMA_VERSION;

            String dataHashHex = keccak256HexUtf8(payload);

            Map<String, String> bcBody = new HashMap<>();
            bcBody.put("batchIdHex", batchIdHex);
            bcBody.put("dataHashHex", dataHashHex);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            HttpEntity<Map<String, String>> entity = new HttpEntity<>(bcBody, headers);

            ResponseEntity<ApiResponse<String>> resp = restTemplate.exchange(
                    blockchainBaseUrl + "/batch",
                    HttpMethod.POST,
                    entity,
                    new ParameterizedTypeReference<ApiResponse<String>>() {}
            );

            ApiResponse<String> api = resp.getBody();
            if (api == null || api.getCode() != 200 || api.getResult() == null) {
                throw new RuntimeException("Anchor RAW failed: " + (api != null ? api.getMessage() : "empty"));
            }
            String txHash = api.getResult();

            RawBatch saved = rawBatchRepository.save(RawBatch.builder()
                    .rawBatchCode(rawBatchCode)
                    .materialType(normalizeString(request.getMaterialType()))
                    .materialName(normalizeString(request.getMaterialName()))
                    .harvestedAt(normalizeDate(request.getHarvestedAt()))
                    .quantity(normalizeQuantity(request.getQuantity()))
                    .unit(normalizeString(request.getUnit()))
                    .location(normalizeString(request.getLocation()))
                    .note(normalizeString(request.getNote()))
                    .schemaVersion(SCHEMA_VERSION)
                    .batchIdHex(batchIdHex)
                    .dataHashHex(dataHashHex)
                    .anchorTxHash(txHash)
                    .actorId(producerId)
                    .ownerId(producerId)
                    .createdAt(LocalDateTime.now())
                    .build());

            Map<String, String> result = new HashMap<>();
            result.put("rawBatchId", saved.getId());
            result.put("rawBatchCode", saved.getRawBatchCode());
            result.put("batchIdHex", saved.getBatchIdHex());
            result.put("dataHashHex", saved.getDataHashHex());
            result.put("anchorTxHash", saved.getAnchorTxHash());
            return result;
        } catch (Exception e) {
            log.error("createRawBatch failed", e);
            throw new RuntimeException("Lỗi tạo RAW batch: " + e.getMessage());
        }
    }

    private static String generateShortCode() {
        return String.valueOf(new SecureRandom().nextInt(1_0000_0000));
    }

    private static String randomBytes32Hex() {
        byte[] b = new byte[32];
        new SecureRandom().nextBytes(b);
        return "0x" + Numeric.toHexStringNoPrefix(b);
    }

    private static String keccak256HexUtf8(String payload) {
        byte[] hash = Hash.sha3(payload.getBytes(StandardCharsets.UTF_8));
        return "0x" + Numeric.toHexStringNoPrefix(hash);
    }

    private static String normalizeDate(String s) {
        return s == null ? "" : s.trim();
    }

    private static String normalizeQuantity(String s) {
        return s == null ? "" : s.trim();
    }

    private static String normalizeString(String s) {
        return s == null ? "" : s.trim();
    }

    private static String extractUserIdFromToken(String tokenHeader) throws Exception {
        String token = tokenHeader;
        if (token.startsWith("Bearer ")) {
            token = token.substring(7);
        }
        SignedJWT signedJWT = SignedJWT.parse(token);
        return signedJWT.getJWTClaimsSet().getStringClaim("userId");
    }
}

