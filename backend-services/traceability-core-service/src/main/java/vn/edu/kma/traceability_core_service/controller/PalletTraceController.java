package vn.edu.kma.traceability_core_service.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import vn.edu.kma.common.dto.response.ApiResponse;
import vn.edu.kma.traceability_core_service.dto.response.DirectTraceResponse;
import vn.edu.kma.traceability_core_service.service.PalletService;

@RestController
@RequestMapping("/api/v1/pallets")
@RequiredArgsConstructor
public class PalletTraceController {

    private final PalletService palletService;

    @GetMapping("/{palletId}/trace-direct")
    public ResponseEntity<ApiResponse<DirectTraceResponse>> traceDirect(@PathVariable String palletId) {
        return ResponseEntity.ok(ApiResponse.<DirectTraceResponse>builder()
                .code(200)
                .message("Truy xuất đầu vào trực tiếp thành công")
                .result(palletService.getDirectTrace(palletId))
                .build());
    }

    @GetMapping("/{palletId}/verify-direct")
    public ResponseEntity<ApiResponse<DirectTraceResponse>> verifyDirect(@PathVariable String palletId) {
        return ResponseEntity.ok(ApiResponse.<DirectTraceResponse>builder()
                .code(200)
                .message("Đã xác minh lô đang xem và đầu vào trực tiếp")
                .result(palletService.verifyDirectTrace(palletId))
                .build());
    }
}
