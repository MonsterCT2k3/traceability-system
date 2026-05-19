package vn.edu.kma.product_service.controller;

import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import vn.edu.kma.common.dto.response.ApiResponse;
import vn.edu.kma.product_service.config.OpenApiConfig;
import vn.edu.kma.product_service.dto.request.ScanHistoryRequest;
import vn.edu.kma.product_service.dto.response.ScanHistoryResponse;
import vn.edu.kma.product_service.service.ScanHistoryService;

@RestController
@RequestMapping("/api/v1/scan-history")
@RequiredArgsConstructor
public class ScanHistoryController {

    private final ScanHistoryService scanHistoryService;

    @PostMapping
    @SecurityRequirement(name = OpenApiConfig.BEARER_AUTH)
    public ResponseEntity<ApiResponse<Void>> recordScan(
            @Valid @RequestBody ScanHistoryRequest request,
            @RequestHeader("Authorization") String tokenHeader) {
        
        scanHistoryService.recordScan(request, tokenHeader);
        
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.<Void>builder()
                .code(201)
                .message("Đã lưu lịch sử quét")
                .build());
    }

    @GetMapping
    @SecurityRequirement(name = OpenApiConfig.BEARER_AUTH)
    public ResponseEntity<ApiResponse<Page<ScanHistoryResponse>>> getScanHistory(
            @RequestHeader("Authorization") String tokenHeader,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
            
        return ResponseEntity.status(HttpStatus.OK).body(ApiResponse.<Page<ScanHistoryResponse>>builder()
                .code(200)
                .message("Lấy lịch sử quét thành công")
                .result(scanHistoryService.getScanHistory(tokenHeader, page, size))
                .build());
    }
}
