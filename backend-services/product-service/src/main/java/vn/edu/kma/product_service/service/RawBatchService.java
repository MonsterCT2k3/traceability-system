package vn.edu.kma.product_service.service;

import vn.edu.kma.product_service.dto.request.RawBatchCreateRequest;
import vn.edu.kma.product_service.dto.request.RawBatchMergeRequest;
import vn.edu.kma.product_service.dto.response.RawBatchResponse;

import java.util.List;
import java.util.Map;

public interface RawBatchService {
    Map<String, String> createRawBatch(RawBatchCreateRequest request, String token);

    /**
     * Gộp nhiều lô cùng loại (materialType + materialName): xử lý DB trước (xóa lô nguồn, tạo lô mới),
     * sau đó neo recordBatch; nếu neo thất bại thì hoàn tác DB (khôi phục lô nguồn, xóa lô mới).
     */
    Map<String, String> mergeRawBatches(RawBatchMergeRequest request, String token);

    List<RawBatchResponse> getMyRawBatches(String token);
}

