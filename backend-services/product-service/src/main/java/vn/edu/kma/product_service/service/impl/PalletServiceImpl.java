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
import vn.edu.kma.product_service.dto.request.PalletAnchorRequest;
import vn.edu.kma.product_service.dto.response.PalletSummaryResponse;
import vn.edu.kma.product_service.entity.Pallet;
import vn.edu.kma.product_service.repository.PalletRepository;
import vn.edu.kma.product_service.repository.ProductRepository;
import vn.edu.kma.product_service.service.PalletService;

import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class PalletServiceImpl implements PalletService {

    private static final String SCHEMA_VERSION = "1";

    @Value("${spring.url.blockchain-service}")
    private String blockchainBaseUrl;

    private final RestTemplate restTemplate;
    private final ProductRepository productRepository;
    private final PalletRepository palletRepository;

    @Override
    public Map<String, String> anchorPallet(String productId, PalletAnchorRequest request, String tokenHeader) {
        try {
            String processorId = extractUserIdFromToken(tokenHeader);
            if (!productRepository.existsById(productId)) {
                throw new RuntimeException("Product not found: " + productId);
            }

            if (request.getParentRawBatchIdHexes() == null) {
                throw new RuntimeException("parentRawBatchIdHexes is required");
            }
            List<String> parents = request.getParentRawBatchIdHexes().stream()
                    .filter(Objects::nonNull)
                    .map(String::trim)
                    .collect(Collectors.toList());
            if (parents.isEmpty()) {
                throw new RuntimeException("parentRawBatchIdHexes must not be empty");
            }
            parents.sort(String::compareTo);

            String palletCode = "PALLET-" + generateShortCode();
            String chainBatchIdHex = randomBytes32Hex();

            String payload = palletCode + "|" +
                    productId + "|" +
                    normalizeString(request.getPalletName()) + "|" +
                    normalizeString(request.getBatchNo()) + "|" +
                    processorId + "|" +
                    normalizeDate(request.getManufacturedAt()) + "|" +
                    normalizeDate(request.getExpiryAt()) + "|" +
                    normalizeQuantity(request.getQuantity()) + "|" +
                    normalizeString(request.getUnit()) + "|" +
                    normalizeString(request.getPackagingType()) + "|" +
                    normalizeString(request.getProcessingMethod()) + "|" +
                    normalizeString(request.getLocation()) + "|" +
                    normalizeString(request.getNote()) + "|" +
                    SCHEMA_VERSION;
            String dataHashHex = keccak256HexUtf8(payload);

            Map<String, Object> bcBody = new HashMap<>();
            bcBody.put("batchIdHex", chainBatchIdHex);
            bcBody.put("dataHashHex", dataHashHex);
            bcBody.put("parentHashesHex", parents);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(bcBody, headers);
            ResponseEntity<ApiResponse<String>> resp = restTemplate.exchange(
                    blockchainBaseUrl + "/transformed-batch",
                    HttpMethod.POST,
                    entity,
                    new ParameterizedTypeReference<ApiResponse<String>>() {}
            );

            ApiResponse<String> api = resp.getBody();
            if (api == null || api.getCode() != 200 || api.getResult() == null) {
                throw new RuntimeException("Anchor pallet failed: " + (api != null ? api.getMessage() : "empty"));
            }
            String txHash = api.getResult();

            Pallet saved = palletRepository.save(Pallet.builder()
                    .palletCode(palletCode)
                    .palletName(normalizeString(request.getPalletName()))
                    .batchNo(normalizeString(request.getBatchNo()))
                    .manufacturedAt(normalizeDate(request.getManufacturedAt()))
                    .expiryAt(normalizeDate(request.getExpiryAt()))
                    .quantity(normalizeQuantity(request.getQuantity()))
                    .unit(normalizeString(request.getUnit()))
                    .packagingType(normalizeString(request.getPackagingType()))
                    .processingMethod(normalizeString(request.getProcessingMethod()))
                    .location(normalizeString(request.getLocation()))
                    .note(normalizeString(request.getNote()))
                    .schemaVersion(SCHEMA_VERSION)
                    .actorId(processorId)
                    .ownerId(processorId)
                    .chainBatchIdHex(chainBatchIdHex)
                    .dataHashHex(dataHashHex)
                    .anchorTxHash(txHash)
                    .productId(productId)
                    .parentRawBatchIdHexes(String.join(",", parents))
                    .createdAt(LocalDateTime.now())
                    .build());

            Map<String, String> result = new HashMap<>();
            result.put("palletId", saved.getId());
            result.put("palletCode", saved.getPalletCode());
            result.put("chainBatchIdHex", saved.getChainBatchIdHex());
            result.put("dataHashHex", saved.getDataHashHex());
            result.put("anchorTxHash", saved.getAnchorTxHash());
            return result;
        } catch (Exception e) {
            log.error("anchorPallet failed", e);
            throw new RuntimeException("Lỗi anchor pallet: " + e.getMessage());
        }
    }

    @Override
    public List<PalletSummaryResponse> getMyPallets(String tokenHeader) {
        try {
            String ownerId = extractUserIdFromToken(tokenHeader);
            return palletRepository.findAllByOwnerIdOrderByCreatedAtDesc(ownerId).stream()
                    .map(p -> PalletSummaryResponse.builder()
                            .id(p.getId())
                            .palletCode(p.getPalletCode())
                            .palletName(p.getPalletName())
                            .batchNo(p.getBatchNo())
                            .productId(p.getProductId())
                            .createdAt(p.getCreatedAt())
                            .build())
                    .toList();
        } catch (Exception e) {
            log.error("getMyPallets failed", e);
            throw new RuntimeException("Lỗi lấy danh sách lô sản xuất: " + e.getMessage());
        }
    }

    private static String extractUserIdFromToken(String tokenHeader) throws Exception {
        String token = tokenHeader;
        if (token.startsWith("Bearer ")) {
            token = token.substring(7);
        }
        SignedJWT signedJWT = SignedJWT.parse(token);
        return signedJWT.getJWTClaimsSet().getStringClaim("userId");
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
}

