package vn.edu.kma.product_service.service.impl;

import com.nimbusds.jwt.SignedJWT;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.web.client.RestTemplate;
import org.web3j.crypto.Hash;
import org.web3j.utils.Numeric;
import vn.edu.kma.common.dto.response.ApiResponse;
import vn.edu.kma.product_service.dto.request.RawBatchCreateRequest;
import vn.edu.kma.product_service.dto.request.RawBatchMergeRequest;
import vn.edu.kma.product_service.dto.response.RawBatchResponse;
import vn.edu.kma.product_service.entity.RawBatch;
import vn.edu.kma.product_service.repository.RawBatchRepository;
import vn.edu.kma.product_service.service.RawBatchService;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

@Service
@Slf4j
public class RawBatchServiceImpl implements RawBatchService {

    private static final String SCHEMA_VERSION = "1";

    @Value("${spring.url.blockchain-service}")
    private String blockchainBaseUrl;

    private final RestTemplate restTemplate;
    private final RawBatchRepository rawBatchRepository;
    private final TransactionTemplate transactionTemplate;

    public RawBatchServiceImpl(RestTemplate restTemplate,
                               RawBatchRepository rawBatchRepository,
                               PlatformTransactionManager transactionManager) {
        this.restTemplate = restTemplate;
        this.rawBatchRepository = rawBatchRepository;
        this.transactionTemplate = new TransactionTemplate(transactionManager);
    }

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
                    .status("NOT_SHIPPED")
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

