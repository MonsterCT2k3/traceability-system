package vn.edu.kma.blockchain_service.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import vn.edu.kma.blockchain_service.domain.GasOperation;
import vn.edu.kma.blockchain_service.domain.GasUsageStatus;
import vn.edu.kma.blockchain_service.dto.response.GasUsageSummaryResponse;
import vn.edu.kma.blockchain_service.dto.response.GasUsageTransactionResponse;
import vn.edu.kma.blockchain_service.service.GasUsageService;
import vn.edu.kma.common.dto.response.ApiResponse;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Base64;

@RestController
@RequestMapping("/api/v1/gas-usage")
@RequiredArgsConstructor
public class GasUsageController {

    private final GasUsageService gasUsageService;
    private final ObjectMapper objectMapper;

    @GetMapping("/my/summary")
    public ResponseEntity<ApiResponse<GasUsageSummaryResponse>> mySummary(
            @RequestHeader(HttpHeaders.AUTHORIZATION) String authorization,
            @RequestParam(required = false) Instant from,
            @RequestParam(required = false) Instant to) {
        JwtClaims claims = claims(authorization);
        requireBillable(claims.role());
        return ok(gasUsageService.summary(claims.userId(), claims.role(), from, to));
    }

    @GetMapping("/my/transactions")
    public ResponseEntity<ApiResponse<Page<GasUsageTransactionResponse>>> myTransactions(
            @RequestHeader(HttpHeaders.AUTHORIZATION) String authorization,
            @RequestParam(required = false) GasOperation operation,
            @RequestParam(required = false) GasUsageStatus status,
            @RequestParam(required = false) Instant from,
            @RequestParam(required = false) Instant to,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        JwtClaims claims = claims(authorization);
        requireBillable(claims.role());
        return ok(gasUsageService.transactions(
                claims.userId(),
                claims.role(),
                operation,
                status,
                from,
                to,
                PageRequest.of(Math.max(page, 0), Math.min(Math.max(size, 1), 100),
                        Sort.by(Sort.Direction.DESC, "createdAt"))));
    }

    @GetMapping("/admin/summary")
    public ResponseEntity<ApiResponse<GasUsageSummaryResponse>> adminSummary(
            @RequestHeader(HttpHeaders.AUTHORIZATION) String authorization,
            @RequestParam(required = false) String actorId,
            @RequestParam(required = false) String role,
            @RequestParam(required = false) Instant from,
            @RequestParam(required = false) Instant to) {
        requireAdmin(claims(authorization));
        return ok(gasUsageService.summary(actorId, normalizeRoleOrNull(role), from, to));
    }

    @GetMapping("/admin/transactions")
    public ResponseEntity<ApiResponse<Page<GasUsageTransactionResponse>>> adminTransactions(
            @RequestHeader(HttpHeaders.AUTHORIZATION) String authorization,
            @RequestParam(required = false) String actorId,
            @RequestParam(required = false) String role,
            @RequestParam(required = false) GasOperation operation,
            @RequestParam(required = false) GasUsageStatus status,
            @RequestParam(required = false) Instant from,
            @RequestParam(required = false) Instant to,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        requireAdmin(claims(authorization));
        return ok(gasUsageService.transactions(
                actorId,
                normalizeRoleOrNull(role),
                operation,
                status,
                from,
                to,
                PageRequest.of(Math.max(page, 0), Math.min(Math.max(size, 1), 100),
                        Sort.by(Sort.Direction.DESC, "createdAt"))));
    }

    private <T> ResponseEntity<ApiResponse<T>> ok(T result) {
        return ResponseEntity.ok(ApiResponse.<T>builder()
                .code(200)
                .message("OK")
                .result(result)
                .build());
    }

    private void requireBillable(String role) {
        if (!"SUPPLIER".equals(role) && !"MANUFACTURER".equals(role)) {
            throw new IllegalArgumentException("Role khong duoc xem thong ke phi gas");
        }
    }

    private void requireAdmin(JwtClaims claims) {
        if (!"ADMIN".equals(claims.role())) {
            throw new IllegalArgumentException("Chi admin moi duoc xem thong ke tong hop");
        }
    }

    private JwtClaims claims(String authorization) {
        if (authorization == null || !authorization.startsWith("Bearer ")) {
            throw new IllegalArgumentException("Missing bearer token");
        }
        try {
            String token = authorization.substring(7);
            String[] parts = token.split("\\.");
            if (parts.length < 2) {
                throw new IllegalArgumentException("Invalid JWT");
            }
            String payload = new String(Base64.getUrlDecoder().decode(parts[1]), StandardCharsets.UTF_8);
            JsonNode node = objectMapper.readTree(payload);
            String userId = text(node, "userId");
            String role = normalizeRoleOrNull(text(node, "role"));
            if (userId == null || role == null) {
                throw new IllegalArgumentException("Missing userId/role claims");
            }
            return new JwtClaims(userId, role);
        } catch (Exception e) {
            throw new IllegalArgumentException("Invalid bearer token: " + e.getMessage());
        }
    }

    private static String text(JsonNode node, String field) {
        JsonNode value = node == null ? null : node.get(field);
        return value == null || value.isNull() ? null : value.asText();
    }

    private static String normalizeRoleOrNull(String role) {
        return role == null || role.isBlank() ? null : role.trim().toUpperCase();
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ApiResponse<Object>> handleBadRequest(IllegalArgumentException e) {
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(ApiResponse.builder()
                .code(403)
                .message(e.getMessage())
                .build());
    }

    private record JwtClaims(String userId, String role) {}
}
