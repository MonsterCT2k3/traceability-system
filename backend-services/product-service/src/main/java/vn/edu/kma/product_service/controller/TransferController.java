package vn.edu.kma.product_service.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import vn.edu.kma.common.dto.response.ApiResponse;
import vn.edu.kma.product_service.dto.request.TransferInitRequest;
import vn.edu.kma.product_service.entity.TransferRecord;
import vn.edu.kma.product_service.service.TransferService;

import java.util.List;

@RestController
@RequestMapping("/api/v1/transfers")
@RequiredArgsConstructor
public class TransferController {
    private final TransferService transferService;

    @PostMapping
    public ResponseEntity<ApiResponse<TransferRecord>> initiateTransfer(
            @RequestBody TransferInitRequest request,
            @RequestHeader("Authorization") String token
    ) {
        return ResponseEntity.ok(ApiResponse.<TransferRecord>builder()
                .code(200)
                .message("Đã gửi yêu cầu chuyển giao, chờ người nhận xác nhận")
                .result(transferService.initiateTransfer(request, token))
                .build());
    }

    @PostMapping("/{transferId}/respond")
    public ResponseEntity<ApiResponse<TransferRecord>> respondTransfer(
            @PathVariable String transferId,
            @RequestParam boolean accept,
            @RequestHeader("Authorization") String token
    ) {
        return ResponseEntity.ok(ApiResponse.<TransferRecord>builder()
                .code(200)
                .message(accept ? "Đã chấp nhận chuyển giao" : "Đã từ chối chuyển giao")
                .result(transferService.respondTransfer(transferId, accept, token))
                .build());
    }

    @GetMapping("/pending")
    public ResponseEntity<ApiResponse<List<TransferRecord>>> getPendingTransfers(
            @RequestHeader("Authorization") String token
    ) {
        return ResponseEntity.ok(ApiResponse.<List<TransferRecord>>builder()
                .code(200)
                .result(transferService.getPendingTransfers(token))
                .build());
    }
}
