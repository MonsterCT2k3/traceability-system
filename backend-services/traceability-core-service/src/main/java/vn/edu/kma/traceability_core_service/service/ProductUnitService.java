package vn.edu.kma.traceability_core_service.service;

import vn.edu.kma.traceability_core_service.dto.request.ProductUnitGenerateRequest;
import vn.edu.kma.traceability_core_service.dto.response.ProductUnitGenerateResponse;
import vn.edu.kma.traceability_core_service.dto.response.ProductUnitPublicTraceResponse;

public interface ProductUnitService {

    ProductUnitGenerateResponse generateUnits(String cartonId, ProductUnitGenerateRequest request, String tokenHeader);

    ProductUnitPublicTraceResponse getPublicTraceByUnitId(String unitId, boolean isHistory);

    ProductUnitPublicTraceResponse getPublicTraceByUnitSerial(String unitSerial, boolean isHistory);

    /**
     * Xác thực lịch sử truy xuất với Blockchain.
     */
    ProductUnitPublicTraceResponse verifyPublicTrace(String unitSerial);

    /**
     * Nội dung gắn trong QR truy xuất unit: chỉ {@code unitId} (UUID) để app quét rồi gọi {@code GET /units/{id}/trace}.
     * Không tăng scanCount khi chỉ tải ảnh QR.
     */
    String getTraceQrPayload(String unitId);
}

