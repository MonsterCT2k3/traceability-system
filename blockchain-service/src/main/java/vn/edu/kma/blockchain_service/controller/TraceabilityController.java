package vn.edu.kma.blockchain_service.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import vn.edu.kma.blockchain_service.dto.response.BatchRecordResponse;
import vn.edu.kma.blockchain_service.dto.response.TransformedBatchRecordResponse;
import vn.edu.kma.blockchain_service.service.TraceabilityService;
import vn.edu.kma.common.dto.response.ApiResponse; // Dùng lại ApiResponse của common-library

import java.util.Collections;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/blockchain")
@RequiredArgsConstructor
public class TraceabilityController {

    private final TraceabilityService traceabilityService;

    // API Deploy Contract (Chỉ chạy 1 lần để lấy địa chỉ)
    @PostMapping("/deploy")
    public ResponseEntity<ApiResponse<String>> deployContract() {
        try {
            String address = traceabilityService.deployContract();
            return ResponseEntity.ok(ApiResponse.<String>builder()
                    .code(200)
                    .message("Deploy thành công")
                    .result(address)
                    .build());
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(ApiResponse.<String>builder()
                    .code(500)
                    .message("Lỗi: " + e.getMessage())
                    .build());
        }
    }

    @PostMapping("/batch")
    public ResponseEntity<ApiResponse<String>> recordBatch(@RequestBody Map<String, String> body) {
        try {
            String batchIdHex = body.get("batchIdHex");
            String dataHashHex = body.get("dataHashHex");
            String txHash = traceabilityService.recordBatch(batchIdHex, dataHashHex);
            return ResponseEntity.ok(ApiResponse.<String>builder()
                    .code(200)
                    .message("Đã ghi lô gốc lên blockchain")
                    .result(txHash)
                    .build());
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(ApiResponse.<String>builder()
                    .code(500)
                    .message("Lỗi: " + e.getMessage())
                    .build());
        }
    }

    @PostMapping("/transformed-batch")
    public ResponseEntity<ApiResponse<String>> recordTransformedBatch(@RequestBody Map<String, Object> body) {
        try {
            String batchIdHex = (String) body.get("batchIdHex");
            String dataHashHex = (String) body.get("dataHashHex");
            @SuppressWarnings("unchecked")
            List<String> parentHashesHex = (List<String>) body.get("parentHashesHex");
            if (parentHashesHex == null) {
                parentHashesHex = Collections.emptyList();
            }
            String txHash = traceabilityService.recordTransformedBatch(batchIdHex, dataHashHex, parentHashesHex);
            return ResponseEntity.ok(ApiResponse.<String>builder()
                    .code(200)
                    .message("Đã ghi pallet/lô chế biến lên blockchain")
                    .result(txHash)
                    .build());
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(ApiResponse.<String>builder()
                    .code(500)
                    .message("Lỗi: " + e.getMessage())
                    .build());
        }
    }

    /**
     * Audit chuyển quyền theo userId (event on-chain). Body: batchIdHex, fromUserId, toUserId.
     */
    @PostMapping("/ownership-change")
    public ResponseEntity<ApiResponse<String>> logOwnershipChange(@RequestBody Map<String, String> body) {
        try {
            String batchIdHex = body.get("batchIdHex");
            String fromUserId = body.get("fromUserId");
            String toUserId = body.get("toUserId");
            String txHash = traceabilityService.logOwnershipChange(batchIdHex, fromUserId, toUserId);
            return ResponseEntity.ok(ApiResponse.<String>builder()
                    .code(200)
                    .message("Đã ghi audit chuyển quyền lên blockchain")
                    .result(txHash)
                    .build());
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(ApiResponse.<String>builder()
                    .code(500)
                    .message("Lỗi: " + e.getMessage())
                    .build());
        }
    }

    @GetMapping("/batch/{batchIdHex}")
    public ResponseEntity<ApiResponse<BatchRecordResponse>> getBatchRecord(@PathVariable String batchIdHex) {
        try {
            BatchRecordResponse record = traceabilityService.getBatchRecord(batchIdHex);
            return ResponseEntity.ok(ApiResponse.<BatchRecordResponse>builder()
                    .code(200)
                    .message("OK")
                    .result(record)
                    .build());
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(ApiResponse.<BatchRecordResponse>builder()
                    .code(500)
                    .message("Lỗi: " + e.getMessage())
                    .build());
        }
    }

    @GetMapping("/transformed-batch/{batchIdHex}")
    public ResponseEntity<ApiResponse<TransformedBatchRecordResponse>> getTransformedBatchRecord(@PathVariable String batchIdHex) {
        try {
            TransformedBatchRecordResponse record = traceabilityService.getTransformedBatchRecord(batchIdHex);
            return ResponseEntity.ok(ApiResponse.<TransformedBatchRecordResponse>builder()
                    .code(200)
                    .message("OK")
                    .result(record)
                    .build());
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(ApiResponse.<TransformedBatchRecordResponse>builder()
                    .code(500)
                    .message("Lỗi: " + e.getMessage())
                    .build());
        }
    }

    @GetMapping("/batch/{batchIdHex}/exists")
    public ResponseEntity<ApiResponse<Boolean>> hasBatch(@PathVariable String batchIdHex) {
        try {
            boolean exists = traceabilityService.hasBatch(batchIdHex);
            return ResponseEntity.ok(ApiResponse.<Boolean>builder()
                    .code(200)
                    .result(exists)
                    .build());
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(ApiResponse.<Boolean>builder()
                    .code(500)
                    .message("Lỗi: " + e.getMessage())
                    .build());
        }
    }

    @GetMapping("/transformed-batch/{batchIdHex}/exists")
    public ResponseEntity<ApiResponse<Boolean>> hasTransformedBatch(@PathVariable String batchIdHex) {
        try {
            boolean exists = traceabilityService.hasTransformedBatch(batchIdHex);
            return ResponseEntity.ok(ApiResponse.<Boolean>builder()
                    .code(200)
                    .result(exists)
                    .build());
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(ApiResponse.<Boolean>builder()
                    .code(500)
                    .message("Lỗi: " + e.getMessage())
                    .build());
        }
    }
}
