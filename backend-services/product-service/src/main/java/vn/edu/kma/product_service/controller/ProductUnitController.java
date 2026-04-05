package vn.edu.kma.product_service.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import vn.edu.kma.common.dto.response.ApiResponse;
import vn.edu.kma.product_service.dto.request.ProductUnitClaimRequest;
import vn.edu.kma.product_service.dto.request.ProductUnitSecretScanRequest;
import vn.edu.kma.product_service.dto.response.ProductUnitClaimResponse;
import vn.edu.kma.product_service.dto.response.ProductUnitPublicTraceResponse;
import vn.edu.kma.product_service.dto.response.ProductUnitSecretScanResponse;
import vn.edu.kma.product_service.service.ProductUnitService;
import vn.edu.kma.product_service.utils.QRCodeGenerator;

@RestController
@RequestMapping("/api/v1/units")
@RequiredArgsConstructor
@Slf4j
public class ProductUnitController {

    private final ProductUnitService productUnitService;

    /**
     * QR truy xuất (PNG). Nội dung = {@code unitId} (UUID); app quét → gọi {@code GET .../units/{id}/trace}.
     * Không tăng scanCount khi chỉ tải ảnh.
     */
    @GetMapping(value = "/{unitId}/qr", produces = MediaType.IMAGE_PNG_VALUE)
    public ResponseEntity<byte[]> traceQrPng(@PathVariable String unitId) {
        try {
            String payload = productUnitService.getTraceQrPayload(unitId);
            byte[] qrImage = QRCodeGenerator.generateQRCodeImage(payload, 300, 300);
            return ResponseEntity.ok().body(qrImage);
        } catch (RuntimeException e) {
            if (e.getMessage() != null && e.getMessage().contains("không tồn tại")) {
                return ResponseEntity.notFound().build();
            }
            log.error("traceQrPng failed", e);
            return ResponseEntity.internalServerError().build();
        } catch (Exception e) {
            log.error("traceQrPng failed", e);
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * Truy xuất công khai theo id. {@code scanCount} trong response = số lần secret-scan, không tăng ở đây.
     */
    @GetMapping("/{unitId}/trace")
    public ResponseEntity<ApiResponse<ProductUnitPublicTraceResponse>> traceById(@PathVariable String unitId) {
        try {
            ProductUnitPublicTraceResponse result = productUnitService.getPublicTraceByUnitId(unitId);
            return ResponseEntity.ok(
                    ApiResponse.<ProductUnitPublicTraceResponse>builder()
                            .code(200)
                            .message("Truy xuất thành công")
                            .result(result)
                            .build()
            );
        } catch (RuntimeException e) {
            if (e.getMessage() != null && e.getMessage().contains("không tồn tại")) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(
                        ApiResponse.<ProductUnitPublicTraceResponse>builder()
                                .code(404)
                                .message(e.getMessage())
                                .build()
                );
            }
            log.error("traceById failed", e);
            return ResponseEntity.internalServerError().body(
                    ApiResponse.<ProductUnitPublicTraceResponse>builder()
                            .code(500)
                            .message("Lỗi: " + e.getMessage())
                            .build()
            );
        }
    }

    /**
     * Truy xuất công khai theo mã serial in trên bao bì. Query: ?serial=...
     */
    @GetMapping("/trace/by-serial")
    public ResponseEntity<ApiResponse<ProductUnitPublicTraceResponse>> traceBySerial(
            @RequestParam("serial") String serial
    ) {
        try {
            ProductUnitPublicTraceResponse result = productUnitService.getPublicTraceByUnitSerial(serial);
            return ResponseEntity.ok(
                    ApiResponse.<ProductUnitPublicTraceResponse>builder()
                            .code(200)
                            .message("Truy xuất thành công")
                            .result(result)
                            .build()
            );
        } catch (RuntimeException e) {
            if (e.getMessage() != null && e.getMessage().contains("không tồn tại")) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(
                        ApiResponse.<ProductUnitPublicTraceResponse>builder()
                                .code(404)
                                .message(e.getMessage())
                                .build()
                );
            }
            log.error("traceBySerial failed", e);
            return ResponseEntity.internalServerError().body(
                    ApiResponse.<ProductUnitPublicTraceResponse>builder()
                            .code(500)
                            .message("Lỗi: " + e.getMessage())
                            .build()
            );
        }
    }

    /**
     * Mỗi lần quét QR bí mật (app gửi đúng secret) tăng {@code scanCount}. Công khai, không cần JWT.
     */
    @PostMapping("/{unitId}/secret-scan")
    public ResponseEntity<ApiResponse<ProductUnitSecretScanResponse>> secretScan(
            @PathVariable String unitId,
            @RequestBody ProductUnitSecretScanRequest request
    ) {
        try {
            ProductUnitSecretScanResponse result = productUnitService.recordSecretScan(unitId, request);
            return ResponseEntity.ok(
                    ApiResponse.<ProductUnitSecretScanResponse>builder()
                            .code(200)
                            .message("Đã ghi nhận quét mã bí mật")
                            .result(result)
                            .build()
            );
        } catch (RuntimeException e) {
            if (e.getMessage() != null && e.getMessage().contains("không tồn tại")) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(
                        ApiResponse.<ProductUnitSecretScanResponse>builder()
                                .code(404)
                                .message(e.getMessage())
                                .build()
                );
            }
            if ("Secret không đúng".equals(e.getMessage())) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(
                        ApiResponse.<ProductUnitSecretScanResponse>builder()
                                .code(400)
                                .message(e.getMessage())
                                .build()
                );
            }
            log.error("secretScan failed", e);
            return ResponseEntity.internalServerError().body(
                    ApiResponse.<ProductUnitSecretScanResponse>builder()
                            .code(500)
                            .message("Lỗi: " + e.getMessage())
                            .build()
            );
        }
    }

    /**
     * Claim sở hữu unit bằng secret scratch-off (một lần). Cần JWT đăng nhập.
     */
    @PostMapping("/{unitId}/claim")
    public ResponseEntity<ApiResponse<ProductUnitClaimResponse>> claim(
            @PathVariable String unitId,
            @RequestBody ProductUnitClaimRequest request,
            @RequestHeader("Authorization") String tokenHeader
    ) {
        try {
            ProductUnitClaimResponse result = productUnitService.claimUnit(unitId, request, tokenHeader);
            return ResponseEntity.ok(
                    ApiResponse.<ProductUnitClaimResponse>builder()
                            .code(200)
                            .message("Claim thành công")
                            .result(result)
                            .build()
            );
        } catch (Exception e) {
            log.error("claim failed", e);
            return ResponseEntity.internalServerError().body(
                    ApiResponse.<ProductUnitClaimResponse>builder()
                            .code(500)
                            .message("Lỗi: " + e.getMessage())
                            .build()
            );
        }
    }
}
