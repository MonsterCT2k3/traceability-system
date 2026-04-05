package vn.edu.kma.product_service.service;

import vn.edu.kma.product_service.dto.request.ProductUnitClaimRequest;
import vn.edu.kma.product_service.dto.request.ProductUnitGenerateRequest;
import vn.edu.kma.product_service.dto.request.ProductUnitSecretScanRequest;
import vn.edu.kma.product_service.dto.response.ProductUnitClaimResponse;
import vn.edu.kma.product_service.dto.response.ProductUnitGenerateResponse;
import vn.edu.kma.product_service.dto.response.ProductUnitPublicTraceResponse;
import vn.edu.kma.product_service.dto.response.ProductUnitSecretScanResponse;

public interface ProductUnitService {

    ProductUnitGenerateResponse generateUnits(String cartonId, ProductUnitGenerateRequest request, String tokenHeader);

    ProductUnitClaimResponse claimUnit(String unitId, ProductUnitClaimRequest request, String tokenHeader);

    /**
     * Mỗi lần quét QR bí mật (gửi đúng secret) tăng scanCount; không cần đăng nhập.
     */
    ProductUnitSecretScanResponse recordSecretScan(String unitId, ProductUnitSecretScanRequest request);

    ProductUnitPublicTraceResponse getPublicTraceByUnitId(String unitId);

    ProductUnitPublicTraceResponse getPublicTraceByUnitSerial(String unitSerial);

    /**
     * Nội dung gắn trong QR truy xuất unit: chỉ {@code unitId} (UUID) để app quét rồi gọi {@code GET /units/{id}/trace}.
     * Không tăng scanCount khi chỉ tải ảnh QR.
     */
    String getTraceQrPayload(String unitId);
}