    @Override
    public Map<String, String> mergeRawBatches(RawBatchMergeRequest request, String token) {
        try {
            String ownerId = extractUserIdFromToken(token);
            if (request == null || request.getSourceRawBatchIds() == null || request.getSourceRawBatchIds().size() < 2) {
                throw new RuntimeException("Cần ít nhất 2 lô nguồn để gộp");
            }
            List<String> ids = request.getSourceRawBatchIds().stream()
                    .filter(Objects::nonNull)
                    .map(String::trim)
                    .filter(s -> !s.isEmpty())
                    .distinct()
                    .sorted()
                    .toList();
            if (ids.size() < 2) {
                throw new RuntimeException("Cần ít nhất 2 lô nguồn khác nhau");
            }

            List<RawBatch> sources = rawBatchRepository.findAllByIdIn(ids);
            if (sources.size() != ids.size()) {
                throw new RuntimeException("Một hoặc nhiều lô không tồn tại");
            }

            for (RawBatch b : sources) {
                if (!ownerId.equals(b.getOwnerId())) {
                    throw new RuntimeException("Chỉ được gộp các lô thuộc quyền sở hữu của bạn");
                }
            }

            RawBatch first = sources.get(0);
            String mt = first.getMaterialType();
            String mn = first.getMaterialName();
            for (RawBatch b : sources) {
                if (!mt.equals(b.getMaterialType()) || !mn.equals(b.getMaterialName())) {
                    throw new RuntimeException("Chỉ gộp được các lô cùng loại (materialType + materialName)");
                }
            }

            List<RawBatch> sorted = new ArrayList<>(sources);
            sorted.sort(Comparator.comparing(RawBatch::getId));

            BigDecimal sumQty = BigDecimal.ZERO;
            for (RawBatch b : sources) {
                sumQty = sumQty.add(parseQuantity(b.getQuantity()));
            }
            String quantityStr = formatQuantity(sumQty);

            String harvestedAt = sorted.stream()
                    .map(RawBatch::getHarvestedAt)
                    .filter(h -> h != null && !h.isBlank())
                    .min(String::compareTo)
                    .orElse("");

            String unit = first.getUnit();
            String locInput = normalizeString(request.getLocation());
            final String mergeLocation = locInput.isEmpty() ? first.getLocation() : locInput;

            String note = normalizeString(request.getNote());
            String rawBatchCode = "RAW-" + generateShortCode();
            String batchIdHex = randomBytes32Hex();

            String payload = rawBatchCode + "|" +
                    normalizeString(mt) + "|" +
                    normalizeString(mn) + "|" +
                    ownerId + "|" +
                    normalizeDate(harvestedAt) + "|" +
                    normalizeQuantity(quantityStr) + "|" +
                    normalizeString(unit) + "|" +
                    normalizeString(mergeLocation) + "|" +
                    note + "|" +
                    SCHEMA_VERSION;

            String dataHashHex = keccak256HexUtf8(payload);

            final List<RawBatch> sourceSnapshots = sources.stream()
                    .map(RawBatchServiceImpl::snapshotCopy)
                    .toList();

            final List<String> sourceIds = ids;
            RawBatch saved = transactionTemplate.execute(status -> {
                rawBatchRepository.deleteAllById(sourceIds);
                return rawBatchRepository.save(RawBatch.builder()
                        .rawBatchCode(rawBatchCode)
                        .materialType(mt)
                        .materialName(mn)
                        .harvestedAt(harvestedAt)
                        .quantity(quantityStr)
                        .unit(unit)
                        .location(mergeLocation)
                        .note(note)
                        .schemaVersion(SCHEMA_VERSION)
                        .batchIdHex(batchIdHex)
                        .dataHashHex(dataHashHex)
                        .anchorTxHash(null)
                        .actorId(ownerId)
                        .ownerId(ownerId)
                        .status("NOT_SHIPPED")
                        .createdAt(LocalDateTime.now())
                        .build());
            });

            if (saved == null) {
                throw new RuntimeException("Gộp lô — không lưu được DB");
            }

            try {
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
                    throw new RuntimeException(api != null ? api.getMessage() : "empty response");
                }
                String txHash = api.getResult();

                final String mergedId = saved.getId();
                transactionTemplate.execute(status -> {
                    RawBatch r = rawBatchRepository.findById(mergedId).orElseThrow();
                    r.setAnchorTxHash(txHash);
                    rawBatchRepository.save(r);
                    return null;
                });

                saved.setAnchorTxHash(txHash);
            } catch (Exception chainEx) {
                log.error("mergeRawBatches: blockchain failed, rolling back DB", chainEx);
                try {
                    transactionTemplate.execute(status -> {
                        rawBatchRepository.deleteById(saved.getId());
                        rawBatchRepository.saveAll(sourceSnapshots);
                        for (RawBatch snap : sourceSnapshots) {
                            RawBatch r = rawBatchRepository.findById(snap.getId()).orElseThrow();
                            r.setCreatedAt(snap.getCreatedAt());
                            rawBatchRepository.save(r);
                        }
                        return null;
                    });
                } catch (Exception compensateEx) {
                    log.error("mergeRawBatches: compensate failed after chain error", compensateEx);
                    throw new RuntimeException(
                            "Neo blockchain thất bại và không hoàn tác được DB: " + compensateEx.getMessage(),
                            compensateEx);
                }
                throw new RuntimeException("Gộp lô — neo blockchain thất bại: " + chainEx.getMessage(), chainEx);
            }

            Map<String, String> result = new HashMap<>();
            result.put("rawBatchId", saved.getId());
            result.put("rawBatchCode", saved.getRawBatchCode());
            result.put("batchIdHex", saved.getBatchIdHex());
            result.put("dataHashHex", saved.getDataHashHex());
            result.put("anchorTxHash", saved.getAnchorTxHash());
            return result;
        } catch (Exception e) {
            log.error("mergeRawBatches failed", e);
            throw new RuntimeException("Lỗi gộp lô: " + e.getMessage());
        }
    }

    /** Bản sao để khôi phục lô nguồn nếu neo chain thất bại (sau khi đã xóa trên DB). */
    private static RawBatch snapshotCopy(RawBatch b) {
        return RawBatch.builder()
                .id(b.getId())
                .rawBatchCode(b.getRawBatchCode())
                .materialType(b.getMaterialType())
                .materialName(b.getMaterialName())
                .harvestedAt(b.getHarvestedAt())
                .quantity(b.getQuantity())
                .unit(b.getUnit())
                .location(b.getLocation())
                .note(b.getNote())
                .schemaVersion(b.getSchemaVersion())
                .batchIdHex(b.getBatchIdHex())
                .dataHashHex(b.getDataHashHex())
                .anchorTxHash(b.getAnchorTxHash())
                .actorId(b.getActorId())
                .ownerId(b.getOwnerId())
                .createdAt(b.getCreatedAt())
                .status(b.getStatus())
                .build();
    }

    private static BigDecimal parseQuantity(String q) {
        if (q == null || q.isBlank()) {
            return BigDecimal.ZERO;
        }
        String s = q.trim().replace(',', '.');
        try {
            return new BigDecimal(s);
        } catch (NumberFormatException e) {
            throw new RuntimeException("Số lượng không hợp lệ: " + q);
        }
    }

    private static String formatQuantity(BigDecimal bd) {
        return bd.stripTrailingZeros().toPlainString();
    }

    @Override
    public List<RawBatchResponse> getMyRawBatches(String token) {
        try {
            String producerId = extractUserIdFromToken(token);
            List<RawBatch> batches = rawBatchRepository.findAllByOwnerIdOrderByCreatedAtDesc(producerId);
            
            return batches.stream().map(batch -> RawBatchResponse.builder()
                    .id(batch.getId())
                    .rawBatchCode(batch.getRawBatchCode())
                    .materialType(batch.getMaterialType())
                    .materialName(batch.getMaterialName())
                    .harvestedAt(batch.getHarvestedAt())
                    .quantity(batch.getQuantity())
                    .unit(batch.getUnit())
                    .location(batch.getLocation())
                    .note(batch.getNote())
                    .status(batch.getStatus())
                    .batchIdHex(batch.getBatchIdHex())
                    .anchorTxHash(batch.getAnchorTxHash())
                    .createdAt(batch.getCreatedAt())
                    .build()).collect(Collectors.toList());
        } catch (Exception e) {
            log.error("getMyRawBatches failed", e);
            throw new RuntimeException("Lỗi lấy danh sách lô nguyên liệu: " + e.getMessage());
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

