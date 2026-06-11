package vn.edu.kma.traceability_core_service.service.impl;

import com.nimbusds.jwt.SignedJWT;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.edu.kma.traceability_core_service.service.BlockchainClient;
import org.web3j.crypto.Hash;
import org.web3j.utils.Numeric;
import vn.edu.kma.common.dto.response.ApiResponse;
import vn.edu.kma.traceability_core_service.dto.request.PalletAnchorRequest;
import vn.edu.kma.traceability_core_service.dto.request.PalletInputRequest;
import vn.edu.kma.traceability_core_service.dto.request.VerifyHashesRequest;
import vn.edu.kma.traceability_core_service.dto.request.VerifyTransformedDirectRequest;
import vn.edu.kma.traceability_core_service.dto.response.DirectTraceResponse;
import vn.edu.kma.traceability_core_service.dto.response.DirectVerificationSummary;
import vn.edu.kma.traceability_core_service.dto.response.PalletSummaryResponse;
import vn.edu.kma.traceability_core_service.dto.response.TraceNodeResponse;
import vn.edu.kma.traceability_core_service.dto.response.VerifyHashesResponse;
import vn.edu.kma.traceability_core_service.dto.response.VerifyTransformedDirectResponse;
import vn.edu.kma.traceability_core_service.domain.PalletInputStatus;
import vn.edu.kma.traceability_core_service.domain.PalletInputType;
import vn.edu.kma.traceability_core_service.entity.PalletInput;
import vn.edu.kma.traceability_core_service.entity.RawBatch;
import vn.edu.kma.traceability_core_service.entity.Pallet;
import vn.edu.kma.traceability_core_service.repository.PalletInputRepository;
import vn.edu.kma.traceability_core_service.repository.PalletRepository;
import vn.edu.kma.traceability_core_service.repository.RawBatchRepository;
import vn.edu.kma.traceability_core_service.client.CatalogClient;
import vn.edu.kma.traceability_core_service.service.IdentityClient;
import vn.edu.kma.traceability_core_service.service.PalletService;

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

    private final BlockchainClient blockchainClient;
    private final CatalogClient catalogClient;
    private final IdentityClient identityClient;
    private final PalletRepository palletRepository;
    private final PalletInputRepository palletInputRepository;
    private final RawBatchRepository rawBatchRepository;
    private final org.springframework.kafka.core.KafkaTemplate<String, Object> kafkaTemplate;

    @Override
    @Transactional
    public Map<String, String> anchorPallet(String productId, PalletAnchorRequest request, String tokenHeader) {
        try {
            String processorId = extractUserIdFromToken(tokenHeader);
            String processorRole = extractRoleFromToken(tokenHeader);
            ApiResponse<Map<String, Object>> pRes = catalogClient.getProductById(productId);
            if (pRes == null || pRes.getResult() == null) {
                throw new RuntimeException("Product not found: " + productId);
            }
            if (!processorId.equals(String.valueOf(pRes.getResult().get("ownerId")))) {
                throw new RuntimeException("Ban khong so huu catalog san pham nay");
            }

            List<ResolvedInput> resolvedInputs = resolveInputs(request, processorId);
            List<String> parents = resolvedInputs.stream()
                    .map(ResolvedInput::batchIdHex)
                    .sorted(String.CASE_INSENSITIVE_ORDER)
                    .toList();

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
                    .manufacturerId(processorId)
                    .inputStatus(PalletInputStatus.AVAILABLE)
                    .chainBatchIdHex(chainBatchIdHex)
                    .dataHashHex(dataHashHex)
                    .anchorTxHash(null) // PENDING
                    .productId(productId)
                    .parentRawBatchIdHexes(String.join(",", parents))
                    .createdAt(LocalDateTime.now())
                    .build());

            for (ResolvedInput input : resolvedInputs) {
                palletInputRepository.save(PalletInput.builder()
                        .outputPalletId(saved.getId())
                        .inputType(input.type())
                        .inputId(input.id())
                        .inputBatchIdHex(input.batchIdHex())
                        .build());
                if (input.type() == PalletInputType.PALLET) {
                    Pallet inputPallet = palletRepository.findById(input.id()).orElseThrow();
                    inputPallet.setInputStatus(PalletInputStatus.CONSUMED);
                    palletRepository.save(inputPallet);
                }
            }

            vn.edu.kma.traceability_core_service.event.BlockchainRecordTransformedBatchEvent event = 
                vn.edu.kma.traceability_core_service.event.BlockchainRecordTransformedBatchEvent.builder()
                    .entityId(saved.getId())
                    .entityType("PALLET")
                    .batchIdHex(chainBatchIdHex)
                    .dataHashHex(dataHashHex)
                    .parentHashesHex(parents)
                    .requestId("pallet:" + saved.getId() + ":anchor")
                    .operation("RECORD_TRANSFORMED_BATCH")
                    .billingActorId(processorId)
                    .billingRole(processorRole)
                    .initiatedByUserId(processorId)
                    .sourceService("traceability-core-service")
                    .build();
            kafkaTemplate.send("blockchain.requests.transformed", event);

            Map<String, String> result = new HashMap<>();
            result.put("palletId", saved.getId());
            result.put("palletCode", saved.getPalletCode());
            result.put("chainBatchIdHex", saved.getChainBatchIdHex());
            result.put("dataHashHex", saved.getDataHashHex());
            result.put("anchorTxHash", "PENDING");
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
            return palletRepository.findAllByOwnerIdOrderByCreatedAtDesc(ownerId).stream().map(this::toSummary).toList();
        } catch (Exception e) {
            log.error("getMyPallets failed", e);
            throw new RuntimeException("Lỗi lấy danh sách lô sản xuất: " + e.getMessage());
        }
    }

    @Override
    public List<PalletSummaryResponse> getPalletsByOwner(String ownerId) {
        return palletRepository.findAllByOwnerIdAndInputStatusOrderByCreatedAtDesc(ownerId, PalletInputStatus.AVAILABLE)
                .stream().map(this::toSummary).toList();
    }

    private PalletSummaryResponse toSummary(Pallet p) {
        return PalletSummaryResponse.builder()
                .id(p.getId()).palletCode(p.getPalletCode()).palletName(p.getPalletName())
                .batchNo(p.getBatchNo()).productId(p.getProductId())
                .inputStatus((p.getInputStatus() == null ? PalletInputStatus.AVAILABLE : p.getInputStatus()).name())
                .ownerId(p.getOwnerId()).manufacturerId(p.getManufacturerId())
                .hasInputs(!palletInputRepository.findByOutputPalletIdOrderByCreatedAtAsc(p.getId()).isEmpty())
                .createdAt(p.getCreatedAt()).build();
    }

    @Override
    @Transactional(readOnly = true)
    public DirectTraceResponse getDirectTrace(String palletId) {
        return buildDirectTrace(palletId, false);
    }

    @Override
    @Transactional(readOnly = true)
    public DirectTraceResponse verifyDirectTrace(String palletId) {
        return buildDirectTrace(palletId, true);
    }

    private DirectTraceResponse buildDirectTrace(String palletId, boolean verify) {
        Pallet pallet = palletRepository.findById(palletId)
                .orElseThrow(() -> new RuntimeException("Pallet không tồn tại"));
        List<PalletInput> inputs = palletInputRepository.findByOutputPalletIdOrderByCreatedAtAsc(palletId);
        Map<String, Boolean> inputVerification = new HashMap<>();
        String currentStatus = "NOT_VERIFIED";
        String relationStatus = "NOT_VERIFIED";

        if (verify) {
            List<String> parentIds = inputs.stream().map(PalletInput::getInputBatchIdHex)
                    .sorted(String.CASE_INSENSITIVE_ORDER).toList();
            ApiResponse<VerifyTransformedDirectResponse> transformed = blockchainClient.verifyTransformedDirect(
                    VerifyTransformedDirectRequest.builder()
                            .batchIdHex(pallet.getChainBatchIdHex())
                            .dataHashHex(calculatePalletHash(pallet))
                            .parentBatchIdHexes(parentIds)
                            .build());
            VerifyTransformedDirectResponse directResult = transformed == null ? null : transformed.getResult();
            currentStatus = directResult != null && directResult.isDataHashMatch() ? "VERIFIED" : "MISMATCH";
            relationStatus = directResult != null && directResult.isParentRootMatch() ? "VERIFIED" : "MISMATCH";

            if (!inputs.isEmpty()) {
                List<VerifyHashesRequest.HashItem> items = inputs.stream().map(this::toHashItem).toList();
                ApiResponse<VerifyHashesResponse> response = blockchainClient.verifyHashes(
                        VerifyHashesRequest.builder().items(items).build());
                if (response != null && response.getResult() != null) {
                    response.getResult().getResults().forEach(r -> inputVerification.put(r.getBatchIdHex(), r.isMatch()));
                }
            }
        }

        List<TraceNodeResponse> directInputs = inputs.stream()
                .map(input -> resolveTraceNode(input, verify, inputVerification))
                .toList();
        long verifiedCount = directInputs.stream().filter(n -> "VERIFIED".equals(n.getVerificationStatus())).count();
        String overall = !verify ? "NOT_VERIFIED"
                : ("VERIFIED".equals(currentStatus) && "VERIFIED".equals(relationStatus)
                    && verifiedCount == directInputs.size() ? "VERIFIED" : "MISMATCH");

        return DirectTraceResponse.builder()
                .currentNode(toPalletNode(pallet, currentStatus))
                .directInputs(directInputs)
                .verificationScope("CURRENT_AND_DIRECT_INPUTS")
                .verificationSummary(DirectVerificationSummary.builder()
                        .currentNodeStatus(currentStatus)
                        .inputRelationStatus(relationStatus)
                        .verifiedInputCount((int) verifiedCount)
                        .totalInputCount(directInputs.size())
                        .overallStatus(overall)
                        .build())
                .build();
    }

    private TraceNodeResponse resolveTraceNode(PalletInput input, boolean verify, Map<String, Boolean> statuses) {
        String status = !verify ? "NOT_VERIFIED"
                : Boolean.TRUE.equals(statuses.get(input.getInputBatchIdHex())) ? "VERIFIED" : "MISMATCH";
        if (input.getInputType() == PalletInputType.RAW_BATCH) {
            RawBatch raw = rawBatchRepository.findById(input.getInputId())
                    .orElseThrow(() -> new RuntimeException("RawBatch input không tồn tại: " + input.getInputId()));
            ActorInfo actor = resolveActor(raw.getActorId());
            return TraceNodeResponse.builder()
                    .id(raw.getId()).nodeType("RAW_BATCH").code(raw.getRawBatchCode()).name(raw.getMaterialName())
                    .actorId(raw.getActorId()).actorName(actor.name()).actorAvatarUrl(actor.avatarUrl())
                    .location(raw.getLocation()).occurredAt(raw.getHarvestedAt())
                    .quantity(raw.getQuantity()).unit(raw.getUnit()).note(raw.getNote())
                    .blockchainBatchIdHex(raw.getBatchIdHex()).hasInputs(false).verificationStatus(status).build();
        }
        Pallet parent = palletRepository.findById(input.getInputId())
                .orElseThrow(() -> new RuntimeException("Pallet input không tồn tại: " + input.getInputId()));
        return toPalletNode(parent, status);
    }

    private TraceNodeResponse toPalletNode(Pallet pallet, String status) {
        ActorInfo actor = resolveActor(pallet.getActorId());
        return TraceNodeResponse.builder()
                .id(pallet.getId()).nodeType("PALLET").code(pallet.getPalletCode()).name(pallet.getPalletName())
                .actorId(pallet.getActorId()).actorName(actor.name()).actorAvatarUrl(actor.avatarUrl())
                .location(pallet.getLocation()).occurredAt(pallet.getManufacturedAt())
                .quantity(pallet.getQuantity()).unit(pallet.getUnit()).note(pallet.getNote())
                .batchNo(pallet.getBatchNo()).expiryAt(pallet.getExpiryAt())
                .packagingType(pallet.getPackagingType()).processingMethod(pallet.getProcessingMethod())
                .blockchainBatchIdHex(pallet.getChainBatchIdHex())
                .hasInputs(!palletInputRepository.findByOutputPalletIdOrderByCreatedAtAsc(pallet.getId()).isEmpty())
                .verificationStatus(status).build();
    }

    private VerifyHashesRequest.HashItem toHashItem(PalletInput input) {
        if (input.getInputType() == PalletInputType.RAW_BATCH) {
            RawBatch raw = rawBatchRepository.findById(input.getInputId()).orElseThrow();
            return VerifyHashesRequest.HashItem.builder().batchIdHex(raw.getBatchIdHex())
                    .dataHashHex(vn.edu.kma.traceability_core_service.utils.BlockchainVerificationUtils.calculateRawBatchHash(raw))
                    .type("RAW").build();
        }
        Pallet pallet = palletRepository.findById(input.getInputId()).orElseThrow();
        return VerifyHashesRequest.HashItem.builder().batchIdHex(pallet.getChainBatchIdHex())
                .dataHashHex(calculatePalletHash(pallet)).type("TRANSFORMED").build();
    }

    private List<ResolvedInput> resolveInputs(PalletAnchorRequest request, String ownerId) {
        List<ResolvedInput> result = new ArrayList<>();
        Set<String> uniqueInputs = new HashSet<>();
        if (request.getInputs() != null && !request.getInputs().isEmpty()) {
            for (PalletInputRequest input : request.getInputs()) {
                if (input == null || input.getType() == null || input.getId() == null || input.getId().isBlank()) {
                    throw new RuntimeException("Input pallet/raw batch không hợp lệ");
                }
                String key = input.getType() + ":" + input.getId().trim();
                if (!uniqueInputs.add(key)) throw new RuntimeException("Input bị trùng: " + key);
                if (input.getType() == PalletInputType.RAW_BATCH) {
                    RawBatch raw = resolveRawBatchInput(input.getId().trim());
                    if (!ownerId.equals(raw.getOwnerId())) throw new RuntimeException("Bạn không sở hữu RawBatch input");
                    result.add(new ResolvedInput(PalletInputType.RAW_BATCH, raw.getId(), raw.getBatchIdHex()));
                } else {
                    Pallet pallet = palletRepository.findByIdForUpdate(input.getId().trim())
                            .orElseThrow(() -> new RuntimeException("Pallet input không tồn tại"));
                    if (!ownerId.equals(pallet.getOwnerId())) throw new RuntimeException("Bạn không sở hữu Pallet input");
                    if (pallet.getInputStatus() != PalletInputStatus.AVAILABLE) {
                        throw new RuntimeException("Pallet input không ở trạng thái AVAILABLE");
                    }
                    result.add(new ResolvedInput(PalletInputType.PALLET, pallet.getId(), pallet.getChainBatchIdHex()));
                }
            }
        } else if (request.getParentRawBatchIdHexes() != null) {
            for (String hex : request.getParentRawBatchIdHexes()) {
                if (hex == null || hex.isBlank()) continue;
                RawBatch raw = rawBatchRepository.findByBatchIdHex(hex.trim())
                        .orElseThrow(() -> new RuntimeException("RawBatch input không tồn tại: " + hex));
                if (!ownerId.equals(raw.getOwnerId())) throw new RuntimeException("Bạn không sở hữu RawBatch input");
                if (uniqueInputs.add("RAW_BATCH:" + raw.getId())) {
                    result.add(new ResolvedInput(PalletInputType.RAW_BATCH, raw.getId(), raw.getBatchIdHex()));
                }
            }
        }
        if (result.isEmpty()) throw new RuntimeException("Cần chọn ít nhất một input sản xuất");
        return result;
    }

    private RawBatch resolveRawBatchInput(String idOrBatchIdHex) {
        return rawBatchRepository.findById(idOrBatchIdHex)
                .or(() -> rawBatchRepository.findByBatchIdHex(idOrBatchIdHex))
                .orElseThrow(() -> new RuntimeException("RawBatch input không tồn tại"));
    }

    private static String calculatePalletHash(Pallet pallet) {
        return vn.edu.kma.traceability_core_service.utils.BlockchainVerificationUtils.calculatePalletHash(pallet);
    }

    private ActorInfo resolveActor(String actorId) {
        if (actorId == null || actorId.isBlank()) return new ActorInfo(null, null);
        try {
            ApiResponse<Map<String, Object>> response = identityClient.getUserById(actorId);
            Map<String, Object> result = response == null ? null : response.getResult();
            return new ActorInfo(mapValue(result, "fullName"), mapValue(result, "avatarUrl"));
        } catch (Exception e) {
            return new ActorInfo(null, null);
        }
    }

    private static String mapValue(Map<String, Object> values, String key) {
        Object value = values == null ? null : values.get(key);
        return value == null ? null : value.toString();
    }

    private record ActorInfo(String name, String avatarUrl) {}
    private record ResolvedInput(PalletInputType type, String id, String batchIdHex) {}

    private static String extractUserIdFromToken(String tokenHeader) throws Exception {
        String token = tokenHeader;
        if (token.startsWith("Bearer ")) {
            token = token.substring(7);
        }
        SignedJWT signedJWT = SignedJWT.parse(token);
        return signedJWT.getJWTClaimsSet().getStringClaim("userId");
    }

    private static String extractRoleFromToken(String tokenHeader) throws Exception {
        String token = tokenHeader;
        if (token.startsWith("Bearer ")) {
            token = token.substring(7);
        }
        SignedJWT signedJWT = SignedJWT.parse(token);
        return signedJWT.getJWTClaimsSet().getStringClaim("role");
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


